package com.example.tiktok.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.tiktok.entity.user.Favorites;

import java.util.List;

public interface FavoritesService extends IService<Favorites> {

    /**
     * 删除收藏夹，收藏夹下的视频一并删除
     * @param id
     * @param userId
     */
    void remove(Long id, Long userId);

    /**
     * 根据用户获取收藏夹
     * @param userId
     * @return
     */
    List<Favorites> listByUserId(Long userId);

    /**
     * 获取收藏夹下的所有视频ID
     * @param favoritesId
     * @param userId
     * @return
     */
    List<Long> listVideoIds(Long favoritesId, Long userId);

    /**
     * 收藏视频
     * @param fId
     * @param vId
     * @return
     */
    Boolean favorites(Long fId, Long vId, Long uId);

    /**
     * 收藏状态
     * @param videoId
     * @param userId
     * @return
     */
    Boolean favoritesState(Long videoId, Long userId);

    /**
     * 判断用户是否拥有该收藏夹
     * @param userId
     * @param fId
     */
    void exist(Long userId, Long fId);
}
