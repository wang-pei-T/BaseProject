package com.baseproject.domain.system.action;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_action")
public class SysAction {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long menuId;
    private String code;
    private String name;
    private String status;
    private Long deletedAt;
    private Long createdAt;
    private Long updatedAt;
}
