package com.example.tiktok.controller;

import com.example.tiktok.config.LocalCache;
import com.example.tiktok.config.QiNiuConfig;
import com.example.tiktok.entity.File;
import com.example.tiktok.entity.Setting;
import com.example.tiktok.holder.UserHolder;
import com.example.tiktok.service.FileService;
import com.example.tiktok.service.SettingService;
import com.example.tiktok.utils.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@RestController
@RequestMapping("/tiktok/file")
public class FileController implements InitializingBean {

    @Autowired
    SettingService settingService;

    @Autowired
    FileService fileService;

    @Autowired
    QiNiuConfig qiNiuConfig;

    /**
     * 保存到文件表
     * @throws Exception
     */
    @PostMapping
    public R save(String fileKey) {
        return R.ok()
                .data(fileService.save(fileKey, UserHolder.get()));
    }

    @GetMapping("/getToken")
    public R token(String type) {
        return R.ok()
                .data(qiNiuConfig.uploadToken(type));
    }

    @GetMapping("/{fileId}")
    public void getUUID(HttpServletRequest request, HttpServletResponse response, @PathVariable Long fileId) throws IOException {

        // String ip = request.getHeader("referer");
        // if (!LocalCache.containsKey(ip)) {
        //     response.sendError(HttpServletResponse.SC_FORBIDDEN);
        //     return;
        // }

        // 如果不是指定IP调用的该接口，则不返回
        File url = fileService.getFileTrustUrl(fileId);
        response.setContentType(URLEncoder.encode(url.getType(), StandardCharsets.UTF_8));
        response.sendRedirect(url.getFileKey());
    }

    @PostMapping("/auth")
    public void auth(@RequestParam(required = false) String uuid, HttpServletResponse response) throws IOException {
        if (Objects.isNull(uuid) || !LocalCache.containsKey(uuid)) {
            response.sendError(401);
        } else {
            LocalCache.rem(uuid);
            response.sendError(200);
        }
    }

    @Override
    public void afterPropertiesSet () {
        final Setting setting = settingService.list().get(0);
        for (String s : setting.getAllowIp().split(",")) {
            LocalCache.put(s, true);
        }
    }
}
