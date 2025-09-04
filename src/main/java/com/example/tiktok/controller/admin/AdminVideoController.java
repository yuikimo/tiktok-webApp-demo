package com.example.tiktok.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.tiktok.authority.Authority;
import com.example.tiktok.constant.AuditStatus;
import com.example.tiktok.entity.user.User;
import com.example.tiktok.entity.video.Type;
import com.example.tiktok.entity.video.Video;
import com.example.tiktok.entity.vo.BasePage;
import com.example.tiktok.entity.vo.VideoStatistics;
import com.example.tiktok.holder.UserHolder;
import com.example.tiktok.service.user.UserService;
import com.example.tiktok.service.video.TypeService;
import com.example.tiktok.service.video.VideoService;
import com.example.tiktok.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 视频管理端
 */
@RestController
@RequestMapping("/admin/video")
public class AdminVideoController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private UserService userService;

    @Autowired
    private TypeService typeService;

    @GetMapping("/{id}")
    @Authority("admin:video:get")
    public R get(@PathVariable Long id) {
        return R.ok()
                .data(videoService.getVideoById(id, null));
    }

    @GetMapping("/page")
    @Authority("admin:video:page")
    public R list(BasePage basePage, @RequestParam(required=false) String YV,
                  @RequestParam(required=false) String title) {
        final LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();

        wrapper.like(!ObjectUtils.isEmpty(YV), Video::getYv, YV)
               .like(!ObjectUtils.isEmpty(title), Video::getTitle, title);

        final IPage<Video> page = videoService.page(basePage.page(), wrapper);
        final List<Video> records = page.getRecords();

        if (ObjectUtils.isEmpty(records)) {
            return R.ok();
        }

        final ArrayList<Long> userIds = new ArrayList<>();
        final ArrayList<Long> typeIds = new ArrayList<>();

        for (Video video : records) {
            userIds.add(video.getUserId());
            typeIds.add(video.getTypeId());
        }

        final Map<Long, String> userMap =
                userService.list(new LambdaQueryWrapper<User>().select(User::getId, User::getNickName)
                                                               .in(User::getId, userIds))
                           .stream()
                           .collect(Collectors.toMap(User::getId, User::getNickName));

        final Map<Long, String> typeMap = typeService.listByIds(typeIds)
                                                     .stream()
                                                     .collect(Collectors.toMap(Type::getId, Type::getName));

        for (Video video : records) {
            video.setAuditStateName(AuditStatus.getName(video.getAuditStatus()));
            video.setUserName(userMap.get(video.getUserId()));
            video.setOpenName(video.getOpen() ? "私密" : "公开");
            video.setTypeName(typeMap.get(video.getTypeId()));
        }

        return R.ok()
                .data(records)
                .count(page.getTotal());
    }

    /**
     * 删除视频
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @Authority("admin:video:delete")
    public R delete(@PathVariable Long id) {
        videoService.deleteVideo(id, UserHolder.get());
        return R.ok()
                .message("删除成功");
    }

    /**
     * 放行视频
     * @param video
     * @return
     */
    @PostMapping("/audit")
    @Authority("admin:video:audit")
    public R audit(@RequestBody Video video) {
        videoService.auditProcess(video);
        return R.ok()
                .message("审核放行");
    }

    /**
     * 下架视频
     * @param id
     * @return
     */
    @PostMapping("/violations/{id}")
    @Authority("admin:video:violations")
    public R Violations(@PathVariable Long id) {
        videoService.violations(id);
        return R.ok()
                .message("下架成功");
    }

    /**
     * 视频审核进度分析
     * @return
     */
    @GetMapping("/statistics")
    @Authority("admin:video:statistics")
    public R show() {
        final VideoStatistics videoStatistics = new VideoStatistics();

        final int allCount = (int) videoService.count();
        final int processCount = (int) videoService.count(new LambdaQueryWrapper<Video>().eq(Video::getAuditStatus,
                                                                                             AuditStatus.PROCESS));
        final int successCount = (int) videoService.count(new LambdaQueryWrapper<Video>().eq(Video::getAuditStatus,
                                                                                             AuditStatus.SUCCESS));
        final int passCount = (int) videoService.count(new LambdaQueryWrapper<Video>().eq(Video::getAuditStatus,
                                                                                          AuditStatus.PASS));

        videoStatistics.setAllCount(allCount);
        videoStatistics.setPassCount(passCount);
        videoStatistics.setSuccessCount(successCount);
        videoStatistics.setProcessCount(processCount);

        return R.ok()
                .data(videoStatistics);
    }
}
