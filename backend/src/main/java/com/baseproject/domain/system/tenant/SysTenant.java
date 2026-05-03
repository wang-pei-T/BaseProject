package com.baseproject.domain.system.tenant;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_tenant")
public class SysTenant {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String tenantCode;
    private String tenantName;
    private String status;
    private String expireAt;
    private Long adminUserId;
    private Long deletedAt;
    private Long createdAt;
    private Long updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
