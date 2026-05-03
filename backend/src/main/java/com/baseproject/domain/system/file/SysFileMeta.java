package com.baseproject.domain.system.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_file_meta")
public class SysFileMeta {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String bucketName;
    private String objectKey;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private String sha256Hex;
    private String bizHint;
    private Long createdBy;
    private Long deletedAt;
    private Long createdAt;
}
