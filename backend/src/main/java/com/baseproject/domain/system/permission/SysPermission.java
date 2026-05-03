package com.baseproject.domain.system.permission;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_permission")
public class SysPermission {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String code;
    private String name;
    private String module;
    private String resourceType;
    private String actionType;
    private String status;
    private Long deletedAt;
    private Long createdAt;
    private Long updatedAt;
}
