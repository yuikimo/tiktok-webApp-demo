package com.example.tiktok.service.audit;

import com.example.tiktok.config.QiNiuConfig;
import com.example.tiktok.constant.AuditStatus;
import com.example.tiktok.entity.response.AuditResponse;
import com.example.tiktok.entity.task.VideoTask;
import com.example.tiktok.entity.video.Video;
import com.example.tiktok.mapper.video.VideoMapper;
import com.example.tiktok.service.FeedService;
import com.example.tiktok.service.FileService;
import com.example.tiktok.service.InterestPushService;
import com.example.tiktok.service.QiNiuFileService;
import com.example.tiktok.service.user.FollowService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 视频发布审核
 */
@Service
public class VideoPublishAuditServiceImpl
        implements AuditService<VideoTask, VideoTask>, InitializingBean {

    @Autowired
    private FeedService feedService;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private InterestPushService interestPushService;

    @Autowired
    private QiNiuFileService qiNiuFileService;

    @Autowired
    private TextAuditService textAuditService;

    @Autowired
    private ImageAuditService imageAuditService;

    @Autowired
    private VideoAuditService videoAuditService;

    @Autowired
    private FollowService followService;

    @Autowired
    private FileService fileService;

    private int maximumPoolSize = 8;

    protected ThreadPoolExecutor executor;

    /**
     * @param videoTask
     * @param auditQueueState 申请快/慢审核
     * @return
     */
    public VideoTask audit (VideoTask videoTask, Boolean auditQueueState) {
        if (auditQueueState) {
            new Thread(() -> {
                audit(videoTask);
            }).start();
        } else {
            audit(videoTask);
        }
        return null;
    }

    /**
     * 进行任务编排
     * @param videoTask
     * @return
     */
    @Override
    public VideoTask audit (VideoTask videoTask) {
        executor.submit(() -> {
            final Video video = videoTask.getVideo();
            final Video video1 = new Video();
            BeanUtils.copyProperties(video, video1);

            // 视频只有在新增或者公开时候才需要调用审核视频 | 封面
            // 新增：必须审核 修改：新老状态不一致
            // 需要审核视频 | 封面
            boolean needAuditVideo = false;

            // 如果是新增
            if (videoTask.getIsAdd() && videoTask.getOldState() == videoTask.getNewState()) {
                needAuditVideo = true;
            } else if (!videoTask.getIsAdd() && videoTask.getOldState() != videoTask.getNewState()) {
                // 如果为修改：新老状态不一致，说明需要更新
                if (!videoTask.getNewState()) {
                    needAuditVideo = true;
                }
            }

            AuditResponse videoAuditResponse = new AuditResponse(AuditStatus.SUCCESS, "正常");
            AuditResponse converAuditResponse = new AuditResponse(AuditStatus.SUCCESS, "正常");
            AuditResponse titleAuditResponse = new AuditResponse(AuditStatus.SUCCESS, "正常");
            AuditResponse descAuditResponse = new AuditResponse(AuditStatus.SUCCESS, "正常");

            if (needAuditVideo) {
                String videoFileKey = fileService.getById(video.getUrl()).getFileKey();
                videoAuditResponse = videoAuditService.audit(QiNiuConfig.CNAME + "/" + videoFileKey);

                String coverFileKey = fileService.getById(video.getCover()).getFileKey();
                converAuditResponse = imageAuditService.audit(QiNiuConfig.CNAME + "/" + coverFileKey);

                interestPushService.pushSystemTypeStockIn(video1);
                interestPushService.pushSystemStockIn(video1);

                // 推入发件箱
                feedService.pushOutBoxFeed(video.getUserId(), video.getId(), video1.getGmtCreated().getTime());
                // TODO 推入到在线粉丝的收件箱中（结合心跳机制）
            } else if (videoTask.getNewState()) {
                interestPushService.deleteSystemStockIn(video1);
                interestPushService.deleteSystemTypeStockIn(video1);

                // 删除发件箱以及收件箱
                final Collection<Long> fans = followService.getFans(video.getUserId(), null);
                feedService.deleteOutBoxFeed(video.getUserId(), fans, video.getId());
            }

            // 新老视频的标题 | 简介不一致时重新审核
            final Video oldVideo = videoTask.getOldVideo();
            if (!video.getTitle().equals(oldVideo.getTitle())) {
                titleAuditResponse = textAuditService.audit(video.getTitle());
            }
            if (!video.getDescription().equals(oldVideo.getDescription()) &&
                !ObjectUtils.isEmpty(video.getDescription())) {
                descAuditResponse = textAuditService.audit(video.getDescription());
            }

            boolean videoAuditStatus = videoAuditResponse.getAuditStatus() == AuditStatus.SUCCESS;
            boolean coverAuditStatus  = converAuditResponse.getAuditStatus() == AuditStatus.SUCCESS;
            boolean titleAuditStatus = titleAuditResponse.getAuditStatus() == AuditStatus.SUCCESS;
            boolean descAuditStatus = descAuditResponse.getAuditStatus()== AuditStatus.SUCCESS;

            if (videoAuditStatus && coverAuditStatus && titleAuditStatus && descAuditStatus) {
                video1.setMsg("通过");
                video1.setAuditStatus(AuditStatus.SUCCESS);

                // 填充视频时长
            } else {
                video1.setAuditStatus(AuditStatus.PASS);
                // 避免干扰
                video1.setMsg("");

                if (!videoAuditStatus) {
                    video1.setMsg("视频有违规行为：" + videoAuditResponse.getMsg());
                }
                if (!coverAuditStatus) {
                    video1.setMsg(video1.getMsg() + "\n封面有违规行为：" + converAuditResponse.getMsg());
                }
                if (!titleAuditStatus) {
                    video1.setMsg(video1.getMsg() + "\n标题有违规行为：" + titleAuditResponse.getMsg());
                }
                if (!descAuditStatus) {
                    video1.setMsg(video.getMsg() + "\n简介有违规行为：" + descAuditResponse.getMsg());
                }
            }
            videoMapper.updateById(video1);
        });
        return null;
    }

    public boolean getAuditQueueState () {
        return executor.getTaskCount() < maximumPoolSize;
    }

    @Override
    public void afterPropertiesSet () {
        executor = new ThreadPoolExecutor(5, maximumPoolSize, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000));
    }
}
