package com.example.tiktok.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.tiktok.entity.File;

public interface FileService extends IService<File> {

    Long save(String fileKey, Long userId);

    /**
     * 根据视频ID生成图片
     * @param fileId
     * @param userId
     * @return
     */
    Long generatePhoto(Long fileId, Long userId);

    /**
     * 获取文件真实URL
     * @param fileId 文件ID
     * @return
     */
    File getFileTrustUrl(Long fileId);
}
