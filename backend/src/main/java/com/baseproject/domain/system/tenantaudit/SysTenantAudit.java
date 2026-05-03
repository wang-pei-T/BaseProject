package com.baseproject.domain.system.tenantaudit;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_tenant_audit")
public class SysTenantAudit {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String event;
    private Long operatorUserId;
    private String targetId;
    private String diffText;
    private String contextText;
    private String requestId;
    private String operatorIp;
    private String userAgent;
    private Long createdAt;
}
