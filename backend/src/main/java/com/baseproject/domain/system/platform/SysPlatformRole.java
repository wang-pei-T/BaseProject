package com.baseproject.domain.system.platform;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_platform_role")
public class SysPlatformRole {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String code;
    private String name;
    private String status;
    private Long deletedAt;
    private Long createdAt;
    private Long updatedAt;
}
