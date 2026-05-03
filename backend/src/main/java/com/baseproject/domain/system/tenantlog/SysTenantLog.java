package com.baseproject.domain.system.tenantlog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_tenant_log")
public class SysTenantLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String level;
    private String module;
    private String action;
    private String message;
    private String requestId;
    private String traceId;
    private Long createdAt;
}
