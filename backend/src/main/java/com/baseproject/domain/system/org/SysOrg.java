package com.baseproject.domain.system.org;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_org")
public class SysOrg {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long parentOrgId;
    private String code;
    private String name;
    private String status;
    private Integer sort;
    private Long deletedAt;
    private Long createdAt;
    private Long updatedAt;
}
