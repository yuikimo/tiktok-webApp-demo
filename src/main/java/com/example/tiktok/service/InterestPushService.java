package com.example.tiktok.service;

import com.example.tiktok.entity.user.User;
import com.example.tiktok.entity.video.Video;
import com.example.tiktok.entity.vo.UserModel;

import java.util.Collection;
import java.util.List;

/**
 * 兴趣推送
 */
public interface InterestPushService {

    /**
     * 推入标签库
     * @param video
     */
    void pushSystemStockIn(Video video);

    /**
     * 添加分类库，用于后续随机推送分类视频
     * @param video
     */
    void pushSystemTypeStockIn(Video video);

    /**
     * 根据分类随机推送视频
     * @param typeId
     * @return
     */
    Collection<Long> listVideoIdByTypeId(Long typeId);

    /**
     * 删除标签内视频
     * @param video
     */
    void deleteSystemStockIn(Video video);

    /**
     * 删除分类库中的视频
     * @param video
     */
    void deleteSystemTypeStockIn(Video video);

    /**
     * 用户初始化模型 -> 订阅分类
     * @param userId 用户ID
     * @param labels 标签名
     */
    void initUserModel(Long userId, List<String> labels);

    /**
     * 用户模型修改概率: 可分批次发送
     * 修改场景：
     * 1. 观看浏览量到达总时长 1/5 +1 概率
     * 2. 观看浏览量未到总时长 1/5 -0.5 概率
     * 3. 点赞视频 +2 概率
     * 4. 收藏视频 +3 概率
     * @param userModel
     */
    void updateUserModel(UserModel userModel);

    /**
     * 用于给用户推送视频 -> 兴趣推送
     * 推送 X 条视频，包含一条和性别相关的
     * @param user 传ID和SEX
     * @return videoIds
     */
    Collection<Long> listVideoIdByUserModel(User user);

    Collection<Long> listVideoIdByLabels(List<String> labelNames);

}
