package com.baseproject.domain.system.platform;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_platform_audit")
public class SysPlatformAudit {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String event;
    private Long operatorAccountId;
    private Long targetTenantId;
    private String requestId;
    private String extraJson;
    private Long createdAt;
}
