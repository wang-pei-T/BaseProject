package com.baseproject.domain.system.config;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_config")
public class SysConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String confKey;
    private String confValue;
    private String valueType;
    private Long version;
    private Long updatedBy;
    private Long deletedAt;
    private Long createdAt;
    private Long updatedAt;
}
