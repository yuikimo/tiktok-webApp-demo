package com.example.tiktok.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.tiktok.entity.json.SettingScoreJson;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_setting")
public class Setting implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    // 审核策略
    private String auditPolicy;

    // 热门视频热度限制
    private Double hotLimit;

    // 审核开关
    private Boolean auditOpen;

    // 资源放行IP
    private String allowIp;
    // 回源鉴权开关,0关闭，1开启,默认为1
    private Boolean auth;

    @TableField(exist = false)
    private SettingScoreJson settingScoreJson;
}
