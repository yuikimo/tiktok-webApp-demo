package com.example.tiktok.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.tiktok.config.LocalCache;
import com.example.tiktok.config.QiNiuConfig;
import com.example.tiktok.entity.File;
import com.example.tiktok.exception.BaseException;
import com.example.tiktok.mapper.FileMapper;
import com.example.tiktok.service.FileService;
import com.example.tiktok.service.QiNiuFileService;
import com.qiniu.storage.model.FileInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, File> implements FileService {

    @Autowired
    private QiNiuFileService qiNiuFileService;

    @Override
    public Long save(String fileKey, Long userId) {
        // 判断文件
        final FileInfo videoFileInfo = qiNiuFileService.getFileInfo(fileKey);
        if (videoFileInfo == null) {
            throw new IllegalArgumentException("参数不正确");
        }

        final File videoFile = new File();
        String type = videoFileInfo.mimeType;
        videoFile.setFileKey(fileKey);
        videoFile.setFormat(type);
        videoFile.setType(type.contains("video") ? "视频" : "图片");
        videoFile.setUserId(userId);
        videoFile.setSize(videoFileInfo.fsize);

        save(videoFile);
        return videoFile.getId();
    }

    /**
     * 将视频的第一帧作为封面
     * @param fileId
     * @param userId
     * @return
     */
    @Override
    public Long generatePhoto(Long fileId, Long userId) {
        final File file = getById(fileId);
        final String fileKey = file.getFileKey() + "?vframe/jpg/offset/1";

        final File fileInfo = new File();
        fileInfo.setFileKey(fileKey);
        fileInfo.setFormat("image/*");
        fileInfo.setType("图片");
        fileInfo.setUserId(userId);

        save(fileInfo);
        return fileInfo.getId();
    }

    @Override
    public File getFileTrustUrl(Long fileId) {
        File file = getById(fileId);
        if (Objects.isNull(file)) {
            throw new BaseException("未找到该文件");
        }

        final String s = UUID.randomUUID().toString();
        LocalCache.put(s, true);

        String url = QiNiuConfig.CNAME + "/" + file.getFileKey();

        if (url.contains("?")) {
            url = url + "&uuid=" + s;
        } else {
            url = url + "?uuid=" + s;
        }
        file.setFileKey(url);
        return file;
    }
}
