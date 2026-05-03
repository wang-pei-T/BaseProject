package com.baseproject.domain.system.platform;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_platform_role_perm")
public class SysPlatformRolePerm {

    private Long roleId;
    private String permissionCode;
    private Long createdAt;
}
