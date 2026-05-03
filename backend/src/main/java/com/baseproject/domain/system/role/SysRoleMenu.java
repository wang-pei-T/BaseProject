package com.baseproject.domain.system.role;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role_menu")
public class SysRoleMenu {

    private Long tenantId;
    private Long roleId;
    private Long menuId;
    private Long createdAt;
}
