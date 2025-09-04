package com.example.tiktok.controller.admin;

import com.example.tiktok.authority.Authority;
import com.example.tiktok.config.LocalCache;
import com.example.tiktok.entity.Setting;
import com.example.tiktok.entity.json.SettingScoreJson;
import com.example.tiktok.service.SettingService;
import com.example.tiktok.utils.R;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/setting")
public class AdminSettingController {

    @Autowired
    private SettingService settingService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    @Authority("admin:setting:get")
    public R get() throws JsonProcessingException {
        final Setting setting = settingService.list().get(0);
        final SettingScoreJson settingScoreJson = objectMapper.readValue(setting.getAuditPolicy(), SettingScoreJson.class);
        setting.setSettingScoreJson(settingScoreJson);

        return R.ok()
                .data(setting);
    }

    @PutMapping
    @Authority("admin:setting:update")
    public R update(@RequestBody @Validated Setting setting) {
        settingService.updateById(setting);
        for (String s : setting.getAllowIp().split(",")) {
            LocalCache.put(s, true);
        }

        return R.ok()
                .message("修改成功");
    }
}
