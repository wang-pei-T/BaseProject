package com.baseproject.domain.system.menu;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long parentId;
    private String name;
    private String path;
    private String icon;
    private Integer sort;
    private String status;
    private String menuType;
    private Integer hidden;
    private Long deletedAt;
    private Long createdAt;
    private Long updatedAt;
}
