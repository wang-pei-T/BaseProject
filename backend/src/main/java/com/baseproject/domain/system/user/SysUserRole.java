package com.baseproject.domain.system.user;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_user_role")
public class SysUserRole {

    private Long tenantId;
    private Long userId;
    private Long roleId;
    private Long createdAt;
}
