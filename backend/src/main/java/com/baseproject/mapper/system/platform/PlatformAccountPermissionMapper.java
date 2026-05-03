package com.baseproject.mapper.system.platform;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PlatformAccountPermissionMapper {

    @Select(
            "SELECT DISTINCT rp.permission_code FROM sys_platform_account_role ar "
                    + "INNER JOIN sys_platform_role_perm rp ON rp.role_id = ar.role_id "
                    + "WHERE ar.account_id = #{accountId}")
    List<String> listPermissionCodesByAccountId(@Param("accountId") Long accountId);
}
