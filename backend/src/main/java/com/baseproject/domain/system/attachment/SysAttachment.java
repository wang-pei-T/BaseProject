package com.baseproject.domain.system.attachment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_attachment")
public class SysAttachment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long fileId;
    private String bizType;
    private String bizId;
    private String tag;
    private String fileName;
    private Long sizeBytes;
    private String contentType;
    private Long createdBy;
    private Long deletedAt;
    private Long createdAt;
}
