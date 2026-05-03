package com.baseproject.domain.system.platform;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_platform_account_role")
public class SysPlatformAccountRole {

    private Long accountId;
    private Long roleId;
    private Long createdAt;
}
