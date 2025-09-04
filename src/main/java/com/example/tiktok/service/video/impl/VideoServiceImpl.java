package com.example.tiktok.service.video.impl;

import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.embeddings.TextEmbeddingResultItem;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.tiktok.config.LocalCache;
import com.example.tiktok.config.QiNiuConfig;
import com.example.tiktok.constant.AuditStatus;
import com.example.tiktok.constant.RedisConstant;
import com.example.tiktok.entity.File;
import com.example.tiktok.entity.LabelDAO;
import com.example.tiktok.entity.response.ChatResponse;
import com.example.tiktok.entity.reuqest.ChatRequest;
import com.example.tiktok.entity.task.VideoTask;
import com.example.tiktok.entity.user.User;
import com.example.tiktok.entity.video.Type;
import com.example.tiktok.entity.video.Video;
import com.example.tiktok.entity.video.VideoShare;
import com.example.tiktok.entity.video.VideoStar;
import com.example.tiktok.entity.vo.BasePage;
import com.example.tiktok.entity.vo.HotVideo;
import com.example.tiktok.entity.vo.UserModel;
import com.example.tiktok.entity.vo.UserVO;
import com.example.tiktok.exception.BaseException;
import com.example.tiktok.holder.UserHolder;
import com.example.tiktok.mapper.video.VideoMapper;
import com.example.tiktok.service.FeedService;
import com.example.tiktok.service.FileService;
import com.example.tiktok.service.InterestPushService;
import com.example.tiktok.service.audit.VideoPublishAuditServiceImpl;
import com.example.tiktok.service.user.FavoritesService;
import com.example.tiktok.service.user.FollowService;
import com.example.tiktok.service.user.UserService;
import com.example.tiktok.service.video.TypeService;
import com.example.tiktok.service.video.VideoService;
import com.example.tiktok.service.video.VideoShareService;
import com.example.tiktok.service.video.VideoStarService;
import com.example.tiktok.utils.DashscopeApiRequest;
import com.example.tiktok.utils.Embedding;
import com.example.tiktok.utils.FileUtil;
import com.example.tiktok.utils.RedisCacheUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VideoServiceImpl extends ServiceImpl<VideoMapper, Video> implements VideoService {

    @Autowired
    private TypeService typeService;

    @Autowired
    private InterestPushService interestPushService;

    @Autowired
    private UserService userService;

    @Autowired
    private VideoStarService videoStarService;

    @Autowired
    private VideoShareService videoShareService;

    @Autowired
    private RedisCacheUtil redisCacheUtil;

    @Autowired
    private FavoritesService favoritesService;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private VideoPublishAuditServiceImpl videoPublishAuditService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private FollowService followService;

    @Autowired
    private FeedService feedService;

    @Autowired
    private FileService fileService;

    final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 更新点赞数
     * @param video
     * @param value
     */
    public void updateStar(Video video, Long value) {
        final UpdateWrapper<Video> updateWrapper = new UpdateWrapper<>();
        updateWrapper.setSql("start_count = start_count + " + value);
        updateWrapper.lambda()
                     .eq(Video::getId, video.getId())
                     .eq(Video::getStartCount, video.getStartCount());

        update(video, updateWrapper);
    }

    /**
     * 更新分享数
     * @param video
     * @param value
     */
    public void updateShare(Video video, Long value) {
        final UpdateWrapper<Video> updateWrapper = new UpdateWrapper<>();
        updateWrapper.setSql("share_count = share_count + ", value);
        updateWrapper.lambda()
                     .eq(Video::getId, video.getId())
                     .eq(Video::getShareCount, video.getShareCount());

        update(video, updateWrapper);
    }

    /**
     * 更新浏览量
     * @param video
     * @param value
     */
    public void updateHistory(Video video, Long value) {
        final UpdateWrapper<Video> updateWrapper = new UpdateWrapper<>();
        updateWrapper.setSql("history_count = history_count + " + value);
        updateWrapper.lambda()
                     .eq(Video::getId, video.getId())
                     .eq(Video::getHistoryCount, video.getHistoryCount());

        update(video, updateWrapper);
    }

    /**
     * 更新收藏夹数
     * @param video
     * @param value
     */
    public void updateFavorites(Video video, Long value) {
        final UpdateWrapper<Video> updateWrapper = new UpdateWrapper<>();
        updateWrapper.setSql("favorites_count = favorites_count + " + value);
        updateWrapper.lambda()
                     .eq(Video::getId, video.getId())
                     .eq(Video::getFavoritesCount, video.getFavoritesCount());

        update(video, updateWrapper);
    }

    /**
     * 设置视频发布人的信息和视频类型
     * @param videos
     */
    public void setUserVoAndUrl(Collection<Video> videos) {
        if (!ObjectUtils.isEmpty(videos)) {
            Set<Long> userIds = new HashSet<>();
            final ArrayList<Long> fileIds = new ArrayList<>();

            for (Video video : videos) {
                userIds.add(video.getUserId());
                fileIds.add(video.getUrl());
                fileIds.add(video.getCover());
            }

            final Map<Long, File> fileMap = fileService.listByIds(fileIds)
                                                       .stream()
                                                       .collect(Collectors.toMap(File::getId, Function.identity()));
            final Map<Long, User> userMap = userService.list(userIds)
                                                       .stream()
                                                       .collect(Collectors.toMap(User::getId, Function.identity()));

            for (Video video : videos) {
                final UserVO userVO = new UserVO();
                final User user = userMap.get(video.getUserId());
                userVO.setId(video.getUserId());
                userVO.setNickName(user.getNickName());
                userVO.setDescription(user.getDescription());
                userVO.setSex(user.getSex());
                video.setUser(userVO);
                final File file = fileMap.get(video.getUrl());
                video.setVideoType(file.getFormat());
            }
        }
    }

    // TODO 优化
    @Override
    public Video getVideoById(Long videoId, Long userId) {
        final Video video = this.getOne(new LambdaQueryWrapper<Video>().eq(Video::getId, videoId));

        if (Objects.isNull(video)) {
            throw new BaseException("指定视频不存在");
        }

        // 私密则返回为空
        if (video.getOpen()) {
            return new Video();
        }

        setUserVoAndUrl(Collections.singleton(video));
        // 用户是否与视频交互过
        // 这里需要优化 如果这里开线程获取则系统g了(因为这里的场景不适合) -> 请求数很多
        // 正确做法: 视频信息存储在Redis中，点赞收藏等行为异步直接写入数据库, 定时任务扫描DB中不重要的数据更新到Redis中
        video.setStart(videoStarService.startState(videoId, userId));
        video.setFavorites(favoritesService.favoritesState(videoId, userId));
        video.setFollow(followService.isFollows(video.getUserId(), userId));
        return video;
    }

    /**
     * 往向量数据库中插入数据
     * @param labelNames 标签
     * @throws Exception
     */
    public static void insertLabel(String[] labelNames) throws Exception {
        LabelDAO labelDAO = new LabelDAO();
        List<String> names = Arrays.asList(labelNames);

        List<TextEmbeddingResultItem> items = Embedding.basicCall(names);
        for (TextEmbeddingResultItem item : items) {
            List<Double> embedding = item.getEmbedding();
            labelDAO.insertLabel(names.get(item.getTextIndex()), embedding);
        }
    }

    /**
     * 发布 | 修改视频
     * @param video
     * @param userId
     */
    @Override
    public void publishVideo(Video video, Long userId) throws Exception {
        Video oldVideo = null;

        // 如果视频ID不为空 -> 说明是修改视频操作
        final Long videoId = video.getId();
        if (!Objects.isNull(videoId)) {
            oldVideo = this.getOne(new LambdaQueryWrapper<Video>().eq(Video::getId, videoId)
                                                                  .eq(Video::getUserId, userId));
            // 不能修改视频源和封面 url 不能一致
            if (!(video.buildVideoUrl().equals(oldVideo.buildVideoUrl())) ||
                !(video.buildCoverUrl().equals(oldVideo.buildCoverUrl())) ||
                !(video.getVideoType().equals(oldVideo.getVideoType())) ||
                !(video.getLabelNames().equals(oldVideo.getLabelNames()))) {
                throw new BaseException("不能更换视频源以及分类标签，只能修改视频信息");
            }
        }
        // 判断对应分类是否存在
        final Type type = typeService.getById(video.getTypeId());
        if (Objects.isNull(type)) {
            throw new BaseException("分类不存在");
        }

        // 校验标签最多不能超过5个
        if (video.buildLabel().size() > 5) {
            throw new BaseException("标签最多只能选择5个");
        }

        // 修改状态为审核中
        video.setAuditStatus(AuditStatus.PROCESS);
        video.setUserId(userId);

        // 视频是否为上传状态
        boolean isAdd = videoId == null;

        // 校验
        video.setYv(null);

        // 如果为修改状态
        if (!isAdd) {
            video.setVideoType(null);
            video.setLabelNames(null);
            video.setUrl(null);
            video.setCover(null);
        } else {
            // 如果是上传状态
            // 如果没有设置封面，自动设置封面
            if (ObjectUtils.isEmpty(video.getCover())) {
                video.setCover(fileService.generatePhoto(video.getUrl(), userId));
            }

            video.setYv("YV" + UUID.randomUUID().toString().replace("-", "").substring(8));
        }

        // 填充视频时长 | 上次发布视频时没有设置视频时长
        if (isAdd || !StringUtils.hasLength(oldVideo.getDuration())) {
            final String uuid = UUID.randomUUID().toString();
            LocalCache.put(uuid, true);
            try {
                Long url = video.getUrl();
                // 如果是修改状态
                if (Objects.isNull(url) || url == 0) {
                    url = oldVideo.getUrl();
                }
                final String fileKey = fileService.getById(url).getFileKey();
                final String duration = FileUtil.getVideoDuration(QiNiuConfig.CNAME + "/" + fileKey + "?uuid=" + uuid);
                video.setDuration(duration);
            } finally {
                LocalCache.rem(uuid);
            }
        }

        // 发布视频时，往向量化数据库插入数据
        String[] labelNames = video.getLabelNames().split(",");
        insertLabel(labelNames);

        this.saveOrUpdate(video);

        final VideoTask videoTask = new VideoTask();
        videoTask.setOldVideo(video);
        videoTask.setVideo(video);
        videoTask.setIsAdd(isAdd);
        videoTask.setOldState(isAdd || video.getOpen());
        videoTask.setNewState(true);

        videoPublishAuditService.audit(videoTask, false);
    }

    @Override
    public void deleteVideo(Long id, Long userId) {
        if (Objects.isNull(id)) {
            throw new BaseException("指定要删除的视频不存在");
        }

        final Video video = this.getOne(new LambdaQueryWrapper<Video>()
                                                .eq(Video::getId, id)
                                                .eq(Video::getUserId, userId));

        if (Objects.isNull(video)) {
            throw new BaseException("指定要删除的视频不存在");
        }

        final boolean b = removeById(id);

        // 删除视频的互动数据以及从系统库中去除
        if (b) {
            // 解耦
            new Thread(() -> {
                // 删除分享量 点赞量
                videoShareService.remove(new LambdaQueryWrapper<VideoShare>()
                                                 .eq(VideoShare::getVideoId, id)
                                                 .eq(VideoShare::getUserId, userId));
                videoStarService.remove(new LambdaQueryWrapper<VideoStar>()
                                                .eq(VideoStar::getVideoId, id)
                                                .eq(VideoStar::getUserId, userId));
                interestPushService.deleteSystemStockIn(video);
                interestPushService.deleteSystemTypeStockIn(video);
            }).start();
        }
    }

    /**
     * 主页推送，可能返回用户观看过的视频
     * @param userId
     * @return
     */
    @Override
    public Collection<Video> pushVideos(Long userId) {
        User user = null;

        if (!Objects.isNull(userId)) {
            user = userService.getById(userId);
        }

        // 可能为空，如果用户已经观看完所有推送视频
        Collection<Long> videoIds = interestPushService.listVideoIdByUserModel(user);
        Collection<Video> videos = new ArrayList<>();

        // 如果为空，直接推送10条视频
        if (ObjectUtils.isEmpty(videoIds)) {
            videoIds = list(new LambdaQueryWrapper<Video>().orderByDesc(Video::getGmtCreated))
                    .stream()
                    .map(Video::getId)
                    .toList();

            videoIds = new HashSet<>(videoIds).stream()
                                              .limit(10)
                                              .toList();
        }

        videos = listByIds(videoIds);
        setUserVoAndUrl(videos);
        return videos;
    }

    @Override
    public Collection<Video> getVideoByTypeId(Long typeId) {
        if (Objects.isNull(typeId)) {
            return Collections.EMPTY_LIST;
        }

        final Type type = typeService.getById(typeId);
        if (Objects.isNull(type)) {
            return Collections.EMPTY_LIST;
        }

        Collection<Long> videoIds = interestPushService.listVideoIdByTypeId(typeId);
        if (ObjectUtils.isEmpty(videoIds)) {
            return Collections.EMPTY_LIST;
        }
        final Collection<Video> videos = listByIds(videoIds);

        setUserVoAndUrl(videos);
        return videos;
    }

    @Override
    public IPage<Video> searchVideo(String search, BasePage basePage, Long userId) {
        final IPage p = basePage.page();

        final LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Video::getAuditStatus, AuditStatus.SUCCESS);

        // 如果带YV则精准搜索该视频
        if (search.contains("YV")) {
            wrapper.eq(Video::getYv, search);
        } else {
            wrapper.like(!ObjectUtils.isEmpty(search), Video::getTitle, search);
        }
        IPage<Video> page = this.page(p, wrapper);
        final List<Video> videos = page.getRecords();

        setUserVoAndUrl(videos);

        userService.addSearchHistory(userId, search);
        return page;
    }

    /**
     * 审核视频后
     * @param video
     */
    @Override
    public void auditProcess(Video video) {
        // 放行后
        updateById(video);
        interestPushService.pushSystemStockIn(video);
        interestPushService.pushSystemTypeStockIn(video);
        // 推送该视频博主的发信箱
        feedService.pushInBoxFeed(video.getUserId(), video.getId(), video.getGmtCreated().getTime());
    }

    /**
     * 向向量数据库中查询相似标签
     * @param labelNames 标签集合
     * @throws Exception
     */
    public static String searchLabel(List<String> labelNames) throws Exception {
        LabelDAO labelDAO = new LabelDAO();

        List<TextEmbeddingResultItem> textEmbeddingResultItems = Embedding.basicCall(labelNames);
        ArrayList<List<Double>> lists = new ArrayList<>();

        for (TextEmbeddingResultItem textEmbeddingResultItem : textEmbeddingResultItems) {
            List<Double> embedding = textEmbeddingResultItem.getEmbedding();
            lists.add(embedding);
        }
        List<String> similarLabelsBatch = labelDAO.findSimilarLabelsBatch(lists);

        String systemPrompt =
                "我将提供两组标签。你的任务是：1. 提取第一组标签的特征，包括类别、属性和相关概念，并确保第一组的标签本身也被包含。2. 在第二组标签中，找到与这些特征相关的标签，包括相同类别、属性或相关概念，尽可能多地匹配。3. 只输出筛选出的第二组标签。请确保匹配基于特征的相似性和关联性，而不仅限于字面上的相同。确保第一组的标签在输出中被包含。只需要回复我标签，不用加上括号之类的，要求每个标签用英文逗号分割，不用回复其余说明。如果无匹配标签，只需要返回提供的第一组标签即可。";
        String userInput = "第一组标签：" + labelNames + "第二组标签：" + similarLabelsBatch.toString();
        ChatRequest chatRequest = new ChatRequest("qwen-max-2024-09-19", systemPrompt, userInput);
        ChatResponse chatResponse = DashscopeApiRequest.sendRequest(chatRequest);

        // 获取响应返回的数据
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(chatResponse.getResponseBody());

        JsonNode messageNode = rootNode.path("choices")
                                       .get(0)
                                       .path("message");
        return messageNode.path("content").asText();
    }

    @Override
    public boolean startVideo(Long videoId, Long userId) throws Exception {
        final Video video = getById(videoId);
        if (Objects.isNull(video)) {
            throw new BaseException("指定视频不存在");
        }

        final VideoStar videoStart = new VideoStar();
        videoStart.setVideoId(videoId);
        videoStart.setUserId(userId);

        final boolean result = videoStarService.startVideo(videoStart);
        updateStar(video, result ? 1L : -1L);

        // 获取标签
        final List<String> labels = video.buildLabel();
        // 向向量数据库中查询相似标签
        List<String> similarLabels = Arrays.asList(searchLabel(labels).split(","));
        final UserModel userModel = UserModel.buildUserModel(similarLabels, videoId, 1.0);
        interestPushService.updateUserModel(userModel);

        return result;
    }

    @Override
    public boolean shareVideo(VideoShare videoShare) {
        final Video video = getById(videoShare.getVideoId());

        if (Objects.isNull(video)) {
            throw new BaseException("指定视频不存在");
        }
        final boolean result = videoShareService.share(videoShare);
        updateShare(video, result ? 1L : 0L);

        return result;
    }

    @Override
    @Async
    public void historyVideo(Long videoId, Long userId) {
        // 判断视频是否已观看过
        String key = RedisConstant.HISTORY_VIDEO + videoId + ":" + userId;
        final Object o = redisCacheUtil.get(key);

        if (Objects.isNull(o)) {
            redisCacheUtil.set(key, videoId, RedisConstant.HISTORY_TIME);

            final Video video = getById(videoId);
            video.setUser(userService.getInfo(video.getUserId()));
            video.setTypeName(typeService.getById(video.getTypeId()).getName());

            redisCacheUtil.zAdd(RedisConstant.USER_HISTORY_VIDEO + userId, new Date().getTime(), video,
                                RedisConstant.HISTORY_TIME);
            updateHistory(video, 1L);
        }
    }

    @Override
    public boolean favoritesVideo(Long fId, Long vId, Long uId) throws Exception {
        final Video video = getById(vId);
        if (Objects.isNull(video)) {
            throw new BaseException("指定视频不存在");
        }

        final boolean favorites = favoritesService.favorites(fId, vId, uId);
        updateFavorites(video, favorites ? 1L : -1L);

        final List<String> labels = video.buildLabel();
        // 向向量数据库中查询相似标签
        List<String> similarLabels = Arrays.asList(searchLabel(labels).split(","));
        final UserModel userModel = UserModel.buildUserModel(similarLabels, vId, 2.0);
        interestPushService.updateUserModel(userModel);

        return favorites;
    }

    @Override
    public LinkedHashMap<String, List<Video>> getHistory(BasePage basePage, Long userId) {
        String key = RedisConstant.USER_HISTORY_VIDEO + userId;
        final Set<ZSetOperations.TypedTuple<Object>> typedTuples =
                redisCacheUtil.zSetGetByPage(key, basePage.getPage(), basePage.getLimit());
        if (ObjectUtils.isEmpty(typedTuples)) {
            return new LinkedHashMap<>();
        }

        List<Video> temp = new ArrayList<>();
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        final LinkedHashMap<String, List<Video>> result = new LinkedHashMap<>();
        for (ZSetOperations.TypedTuple<Object> typedTuple : typedTuples) {
            final Date date = new Date(typedTuple.getScore().longValue());
            final String format = simpleDateFormat.format(date);
            if (!result.containsKey(format)) {
                result.put(format, new ArrayList<>());
            }

            final Video video = (Video) typedTuple.getValue();
            result.get(format).add(video);
            temp.add(video);
        }

        setUserVoAndUrl(temp);

        return result;
    }

    @Override
    public Collection<Video> listVideoByFavorites(Long favoritesId, Long userId) {
        final List<Long> videoIds = favoritesService.listVideoIds(favoritesId, userId);
        if (ObjectUtils.isEmpty(videoIds)) {
            return Collections.EMPTY_LIST;
        }

        final Collection<Video> videos = listByIds(videoIds);
        setUserVoAndUrl(videos);
        return videos;
    }

    @Override
    public Collection<HotVideo> hotRank() {
        final Set<ZSetOperations.TypedTuple<Object>> zSet =
                redisTemplate.opsForZSet().reverseRangeWithScores(RedisConstant.HOT_RANK, 0, -1);

        final ArrayList<HotVideo> hotVideos = new ArrayList<>();
        for (ZSetOperations.TypedTuple<Object> objectTypedTuple : zSet) {
            final HotVideo hotVideo;
            try {
                hotVideo = objectMapper.readValue(objectTypedTuple.getValue().toString(), HotVideo.class);
                hotVideo.setHot((double) objectTypedTuple.getScore().intValue());
                hotVideo.hotFormat();
                hotVideos.add(hotVideo);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return hotVideos;
    }

    /**
     * 根据视频标签，推送相似视频
     * @param video
     * @return
     */
    @Override
    public Collection<Video> listSimilarVideo(Video video) {
        if (ObjectUtils.isEmpty(video) || ObjectUtils.isEmpty(video.getLabelNames())) {
            return Collections.EMPTY_LIST;
        }

        // 根据视频的标签，从标签库中返回相应的视频ID集合
        final List<String> labels = video.buildLabel();
        final ArrayList<String> labelNames = new ArrayList<>();
        labelNames.addAll(labels);
        labelNames.addAll(labels);
        final Set<Long> videoIds = (Set<Long>) interestPushService.listVideoIdByLabels(labelNames);

        Collection<Video> videos = new ArrayList<>();

        // 去重，相关推荐中不能再推送当前视频
        videoIds.remove(video.getId());

        if (!ObjectUtils.isEmpty(videoIds)) {
            videos = listByIds(videoIds);
            setUserVoAndUrl(videos);
        }

        return videos;
    }

    @Override
    public IPage<Video> listByUserIdOpenVideo(Long userId, BasePage basePage) {
        if (Objects.isNull(userId)) {
            return new Page<>();
        }
        final IPage<Video> page = page(basePage.page(), new LambdaQueryWrapper<Video>().eq(Video::getUserId, userId)
                                                                                       .eq(Video::getAuditStatus,
                                                                                           AuditStatus.SUCCESS)
                                                                                       .orderByDesc(
                                                                                               Video::getGmtCreated));
        final List<Video> videos = page.getRecords();
        setUserVoAndUrl(videos);

        return page;
    }

    @Override
    public String getAuditQueueState() {
        return videoPublishAuditService.getAuditQueueState() ? "快速" : "慢速";
    }

    @Override
    public List<Video> selectNDaysAgeVideo(long id, int days, int limit) {
        return videoMapper.selectNDaysAgeVideo(id, days, limit);
    }

    /**
     * 推送热门视频
     * @return
     */
    @Override
    public Collection<Video> listHotVideo() {
        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DATE);

        // Key: label Value: videoCount
        final HashMap<String, Integer> map = new HashMap<>();
        // 优先推送今日的视频
        map.put(RedisConstant.HOT_VIDEO + today, 10);
        map.put(RedisConstant.HOT_VIDEO + (today - 1), 3);
        map.put(RedisConstant.HOT_VIDEO + (today - 2), 2);

        // 游客不用记录
        // 获取今日日期
        final List<Long> hotVideoIds = redisCacheUtil.pipeline(connection -> {
            map.forEach((k, v) -> {
                connection.sRandMember(k.getBytes(), v);
            });
            return null;
        });

        if (ObjectUtils.isEmpty(hotVideoIds)) {
            return Collections.EMPTY_LIST;
        }

        final ArrayList<Long> videoIds = new ArrayList<>();
        // 可能返回结果有null，做下校验
        for (Object videoId : hotVideoIds) {
            if (!ObjectUtils.isEmpty(videoId)) {
                videoIds.addAll((List) videoId);
            }
        }
        if (ObjectUtils.isEmpty(videoIds)) {
            return Collections.EMPTY_LIST;
        }

        final Collection<Video> videos = listByIds(videoIds);
        // 和浏览记录做交集？不需要做交集，热门视频和兴趣推送不一样，无需去重
        setUserVoAndUrl(videos);
        return videos;
    }

    /**
     * 推送关注的人视频 lastTime为分页视频参数，offset用于跳过前一个查询
     * @param userId   用户ID
     * @param lastTime 滚动分页参数，首次为null，后续为上传的末尾视频时间
     * @return
     */
    @Override
    public Collection<Video> followFeed(Long userId, Long lastTime) {
        // 客户端每次传入上次拉取的最早时间戳，服务端返回该时间戳之前的5个视频
        Set<Long> set = redisTemplate.opsForZSet()
                                     .reverseRangeByScore(RedisConstant.IN_FOLLOW + userId, 0,
                                                          lastTime == null ? new Date().getTime() : lastTime,
                                                          lastTime == null ? 0 : 1, 5);
        if (ObjectUtils.isEmpty(set)) {
            // 可能只是缓存中没有了，缓存只存储7天内的关注视频，继续往后查看关注的用户太少了，不做考虑 - feed流必然回产生的问题
            return Collections.EMPTY_LIST;
        }

        // 这里不好按照时间排序，需要手动排序
        final Collection<Video> videos = list(new LambdaQueryWrapper<Video>().in(Video::getId, set)
                                                                             .orderByDesc(Video::getGmtCreated));
        setUserVoAndUrl(videos);
        return videos;
    }

    /**
     * 将关注发件箱中的视频拉到自己收件箱中
     * @param userId
     */
    @Override
    public void initFollowFeed(Long userId) {
        // 获取所有关注的人
        final Collection<Long> followIds = followService.getFollow(userId, null);
        feedService.initFollowFeed(userId, followIds);
    }

    /**
     * 查询指定用户发布的视频列表
     * @param basePage
     * @param userId
     * @return
     */
    @Override
    public IPage<Video> listByUserIdVideo(BasePage basePage, Long userId) {
        final IPage page = page(basePage.page(), new LambdaQueryWrapper<Video>().eq(Video::getUserId, userId)
                                                                                .orderByDesc(Video::getGmtCreated));
        setUserVoAndUrl(page.getRecords());
        return page;
    }

    /**
     * 查询指定用户发布的视频ID列表
     * @param userId
     * @return
     */
    @Override
    public Collection<Long> listVideoIdByUserId(Long userId) {
        final List<Long> ids = list(new LambdaQueryWrapper<Video>().eq(Video::getUserId, userId)
                                                                   .eq(Video::getOpen, 0)
                                                                   .select(Video::getId))
                .stream()
                .map(Video::getId)
                .collect(Collectors.toList());
        return ids;
    }

    @Override
    public void violations(Long videoId) {
        final Video video = getById(videoId);
        final Type type = typeService.getById(video.getTypeId());
        video.setLabelNames(type.getLabelNames());

        // 修改视频信息
        video.setOpen(true);
        video.setMsg("该视频违反了TikTok平台的规则，已被下架私密");
        video.setAuditStatus(AuditStatus.PASS);

        // 删除分类库的视频
        interestPushService.deleteSystemTypeStockIn(video);
        // 删除标签库中的视频
        interestPushService.deleteSystemStockIn(video);

        // 获取视频发布者ID，删除对应的发件箱
        final Long userId = video.getUserId();
        redisTemplate.opsForZSet().remove(RedisConstant.OUT_FOLLOW + userId, videoId);

        // 获取视频发布者粉丝，并删除对应的收件箱视频
        final Collection<Long> fansIds = followService.getFans(userId, null);
        feedService.deleteInBoxFeed(userId, Collections.singletonList(videoId));
        feedService.deleteOutBoxFeed(userId, fansIds, videoId);

        // 热门视频以及热度排行榜中的视频
        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DATE);
        // 尝试去找到并删除
        redisTemplate.opsForSet().remove(RedisConstant.HOT_VIDEO + today, videoId);
        redisTemplate.opsForSet().remove(RedisConstant.HOT_VIDEO + (today - 1), videoId);
        redisTemplate.opsForSet().remove(RedisConstant.HOT_VIDEO + (today - 2), videoId);

        redisTemplate.opsForZSet().remove(RedisConstant.HOT_RANK, videoId);

        // 修改视频
        updateById(video);
    }
}
