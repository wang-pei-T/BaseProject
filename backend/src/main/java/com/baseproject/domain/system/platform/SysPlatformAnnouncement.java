package com.baseproject.domain.system.platform;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_platform_announcement")
public class SysPlatformAnnouncement {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String title;
    private String content;
    private String targetType;
    private String tenantIdsJson;
    private String status;
    private Long createdAt;
    private Long updatedAt;
    private Long publishedAt;
}
