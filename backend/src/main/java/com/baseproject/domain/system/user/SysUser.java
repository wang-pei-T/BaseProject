package com.baseproject.domain.system.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long orgId;
    private String username;
    private String displayName;
    private String phone;
    private String email;
    private String passwordHash;
    private String status;
    private String lockStatus;
    private Long deletedAt;
    private Long createdAt;
    private Long updatedAt;
}
