package com.example.tiktok.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.tiktok.constant.AuditStatus;
import com.example.tiktok.constant.RedisConstant;
import com.example.tiktok.entity.response.AuditResponse;
import com.example.tiktok.entity.user.Favorites;
import com.example.tiktok.entity.user.User;
import com.example.tiktok.entity.user.UserSubscribe;
import com.example.tiktok.entity.video.Type;
import com.example.tiktok.entity.vo.*;
import com.example.tiktok.exception.BaseException;
import com.example.tiktok.holder.UserHolder;
import com.example.tiktok.mapper.user.UserMapper;
import com.example.tiktok.service.FileService;
import com.example.tiktok.service.InterestPushService;
import com.example.tiktok.service.audit.ImageAuditService;
import com.example.tiktok.service.audit.TextAuditService;
import com.example.tiktok.service.user.FavoritesService;
import com.example.tiktok.service.user.FollowService;
import com.example.tiktok.service.user.UserService;
import com.example.tiktok.service.user.UserSubscribeService;
import com.example.tiktok.service.video.TypeService;
import com.example.tiktok.utils.RedisCacheUtil;
import com.example.tiktok.utils.SaltMD5Util;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private TypeService typeService;

    @Autowired
    private UserSubscribeService userSubscribeService;

    @Autowired
    private FollowService followService;

    @Autowired
    private RedisCacheUtil redisCacheUtil;

    @Autowired
    private FileService fileService;

    @Autowired
    private InterestPushService interestPushService;

    @Autowired
    private FavoritesService favoritesService;

    @Autowired
    private TextAuditService textAuditService;

    @Autowired
    private ImageAuditService imageAuditService;

    @Override
    public boolean register(RegisterVO registerVO) throws Exception {
        final long count = count(new LambdaQueryWrapper<User>().eq(User::getEmail, registerVO.getEmail()));
        if (count == 1) {
            throw new BaseException("邮箱已被注册");
        }

        final String code = registerVO.getCode();
        final Object o = redisCacheUtil.get(RedisConstant.EMAIL_CODE + registerVO.getEmail());
        if (o == null) {
            throw new BaseException("验证码为空");
        }
        if (!code.equals(o)) {
            return false;
        }

        final User user = new User();
        user.setNickName(registerVO.getNickName());
        user.setEmail(registerVO.getEmail());
        user.setDescription("这个人很懒...");
        user.setPassword(SaltMD5Util.generateSaltPassword(registerVO.getPassword()));
        save(user);

        // 创建默认收藏夹
        final Favorites favorites = new Favorites();
        favorites.setUserId(user.getId());
        favorites.setName("默认收藏夹");
        favoritesService.save(favorites);

        // 这里如果单独抽出一个用户配置表就好了,但是没有必要再搞个表
        user.setDefaultFavoritesId(favorites.getId());
        updateById(user);

        return true;
    }

    @Override
    public UserVO getInfo(Long userId) {
        final User user = getById(userId);
        if (ObjectUtils.isEmpty(user)) {
            return new UserVO();
        }

        final UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);

        // 查出关注数量
        final long followCount = followService.getFollowCount(userId);

        // 查出粉丝数量
        final long fansCount = followService.getFansCount(userId);

        userVO.setFollow(followCount);
        userVO.setFans(fansCount);
        return userVO;
    }

    /**
     * 将用户的基本信息封装为Map集合
     * @param userIds
     * @return Key: userId Value: User
     */
    private Map<Long, User> getBaseInfoUserToMap(Collection<Long> userIds) {
        List<User> users = new ArrayList<>();
        if (!ObjectUtils.isEmpty(userIds)) {
            users = list(new LambdaQueryWrapper<User>().in(User::getId, userIds)
                                                       .select(User::getId, User::getNickName,
                                                               User::getDescription, User::getSex, User::getAvatar));
        }
        return users.stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    @Override
    public Page<User> getFollows(Long userId, BasePage basePage) {
        Page<User> page = new Page<>();

        // 获取用户的关注列表
        final Collection<Long> followsIds = followService.getFollow(userId, basePage);
        if (ObjectUtils.isEmpty(followsIds)) {
            return page;
        }

        // 获取用户的粉丝列表
        final HashSet<Long> fans = new HashSet<>();
        // 这里需要将数据转换，因为存储到Redis中数值小的使用int保存，取出来需要用long进行比较
        fans.addAll(followService.getFans(userId, null));

        // key 关注ID value 是否互关
        Map<Long, Boolean> map = new HashMap<>();
        for (Long followId : followsIds) {
            map.put(followId, fans.contains(followId));
        }

        final ArrayList<User> users = new ArrayList<>();
        final Map<Long, User> userMap = getBaseInfoUserToMap(map.keySet());

        for (Long followId : followsIds) {
            final User user = userMap.get(followId);
            user.setEach(map.get(user.getId()));
            users.add(user);
        }
        page.setRecords(users);
        page.setTotal(users.size());

        return page;
    }

    @Override
    public Page<User> getFans(Long userId, BasePage basePage) {
        final Page<User> page = new Page<>();

        // 获取用户粉丝列表
        final Collection<Long> fansIds = followService.getFans(userId, basePage);
        if (ObjectUtils.isEmpty(fansIds)) {
            return page;
        }
        // 获取用户关注列表
        final HashSet<Long> followIds = new HashSet<>();
        followIds.addAll(followService.getFollow(userId, null));

        Map<Long, Boolean> map = new HashMap<>();
        // 遍历粉丝，查看关注列表中是否有
        for (Long fansId : fansIds) {
            map.put(fansId, followIds.contains(fansId));
        }

        final ArrayList<User> users = new ArrayList<>();
        final Map<Long, User> userMap = getBaseInfoUserToMap(map.keySet());
        // 遍历粉丝列表，保证有序性
        for (Long fansId : fansIds) {
            final User user = userMap.get(fansId);
            user.setEach(map.get(user.getId()));
            users.add(user);
        }

        page.setRecords(users);
        page.setTotal(users.size());
        return page;
    }

    /**
     * 获取用户基本信息列表
     * @param userIds
     * @return
     */
    @Override
    public List<User> list(Collection<Long> userIds) {
        return list(new LambdaQueryWrapper<User>().in(User::getId, userIds)
                                                  .select(User::getId, User::getNickName, User::getSex,
                                                          User::getAvatar, User::getDescription));
    }

    /**
     * 修改模型
     * @param modelVO
     */
    public void updateModel(ModelVO modelVO) {
        interestPushService.initUserModel(modelVO.getUserId(), modelVO.getLabels());
    }

    @Override
    @Transactional
    public void subscribe(Set<Long> typeIds, Long userId) {
        if (ObjectUtils.isEmpty(typeIds)) {
            return;
        }

        // 校验分类
        final Collection<Type> types = typeService.listByIds(typeIds);
        if (typeIds.size() != types.size()) {
            throw new BaseException("不存在的分类");
        }

        final ArrayList<UserSubscribe> userSubscribes = new ArrayList<>();
        for (Long typeId : typeIds) {
            final UserSubscribe userSubscribe = new UserSubscribe();
            userSubscribe.setUserId(userId);
            userSubscribe.setTypeId(typeId);
            userSubscribes.add(userSubscribe);
        }

        // 删除之前的
        userSubscribeService.remove(new LambdaQueryWrapper<UserSubscribe>().eq(UserSubscribe::getUserId, userId));
        userSubscribeService.saveBatch(userSubscribes);

        // 更新用户模型
        final ModelVO modelVO = new ModelVO();
        modelVO.setUserId(userId);
        // 获取分类下的标签列表
        List<String> labels = new ArrayList<>();
        for (Type type : types) {
            labels.addAll(type.buildLabel());
        }
        modelVO.setLabels(labels);
        updateModel(modelVO);
    }

    @Override
    public Collection<Type> listSubscribeType(Long userId) {
        if (Objects.isNull(userId)) {
            return Collections.EMPTY_SET;
        }

        final List<Long> typeIds = userSubscribeService.list(new LambdaQueryWrapper<UserSubscribe>()
                                                                     .eq(UserSubscribe::getUserId, userId))
                                                       .stream()
                                                       .map(UserSubscribe::getTypeId)
                                                       .toList();

        if (ObjectUtils.isEmpty(typeIds)) {
            return Collections.EMPTY_SET;
        }

        final List<Type> types = typeService.list(new LambdaQueryWrapper<Type>().in(Type::getId, typeIds)
                                                                                .select(Type::getId, Type::getName,
                                                                                        Type::getIcon));
        return types;
    }

    /**
     * 关注 | 取关
     * @param followsUserId
     * @param userId
     * @return
     */
    @Override
    public boolean follows(Long followsUserId, Long userId) {
        return followService.follows(followsUserId, userId);
    }

    @Override
    public void updateUserModel(UserModel userModel) {
        interestPushService.updateUserModel(userModel);
    }

    @Override
    public Boolean findPassword(FindPWVO findPWVO) {

        // 从Redis中取出验证码
        final Object o = redisCacheUtil.get(RedisConstant.EMAIL_CODE + findPWVO.getEmail());
        if (Objects.isNull(o)) {
            return false;
        }

        // 校验
        if (Integer.parseInt(o.toString()) != findPWVO.getCode()) {
            return false;
        }

        // 修改
        final User user = new User();
        user.setEmail(findPWVO.getEmail());
        final String saltPassword = SaltMD5Util.generateSaltPassword(findPWVO.getNewPassword());
        user.setPassword(saltPassword);

        update(user, new UpdateWrapper<User>().lambda()
                                              .set(User::getPassword, saltPassword)
                                              .eq(User::getEmail, findPWVO.getEmail()));
        return true;
    }

    /**
     * 更新用户信息
     * @param user
     */
    @Override
    public void updateUser(UpdateUserVO user) {
        final Long userId = UserHolder.get();
        final User oldUser = getById(userId);
        // 需要审核
        if (!oldUser.getNickName().equals(user.getNickName())) {
            oldUser.setNickName(user.getNickName());
            final AuditResponse audit = textAuditService.audit(user.getNickName());
            if (audit.getAuditStatus() != AuditStatus.SUCCESS) {
                throw new BaseException(audit.getMsg());
            }
        }
        if (!ObjectUtils.isEmpty(user.getDescription()) && !oldUser.getDescription().equals(user.getDescription())) {
            oldUser.setDescription(user.getDescription());
            final AuditResponse audit = textAuditService.audit(user.getNickName());
            if (audit.getAuditStatus() != AuditStatus.SUCCESS) {
                throw new BaseException(audit.getMsg());
            }
        }
        if (!Objects.equals(user.getAvatar(), oldUser.getAvatar())) {
            final AuditResponse audit = imageAuditService.audit(fileService.getById(user.getAvatar()).getFileKey());
            if (audit.getAuditStatus() != AuditStatus.SUCCESS) {
                throw new BaseException(audit.getMsg());
            }
            oldUser.setAvatar(user.getAvatar());
        }

        if (!ObjectUtils.isEmpty(user.getDefaultFavoritesId())) {
            // 校验收藏夹
            favoritesService.exist(userId, user.getDefaultFavoritesId());
        }

        oldUser.setSex(user.getSex());
        oldUser.setDefaultFavoritesId(user.getDefaultFavoritesId());

        updateById(oldUser);
    }

    /**
     * 从Redis中取出用户的历史记录
     * @param userId
     * @return
     */
    @Override
    public Collection<String> searchHistory(Long userId) {
        List<String> searches = new ArrayList<>();
        if (!Objects.isNull(userId)) {
            searches.addAll(redisCacheUtil.zGet(RedisConstant.USER_SEARCH_HISTORY + userId));
            // 截取搜索记录的前20条（如果不足20条，则返回全部）
            searches = searches.subList(0, Math.min(searches.size(), 20));
        }
        return searches;
    }

    @Override
    @Async
    public void addSearchHistory(Long userId, String search) {
        if (!Objects.isNull(userId)) {
            redisCacheUtil.zAdd(RedisConstant.USER_SEARCH_HISTORY + userId, new Date().getTime(),
                                search, RedisConstant.USER_SEARCH_HISTORY_TIME);
        }
    }

    @Override
    public void deleteSearchHistory(Long userId) {
        if (!Objects.isNull(userId)) {
            redisCacheUtil.del(RedisConstant.USER_SEARCH_HISTORY + userId);
        }
    }

    @Override
    public Collection<Type> listNoSubscribeType(Long userId) {
        // 获取用户订阅的分类
        final Set<Long> set = listSubscribeType(userId).stream()
                                                       .map(Type::getId)
                                                       .collect(Collectors.toSet());
        // 获取所有分类
        final List<Type> allType = typeService.list();

        final ArrayList<Type> types = new ArrayList<>();
        for (Type type : allType) {
            if (!set.contains(type.getId())) {
                types.add(type);
            }
        }

        return types;
    }

    /**
     * 将用户ID列表封装为User对象返回
     * @param ids
     * @return
     */
    public List<User> getUsers(Collection<Long> ids) {
        final Map<Long, User> userMap = listByIds(ids).stream()
                                                      .collect(Collectors.toMap(User::getId, Function.identity()));

        List<User> result = new ArrayList<>();

        for (Long followId : ids) {
            final User user = new User();
            final User u = userMap.get(followId);
            user.setId(followId);
            user.setNickName(u.getNickName());
            user.setSex(u.getSex());
            user.setDescription(u.getDescription());

            result.add(user);
        }
        return result;
    }

}
