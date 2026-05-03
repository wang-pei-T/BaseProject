-- 初始化数据（依赖 init_schema.sql 已执行）
USE `baseproject`;

SET @ts = 1704067200000;
SET @default_password_plain = 'Admin@123456';
SET @default_password_hash = '$2a$10$rRMC6.NuB2a5VWDHHP3AquE7wufr0Lneb54CqKJCqsgfOrp6uW8NW';

INSERT INTO `sys_org` (`id`,`tenant_id`,`parent_org_id`,`code`,`name`,`status`,`sort`,`deleted_at`,`created_at`,`updated_at`) VALUES
(1, 1, NULL, 'DEFAULT', '默认机构', 'ENABLED', 0, NULL, @ts, @ts);

INSERT INTO `sys_user` (`id`,`tenant_id`,`org_id`,`username`,`display_name`,`phone`,`email`,`password_hash`,`status`,`lock_status`,`deleted_at`,`created_at`,`updated_at`) VALUES
(1, 1, 1, 'admin', '系统管理员', NULL, NULL, @default_password_hash, 'ENABLED', 'UNLOCKED', NULL, @ts, @ts);

INSERT INTO `sys_tenant` (`id`,`tenant_code`,`tenant_name`,`status`,`expire_at`,`admin_user_id`,`deleted_at`,`created_at`,`updated_at`,`created_by`,`updated_by`) VALUES
(1, 'default', '默认租户', 'ENABLED', NULL, 1, NULL, @ts, @ts, NULL, NULL);

INSERT INTO `sys_role` (`id`,`tenant_id`,`code`,`name`,`status`,`deleted_at`,`created_at`,`updated_at`) VALUES
(1, 1, 'TENANT_ADMIN', '租户管理员', 'ENABLED', NULL, @ts, @ts);

INSERT INTO `sys_permission` (`id`,`tenant_id`,`code`,`name`,`module`,`resource_type`,`action_type`,`status`,`deleted_at`,`created_at`,`updated_at`) VALUES
(10001,1,'tenant.user.create','新增用户','user','user','create','ENABLED',NULL,@ts,@ts),
(10002,1,'tenant.user.update','编辑用户','user','user','update','ENABLED',NULL,@ts,@ts),
(10003,1,'tenant.user.enable','启用用户','user','user','enable','ENABLED',NULL,@ts,@ts),
(10004,1,'tenant.user.disable','禁用用户','user','user','disable','ENABLED',NULL,@ts,@ts),
(10005,1,'tenant.user.lock','锁定用户','user','user','lock','ENABLED',NULL,@ts,@ts),
(10006,1,'tenant.user.unlock','解锁用户','user','user','unlock','ENABLED',NULL,@ts,@ts),
(10007,1,'tenant.user.password.reset','重置用户密码','user','user','password.reset','ENABLED',NULL,@ts,@ts),
(10008,1,'tenant.user.role.assign','分配用户角色','user','user','role.assign','ENABLED',NULL,@ts,@ts),
(10009,1,'tenant.user.delete','软删除用户','user','user','delete','ENABLED',NULL,@ts,@ts),
(10010,1,'tenant.user.restore','恢复用户','user','user','restore','ENABLED',NULL,@ts,@ts),
(10011,1,'tenant.user.read','查询用户','user','user','read','ENABLED',NULL,@ts,@ts),
(10012,1,'tenant.org.create','新增机构','org','org','create','ENABLED',NULL,@ts,@ts),
(10013,1,'tenant.org.update','编辑机构','org','org','update','ENABLED',NULL,@ts,@ts),
(10014,1,'tenant.org.move','调整机构父节点','org','org','move','ENABLED',NULL,@ts,@ts),
(10015,1,'tenant.org.enable','启用机构','org','org','enable','ENABLED',NULL,@ts,@ts),
(10016,1,'tenant.org.disable','禁用机构','org','org','disable','ENABLED',NULL,@ts,@ts),
(10017,1,'tenant.org.delete','软删除机构','org','org','delete','ENABLED',NULL,@ts,@ts),
(10018,1,'tenant.org.restore','恢复机构','org','org','restore','ENABLED',NULL,@ts,@ts),
(10019,1,'tenant.org.user.move','批量移动用户到机构','org','org','user.move','ENABLED',NULL,@ts,@ts),
(10020,1,'tenant.org.read','查询机构','org','org','read','ENABLED',NULL,@ts,@ts),
(10021,1,'tenant.role.create','新增角色','role','role','create','ENABLED',NULL,@ts,@ts),
(10022,1,'tenant.role.update','编辑角色','role','role','update','ENABLED',NULL,@ts,@ts),
(10023,1,'tenant.role.enable','启用角色','role','role','enable','ENABLED',NULL,@ts,@ts),
(10024,1,'tenant.role.disable','禁用角色','role','role','disable','ENABLED',NULL,@ts,@ts),
(10025,1,'tenant.role.delete','软删除角色','role','role','delete','ENABLED',NULL,@ts,@ts),
(10026,1,'tenant.role.read','查询角色','role','role','read','ENABLED',NULL,@ts,@ts),
(10027,1,'tenant.role.permission.assign','配置角色权限','role','role','permission.assign','ENABLED',NULL,@ts,@ts),
(10028,1,'tenant.permission.read','查询权限点列表','permission','permission','read','ENABLED',NULL,@ts,@ts),
(10029,1,'tenant.permission.trace','用户权限追溯','permission','permission','trace','ENABLED',NULL,@ts,@ts),
(10030,1,'tenant.menu.create','新增菜单','menu','menu','create','ENABLED',NULL,@ts,@ts),
(10031,1,'tenant.menu.update','编辑菜单','menu','menu','update','ENABLED',NULL,@ts,@ts),
(10032,1,'tenant.menu.sort','菜单排序','menu','menu','sort','ENABLED',NULL,@ts,@ts),
(10033,1,'tenant.menu.enable','启用菜单','menu','menu','enable','ENABLED',NULL,@ts,@ts),
(10034,1,'tenant.menu.disable','禁用菜单','menu','menu','disable','ENABLED',NULL,@ts,@ts),
(10035,1,'tenant.menu.read','查询菜单树','menu','menu','read','ENABLED',NULL,@ts,@ts),
(10036,1,'tenant.menu.permission.bind','菜单绑定权限','menu','menu','permission.bind','ENABLED',NULL,@ts,@ts),
(10037,1,'tenant.action.create','新增操作点','action','action','create','ENABLED',NULL,@ts,@ts),
(10038,1,'tenant.action.update','编辑操作点','action','action','update','ENABLED',NULL,@ts,@ts),
(10039,1,'tenant.action.delete','删除操作点','action','action','delete','ENABLED',NULL,@ts,@ts),
(10040,1,'tenant.action.permission.bind','操作点绑定权限','action','action','permission.bind','ENABLED',NULL,@ts,@ts),
(10041,1,'tenant.dict.type.create','新增字典类型','dict','dict.type','create','ENABLED',NULL,@ts,@ts),
(10042,1,'tenant.dict.type.update','编辑字典类型','dict','dict.type','update','ENABLED',NULL,@ts,@ts),
(10043,1,'tenant.dict.type.enable','启用字典类型','dict','dict.type','enable','ENABLED',NULL,@ts,@ts),
(10044,1,'tenant.dict.type.disable','禁用字典类型','dict','dict.type','disable','ENABLED',NULL,@ts,@ts),
(10045,1,'tenant.dict.type.read','查询字典类型','dict','dict.type','read','ENABLED',NULL,@ts,@ts),
(10046,1,'tenant.dict.item.create','新增字典项','dict','dict.item','create','ENABLED',NULL,@ts,@ts),
(10047,1,'tenant.dict.item.update','编辑字典项','dict','dict.item','update','ENABLED',NULL,@ts,@ts),
(10048,1,'tenant.dict.item.sort','字典项排序','dict','dict.item','sort','ENABLED',NULL,@ts,@ts),
(10049,1,'tenant.dict.item.enable','启用字典项','dict','dict.item','enable','ENABLED',NULL,@ts,@ts),
(10050,1,'tenant.dict.item.disable','禁用字典项','dict','dict.item','disable','ENABLED',NULL,@ts,@ts),
(10051,1,'tenant.dict.item.delete','删除字典项','dict','dict.item','delete','ENABLED',NULL,@ts,@ts),
(10052,1,'tenant.dict.item.read','查询字典项','dict','dict.item','read','ENABLED',NULL,@ts,@ts),
(10053,1,'tenant.config.read','查询租户配置','config','config','read','ENABLED',NULL,@ts,@ts),
(10054,1,'tenant.config.update','更新租户配置','config','config','update','ENABLED',NULL,@ts,@ts),
(10055,1,'tenant.config.override.read','查询租户可覆盖项','config','config','override.read','ENABLED',NULL,@ts,@ts),
(10056,1,'tenant.config.override.update','更新租户覆盖项','config','config','override.update','ENABLED',NULL,@ts,@ts),
(10057,1,'tenant.config.override.restore','清除租户覆盖','config','config','override.restore','ENABLED',NULL,@ts,@ts),
(10058,1,'tenant.file.upload','上传文件','file','file','upload','ENABLED',NULL,@ts,@ts),
(10059,1,'tenant.file.download','下载文件','file','file','download','ENABLED',NULL,@ts,@ts),
(10060,1,'tenant.file.preview','预览文件','file','file','preview','ENABLED',NULL,@ts,@ts),
(10061,1,'tenant.attachment.bind','绑定附件','attachment','attachment','bind','ENABLED',NULL,@ts,@ts),
(10062,1,'tenant.attachment.unbind','解绑附件','attachment','attachment','unbind','ENABLED',NULL,@ts,@ts),
(10063,1,'tenant.attachment.delete','删除附件','attachment','attachment','delete','ENABLED',NULL,@ts,@ts),
(10064,1,'tenant.attachment.read','查询附件','attachment','attachment','read','ENABLED',NULL,@ts,@ts),
(10065,1,'tenant.audit.read','查询租户审计','audit','audit','read','ENABLED',NULL,@ts,@ts),
(10066,1,'tenant.log.read','查询租户日志','log','log','read','ENABLED',NULL,@ts,@ts),
(10067,1,'tenant.ops_log.read','查看运行日志','ops_log','ops_log','read','ENABLED',NULL,@ts,@ts);

INSERT INTO `sys_role_permission` (`tenant_id`,`role_id`,`permission_code`,`created_at`)
SELECT 1, 1, `code`, @ts FROM `sys_permission` WHERE `tenant_id` = 1 AND `id` BETWEEN 10001 AND 10067;

INSERT INTO `sys_user_role` (`tenant_id`,`user_id`,`role_id`,`created_at`) VALUES
(1, 1, 1, @ts);

INSERT INTO `sys_platform_role` (`id`,`code`,`name`,`status`,`deleted_at`,`created_at`,`updated_at`) VALUES
(1, 'PLATFORM_SUPER', '平台超级角色', 'ENABLED', NULL, @ts, @ts);

-- 平台登录: POST /auth/login JSON 带 loginScope=PLATFORM、username、password；忽略 tenantCode。验证码开关读 tenantId=0 的平台层配置 auth.captcha.enabled。默认账号 platform_admin 初始密码同 @default_password_plain，上线后请改密。
INSERT INTO `sys_platform_account` (`id`,`username`,`display_name`,`phone`,`email`,`password_hash`,`status`,`deleted_at`,`created_at`,`updated_at`) VALUES
(1, 'platform_admin', '平台管理员', NULL, NULL, @default_password_hash, 'ENABLED', NULL, @ts, @ts);

INSERT INTO `sys_platform_account_role` (`account_id`,`role_id`,`created_at`) VALUES
(1, 1, @ts);

INSERT INTO `sys_platform_role_perm` (`role_id`,`permission_code`,`created_at`) VALUES
(1, 'platform.config.read', @ts),
(1, 'platform.config.update', @ts),
(1, 'platform.tenant.create', @ts),
(1, 'platform.tenant.update', @ts),
(1, 'platform.tenant.enable', @ts),
(1, 'platform.tenant.disable', @ts),
(1, 'platform.tenant.renew', @ts),
(1, 'platform.tenant.delete', @ts),
(1, 'platform.tenant.restore', @ts),
(1, 'platform.tenant.read', @ts),
(1, 'platform.tenant.admin.reset_password', @ts),
(1, 'platform.tenant.admin.force_logout', @ts),
(1, 'platform.assist.force_logout_user', @ts),
(1, 'platform.assist.permission_trace', @ts),
(1, 'platform.account.create', @ts),
(1, 'platform.account.update', @ts),
(1, 'platform.account.enable', @ts),
(1, 'platform.account.disable', @ts),
(1, 'platform.account.password.reset', @ts),
(1, 'platform.account.role.assign', @ts),
(1, 'platform.account.read', @ts),
(1, 'platform.role.create', @ts),
(1, 'platform.role.update', @ts),
(1, 'platform.role.enable', @ts),
(1, 'platform.role.disable', @ts),
(1, 'platform.role.delete', @ts),
(1, 'platform.role.permission.assign', @ts),
(1, 'platform.role.read', @ts),
(1, 'platform.permission.read', @ts),
(1, 'platform.announcement.create', @ts),
(1, 'platform.announcement.publish', @ts),
(1, 'platform.announcement.revoke', @ts),
(1, 'platform.announcement.read', @ts),
(1, 'platform.audit.read', @ts),
(1, 'platform.log.read', @ts);

INSERT INTO `sys_tenant_audit` (`id`,`tenant_id`,`event`,`operator_user_id`,`target_id`,`diff_text`,`context_text`,`request_id`,`created_at`) VALUES
(1, 1, 'SEED_INIT', 1, '1', NULL, 'sql_seed', 'seed-001', @ts);

INSERT INTO `sys_tenant_log` (`id`,`tenant_id`,`level`,`module`,`action`,`message`,`request_id`,`trace_id`,`created_at`) VALUES
(1, 1, 'INFO', 'system', 'init', 'database seed applied', 'seed-001', NULL, @ts);

SET @ts = 1704067200001;

INSERT INTO `sys_org` (`id`,`tenant_id`,`parent_org_id`,`code`,`name`,`status`,`sort`,`deleted_at`,`created_at`,`updated_at`) VALUES
(2, 1, 1, 'DEMO_ORG', '演示子机构', 'ENABLED', 10, NULL, @ts, @ts);

INSERT INTO `sys_dict_type` (`id`,`tenant_id`,`code`,`name`,`status`,`deleted_at`,`created_at`,`updated_at`) VALUES
(1, 1, 'DEMO_STATUS', '演示状态', 'ENABLED', NULL, @ts, @ts);

INSERT INTO `sys_dict_item` (`id`,`tenant_id`,`dict_type_id`,`code`,`label`,`item_value`,`sort`,`status`,`deleted_at`,`created_at`,`updated_at`) VALUES
(1, 1, 1, 'ON', '启用', '1', 0, 'ENABLED', NULL, @ts, @ts),
(2, 1, 1, 'OFF', '停用', '0', 1, 'ENABLED', NULL, @ts, @ts);

INSERT INTO `sys_menu` (`id`,`tenant_id`,`parent_id`,`name`,`path`,`icon`,`sort`,`status`,`menu_type`,`hidden`,`deleted_at`,`created_at`,`updated_at`) VALUES
(1, 1, NULL, '系统管理', '/system', 'SettingOutlined', 10, 'ENABLED', 'DIR', 0, NULL, @ts, @ts),
(2, 1, 1, '用户管理', '/users', 'UserOutlined', 10, 'ENABLED', 'PAGE', 0, NULL, @ts, @ts),
(3, 1, 1, '机构管理', '/orgs', 'ApartmentOutlined', 20, 'ENABLED', 'PAGE', 0, NULL, @ts, @ts),
(4, 1, 1, '角色管理', '/roles', 'TeamOutlined', 30, 'ENABLED', 'PAGE', 0, NULL, @ts, @ts),
(5, 1, 1, '菜单管理', '/menus', 'MenuOutlined', 35, 'ENABLED', 'PAGE', 0, NULL, @ts, @ts),
(6, 1, 1, '字典管理', '/dicts', 'BookOutlined', 40, 'ENABLED', 'PAGE', 0, NULL, @ts, @ts),
(7, 1, 1, '配置中心', '/configs', 'ControlOutlined', 45, 'ENABLED', 'PAGE', 0, NULL, @ts, @ts),
(8, 1, 1, '会话管理', '/sessions', 'LockOutlined', 50, 'ENABLED', 'PAGE', 0, NULL, @ts, @ts),
(10, 1, NULL, '消息中心', '/messages', 'BellOutlined', 5, 'ENABLED', 'PAGE', 0, NULL, @ts, @ts),
(11, 1, 1, '审计管理', '/audits', 'AuditOutlined', 55, 'ENABLED', 'PAGE', 0, NULL, @ts, @ts),
(12, 1, 1, '日志管理', '/logs', 'FileSearchOutlined', 60, 'ENABLED', 'PAGE', 0, NULL, @ts, @ts),
(13, 1, 1, '运行日志', '/runtime-logs', 'MonitorOutlined', 57, 'ENABLED', 'PAGE', 0, NULL, @ts, @ts);

INSERT INTO `sys_role_menu` (`tenant_id`,`role_id`,`menu_id`,`created_at`) VALUES
(1, 1, 1, @ts),
(1, 1, 2, @ts),
(1, 1, 3, @ts),
(1, 1, 4, @ts),
(1, 1, 5, @ts),
(1, 1, 6, @ts),
(1, 1, 7, @ts),
(1, 1, 8, @ts),
(1, 1, 10, @ts),
(1, 1, 11, @ts),
(1, 1, 12, @ts),
(1, 1, 13, @ts);

INSERT INTO `sys_config` (`id`,`tenant_id`,`conf_key`,`conf_value`,`value_type`,`version`,`updated_by`,`deleted_at`,`created_at`,`updated_at`) VALUES
(900001, 0, 'auth.captcha.enabled', 'false', 'STRING', 1, NULL, NULL, @ts, @ts),
(900002, 0, 'security.password.minLength', '8', 'STRING', 1, NULL, NULL, @ts, @ts),
(900003, 0, 'ui.theme.default', 'light', 'STRING', 1, NULL, NULL, @ts, @ts),
(900004, 0, 'ui.page.defaultSize', '20', 'STRING', 1, NULL, NULL, @ts, @ts),
(900005, 0, 'auth.session.idleTimeoutSeconds', '1800', 'STRING', 1, NULL, NULL, @ts, @ts),
(900006, 0, 'file.upload.maxSizeMB', '20', 'STRING', 1, NULL, NULL, @ts, @ts),
(900007, 0, 'file.upload.allowedTypes', 'jpg,png,pdf', 'STRING', 1, NULL, NULL, @ts, @ts),
(900008, 0, 'notify.email.enabled', 'false', 'STRING', 1, NULL, NULL, @ts, @ts),
(900009, 0, 'notify.retry.maxAttempts', '3', 'STRING', 1, NULL, NULL, @ts, @ts),
(900010, 0, 'audit.retention.days', '365', 'STRING', 1, NULL, NULL, @ts, @ts),
(900011, 0, 'tenant.log.retention.days', '90', 'STRING', 1, NULL, NULL, @ts, @ts),
(900101, 1, 'auth.captcha.enabled', 'false', 'STRING', 1, NULL, NULL, @ts, @ts),
(900102, 1, 'security.password.minLength', '10', 'STRING', 1, NULL, NULL, @ts, @ts),
(900103, 1, 'ui.theme.default', 'light', 'STRING', 1, NULL, NULL, @ts, @ts),
(900104, 1, 'ui.page.defaultSize', '15', 'STRING', 1, NULL, NULL, @ts, @ts),
(900105, 1, 'file.upload.maxSizeMB', '50', 'STRING', 1, NULL, NULL, @ts, @ts),
(900106, 1, 'file.upload.allowedTypes', 'jpg,png,pdf,docx', 'STRING', 1, NULL, NULL, @ts, @ts),
(900107, 1, 'notify.email.enabled', 'false', 'STRING', 1, NULL, NULL, @ts, @ts),
(900108, 1, 'notify.retry.maxAttempts', '3', 'STRING', 1, NULL, NULL, @ts, @ts),
(900109, 1, 'auth.session.idleTimeoutSeconds', '1800', 'STRING', 1, NULL, NULL, @ts, @ts);
