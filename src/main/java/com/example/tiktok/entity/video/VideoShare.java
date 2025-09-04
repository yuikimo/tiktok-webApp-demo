package com.example.tiktok.entity.video;

import com.example.tiktok.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class VideoShare extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long videoId;

    private Long userId;

    private String ip;

}
