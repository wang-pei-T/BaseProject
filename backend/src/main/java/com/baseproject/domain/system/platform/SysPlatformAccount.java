package com.baseproject.domain.system.platform;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_platform_account")
public class SysPlatformAccount {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String username;
    private String displayName;
    private String phone;
    private String email;
    private String passwordHash;
    private String status;
    private Long deletedAt;
    private Long createdAt;
    private Long updatedAt;
}
