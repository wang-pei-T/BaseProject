package com.baseproject.mapper.system.profile;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysUserNotificationPrefMapper {

    @Select("SELECT preferences FROM sys_user_notification_pref WHERE tenant_id = #{tid} AND user_id = #{uid}")
    String selectPreferences(@Param("tid") long tid, @Param("uid") long uid);

    @Insert("INSERT INTO sys_user_notification_pref(tenant_id,user_id,preferences,updated_at) VALUES (#{tid},#{uid},#{prefs},#{ts})")
    int insertPrefs(@Param("tid") long tid, @Param("uid") long uid, @Param("prefs") String prefs, @Param("ts") long ts);

    @Update("UPDATE sys_user_notification_pref SET preferences = #{prefs}, updated_at = #{ts} WHERE tenant_id = #{tid} AND user_id = #{uid}")
    int updatePrefs(@Param("tid") long tid, @Param("uid") long uid, @Param("prefs") String prefs, @Param("ts") long ts);
}
