package com.baseproject.domain.system.message;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_message")
public class SysMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private String linkUrl;
    private String payloadJson;
    private String status;
    private Long readAt;
    private Long deletedAt;
    private Long createdAt;
}
