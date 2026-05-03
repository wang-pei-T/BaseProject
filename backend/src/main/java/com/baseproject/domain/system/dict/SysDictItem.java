package com.baseproject.domain.system.dict;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_dict_item")
public class SysDictItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long dictTypeId;
    private String code;
    private String label;
    private String itemValue;
    private Integer sort;
    private String status;
    private Long deletedAt;
    private Long createdAt;
    private Long updatedAt;
}
