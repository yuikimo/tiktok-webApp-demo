package com.example.tiktok.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.tiktok.entity.user.Favorites;
import com.example.tiktok.entity.user.FavoritesVideo;
import com.example.tiktok.exception.BaseException;
import com.example.tiktok.mapper.user.FavoritesMapper;
import com.example.tiktok.service.user.FavoritesService;
import com.example.tiktok.service.user.FavoritesVideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FavoritesServiceImpl extends ServiceImpl<FavoritesMapper, Favorites> implements FavoritesService {

    @Autowired
    private FavoritesVideoService favoritesVideoService;

    @Override
    @Transactional
    public void remove (Long id, Long userId) {
        // 不能删除默认收藏夹
        final Favorites favorites = getOne(new LambdaQueryWrapper<Favorites>().eq(Favorites::getId, id)
                                                                              .eq(Favorites::getUserId, userId));
        if (favorites.getName().equals("默认收藏夹")) {
            throw new BaseException("默认收藏夹不允许被删除");
        }

        final boolean result = remove(new LambdaQueryWrapper<Favorites>().eq(Favorites::getId, id)
                                                                         .eq(Favorites::getUserId, userId));
        // 如果能删除成功说明是自己的收藏夹
        if (result) {
            favoritesVideoService.remove(new LambdaQueryWrapper<FavoritesVideo>().eq(FavoritesVideo::getFavoritesId, id));
        } else {
            throw new BaseException("非法操作，无法移除他人收藏夹");
        }

    }

    @Override
    public List<Favorites> listByUserId (Long userId) {
        // 查出用户拥有的收藏夹ID
        final List<Favorites> favorites = list(new LambdaQueryWrapper<Favorites>().eq(Favorites::getUserId, userId));
        if (ObjectUtils.isEmpty(favorites)) {
            return Collections.EMPTY_LIST;
        }
        final List<Long> fIds = favorites.stream()
                                         .map(Favorites::getId)
                                         .toList();

        // Key: 收藏夹ID, Value: 视频数量
        final Map<Long, Long> fMap = favoritesVideoService.list(new LambdaQueryWrapper<FavoritesVideo>().in(FavoritesVideo::getFavoritesId, fIds))
                                                          .stream()
                                                          .collect(Collectors.groupingBy(FavoritesVideo::getFavoritesId, Collectors.counting()));

        // 计算对应视频总数
        for (Favorites favorite : favorites) {
            final Long videoCount = fMap.get(favorite.getId());
            favorite.setVideoCount(videoCount == null ? 0 : videoCount);
        }

        return favorites;
    }

    /**
     * 获取收藏夹下的视频ID列表
     * @param favoritesId
     * @param userId
     * @return
     */
    @Override
    public List<Long> listVideoIds (Long favoritesId, Long userId) {
        // 不直接返回中间表是为了隐私性(暂未实现收藏夹公开功能)

        // 校验
        final Favorites favorites = getOne(new LambdaQueryWrapper<Favorites>().eq(Favorites::getId, favoritesId)
                                                                              .eq(Favorites::getUserId, userId));
        if (Objects.isNull(favorites)) {
            throw new BaseException("收藏夹为空");
        }

        final List<Long> videoIds = favoritesVideoService.list(new LambdaQueryWrapper<FavoritesVideo>()
                                                                       .eq(FavoritesVideo::getFavoritesId, favoritesId))
                                                         .stream()
                                                         .map(FavoritesVideo::getVideoId)
                                                         .toList();
        return videoIds;
    }

    /**
     * 收藏 | 取消收藏
     * @param fId
     * @param vId
     * @param uId
     * @return
     */
    @Override
    public Boolean favorites (Long fId, Long vId, Long uId) {
        // 唯一索引
        try {
            final FavoritesVideo favoritesVideo = new FavoritesVideo();
            favoritesVideo.setFavoritesId(fId);
            favoritesVideo.setVideoId(vId);
            favoritesVideo.setUserId(uId);

            favoritesVideoService.save(favoritesVideo);
        } catch (Exception e) {
            favoritesVideoService.remove(new LambdaQueryWrapper<FavoritesVideo>().eq(FavoritesVideo::getFavoritesId, fId)
                                                                                 .eq(FavoritesVideo::getVideoId, vId)
                                                                                 .eq(FavoritesVideo::getUserId, uId));
            return false;
        }
        return true;
    }

    /**
     * 视频收藏状态
     * @param videoId
     * @param userId
     * @return true | false
     */
    @Override
    public Boolean favoritesState (Long videoId, Long userId) {
        if (Objects.isNull(userId)) {
            return false;
        }
        return favoritesVideoService.count(new LambdaQueryWrapper<FavoritesVideo>().eq(FavoritesVideo::getVideoId, videoId)
                                                                                   .eq(FavoritesVideo::getUserId, userId))
               == 1;
    }

    @Override
    public void exist (Long userId, Long fId) {
        final long count = count(new LambdaQueryWrapper<Favorites>().eq(Favorites::getUserId, userId)
                                                                    .eq(Favorites::getId, fId));
        if (count == 0) {
            throw new BaseException("收藏夹选择错误");
        }
    }

}
