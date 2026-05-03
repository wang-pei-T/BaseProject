/*!40101 SET NAMES utf8mb4 */;
-- 库表 DDL（含索引）。执行: cd sql && mysql -h127.0.0.1 -uroot -p < init_schema.sql
-- 完成后执行 init_data.sql 导入数据。

CREATE DATABASE IF NOT EXISTS `baseproject` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `baseproject`;

DROP TABLE IF EXISTS `sys_platform_account_role`;
DROP TABLE IF EXISTS `sys_platform_role_perm`;
DROP TABLE IF EXISTS `sys_user_role`;
DROP TABLE IF EXISTS `sys_role_menu`;
DROP TABLE IF EXISTS `sys_role_permission`;
DROP TABLE IF EXISTS `sys_attachment`;
DROP TABLE IF EXISTS `sys_message`;
DROP TABLE IF EXISTS `sys_tenant_audit`;
DROP TABLE IF EXISTS `sys_tenant_log`;
DROP TABLE IF EXISTS `sys_user_notification_pref`;
DROP TABLE IF EXISTS `sys_dict_item`;
DROP TABLE IF EXISTS `sys_dict_type`;
DROP TABLE IF EXISTS `sys_config`;
DROP TABLE IF EXISTS `sys_file_meta`;
DROP TABLE IF EXISTS `sys_action`;
DROP TABLE IF EXISTS `sys_menu`;
DROP TABLE IF EXISTS `sys_permission`;
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `sys_org`;
DROP TABLE IF EXISTS `sys_platform_audit`;
DROP TABLE IF EXISTS `sys_platform_announcement`;
DROP TABLE IF EXISTS `sys_platform_account`;
DROP TABLE IF EXISTS `sys_platform_role`;
DROP TABLE IF EXISTS `sys_tenant`;

CREATE TABLE `sys_tenant` (
  `id` BIGINT NOT NULL PRIMARY KEY COMMENT '租户主键',
  `tenant_code` VARCHAR(64) NOT NULL COMMENT '租户编码',
  `tenant_name` VARCHAR(128) NOT NULL COMMENT '租户名称',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  `expire_at` VARCHAR(32) DEFAULT NULL COMMENT '到期时间(ISO或业务约定)',
  `admin_user_id` BIGINT DEFAULT NULL COMMENT '租户管理员用户ID',
  `deleted_at` BIGINT DEFAULT NULL COMMENT '软删毫秒时间戳',
  `created_at` BIGINT NOT NULL,
  `updated_at` BIGINT NOT NULL,
  `created_by` BIGINT DEFAULT NULL,
  `updated_by` BIGINT DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台租户注册表';

CREATE TABLE `sys_org` (
  `id` BIGINT NOT NULL PRIMARY KEY,
  `tenant_id` BIGINT NOT NULL,
  `parent_org_id` BIGINT DEFAULT NULL,
  `code` VARCHAR(64) DEFAULT NULL,
  `name` VARCHAR(128) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  `sort` INT NOT NULL DEFAULT 0,
  `deleted_at` BIGINT DEFAULT NULL,
  `created_at` BIGINT NOT NULL,
  `updated_at` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户机构';

CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL PRIMARY KEY,
  `tenant_id` BIGINT NOT NULL,
  `org_id` BIGINT DEFAULT NULL,
  `username` VARCHAR(64) NOT NULL,
  `display_name` VARCHAR(128) DEFAULT NULL,
  `phone` VARCHAR(32) DEFAULT NULL,
  `email` VARCHAR(128) DEFAULT NULL,
  `password_hash` VARCHAR(255) DEFAULT NULL COMMENT 'BCrypt等，接库后写入',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  `lock_status` VARCHAR(32) NOT NULL DEFAULT 'UNLOCKED',
  `deleted_at` BIGINT DEFAULT NULL,
  `created_at` BIGINT NOT NULL,
  `updated_at` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户用户';

CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL PRIMARY KEY,
  `tenant_id` BIGINT NOT NULL,
  `code` VARCHAR(64) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  `deleted_at` BIGINT DEFAULT NULL,
  `created_at` BIGINT NOT NULL,
  `updated_at` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户角色';

CREATE TABLE `sys_permission` (
  `id` BIGINT NOT NULL PRIMARY KEY,
  `tenant_id` BIGINT NOT NULL COMMENT '0表示全局模板权限定义时可不用，业务租户非0',
  `code` VARCHAR(128) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `module` VARCHAR(64) DEFAULT NULL,
  `resource_type` VARCHAR(64) DEFAULT NULL,
  `action_type` VARCHAR(64) DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  `deleted_at` BIGINT DEFAULT NULL,
  `created_at` BIGINT NOT NULL,
  `updated_at` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限点';

CREATE TABLE `sys_role_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `tenant_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  `permission_code` VARCHAR(128) NOT NULL,
  `created_at` BIGINT NOT NULL,
  UNIQUE KEY `uk_role_perm` (`tenant_id`,`role_id`,`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联';

CREATE TABLE `sys_user_role` (
  `tenant_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  `created_at` BIGINT NOT NULL,
  PRIMARY KEY (`tenant_id`,`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色';

CREATE TABLE `sys_role_menu` (
  `tenant_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  `menu_id` BIGINT NOT NULL,
  `created_at` BIGINT NOT NULL,
  PRIMARY KEY (`tenant_id`,`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联';

CREATE TABLE `sys_menu` (
  `id` BIGINT NOT NULL PRIMARY KEY,
  `tenant_id` BIGINT NOT NULL,
  `parent_id` BIGINT DEFAULT NULL,
  `name` VARCHAR(128) NOT NULL,
  `path` VARCHAR(256) DEFAULT NULL,
  `icon` VARCHAR(64) DEFAULT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  `menu_type` VARCHAR(32) NOT NULL DEFAULT 'PAGE',
  `hidden` TINYINT(1) NOT NULL DEFAULT 0,
  `deleted_at` BIGINT DEFAULT NULL,
  `created_at` BIGINT NOT NULL,
  `updated_at` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户菜单';

CREATE TABLE `sys_action` (
  `id` BIGINT NOT NULL PRIMARY KEY,
  `tenant_id` BIGINT NOT NULL,
  `menu_id` BIGINT DEFAULT NULL,
  `code` VARCHAR(128) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  `deleted_at` BIGINT DEFAULT NULL,
  `created_at` BIGINT NOT NULL,
  `updated_at` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='按钮动作';

CREATE TABLE `sys_dict_type` (
  `id` BIGINT NOT NULL PRIMARY KEY,
  `tenant_id` BIGINT NOT NULL,
  `code` VARCHAR(64) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  `deleted_at` BIGINT DEFAULT NULL,
  `created_at` BIGINT NOT NULL,
  `updated_at` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典类型';

CREATE TABLE `sys_dict_item` (
  `id` BIGINT NOT NULL PRIMARY KEY,
  `tenant_id` BIGINT NOT NULL,
  `dict_type_id` BIGINT NOT NULL,
  `code` VARCHAR(64) NOT NULL,
  `label` VARCHAR(128) NOT NULL,
  `item_value` VARCHAR(512) NOT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  `deleted_at` BIGINT DEFAULT NULL,
  `created_at` BIGINT NOT NULL,
  `updated_at` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典项';

CREATE TABLE `sys_config` (
  `id` BIGINT NOT NULL PRIMARY KEY,
  `tenant_id` BIGINT NOT NULL,
  `conf_key` VARCHAR(128) NOT NULL,
  `conf_value` TEXT,
  `value_type` VARCHAR(32) DEFAULT 'STRING',
  `version` BIGINT NOT NULL DEFAULT 1,
  `updated_by` BIGINT DEFAULT NULL,
  `deleted_at` BIGINT DEFAULT NULL,
  `created_at` BIGINT NOT NULL,
  `updated_at` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户配置项';

CREATE TABLE `sys_file_meta` (
  `id` BIGINT NOT NULL PRIMARY KEY,
  `tenant_id` BIGINT NOT NULL,
  `bucket_name` VARCHAR(128) NOT NULL,
  `object_key` VARCHAR(512) NOT NULL,
  `original_filename` VARCHAR(256) NOT NULL,
  `content_type` VARCHAR(128) DEFAULT NULL,
  `size_bytes` BIGINT NOT NULL,
  `sha256_hex` VARCHAR(64) DEFAULT NULL,
  `biz_hint` VARCHAR(128) DEFAULT NULL,
  `created_by` BIGINT NOT NULL,
  `deleted_at` BIGINT DEFAULT NULL,
  `created_at` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件元数据';

CREATE TABLE `sys_attachment` (
  `id` BIGINT NOT NULL PRIMARY KEY,
  `tenant_id` BIGINT NOT NULL,
  `file_id` BIGINT NOT NULL,
  `biz_type` VARCHAR(64) NOT NULL,
  `biz_id` VARCHAR(64) NOT NULL,
  `tag` VARCHAR(64) DEFAULT NULL,
  `file_name` VARCHAR(256) NOT NULL,
  `size_bytes` BIGINT NOT NULL,
  `content_type` VARCHAR(128) DEFAULT NULL,
  `created_by` BIGINT NOT NULL,
  `deleted_at` BIGINT DEFAULT NULL,
  `created_at` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务附件';

CREATE TABLE `sys_message` (
  `id` BIGINT NOT NULL PRIMARY KEY,
  `tenant_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `type` VARCHAR(32) NOT NULL,
  `title` VARCHAR(256) NOT NULL,
  `content` TEXT,
  `link_url` VARCHAR(512) DEFAULT NULL,
  `payload_json` JSON DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'UNREAD',
  `read_at` BIGINT DEFAULT NULL,
  `deleted_at` BIGINT DEFAULT NULL,
  `created_at` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内消息';

CREATE TABLE `sys_tenant_audit` (
  `id` BIGINT NOT NULL PRIMARY KEY,
  `tenant_id` BIGINT NOT NULL,
  `event` VARCHAR(128) NOT NULL,
  `operator_user_id` BIGINT NOT NULL,
  `target_id` VARCHAR(64) DEFAULT NULL,
  `diff_text` TEXT,
  `context_text` VARCHAR(512) DEFAULT NULL,
  `request_id` VARCHAR(64) DEFAULT NULL,
  `operator_ip` VARCHAR(64) DEFAULT NULL,
  `user_agent` VARCHAR(512) DEFAULT NULL,
  `created_at` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户审计';

CREATE TABLE `sys_tenant_log` (
  `id` BIGINT NOT NULL PRIMARY KEY,
  `tenant_id` BIGINT NOT NULL,
  `level` VARCHAR(16) NOT NULL,
  `module` VARCHAR(64) NOT NULL,
  `action` VARCHAR(64) NOT NULL,
  `message` VARCHAR(1024) NOT NULL,
  `request_id` VARCHAR(64) DEFAULT NULL,
  `trace_id` VARCHAR(64) DEFAULT NULL,
  `created_at` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户日志';

CREATE TABLE `sys_user_notification_pref` (
  `tenant_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `preferences` JSON NOT NULL,
  `updated_at` BIGINT NOT NULL,
  PRIMARY KEY (`tenant_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知偏好';

CREATE TABLE `sys_platform_role` (
  `id` BIGINT NOT NULL PRIMARY KEY,
  `code` VARCHAR(64) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  `deleted_at` BIGINT DEFAULT NULL,
  `created_at` BIGINT NOT NULL,
  `updated_at` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台角色';

CREATE TABLE `sys_platform_account` (
  `id` BIGINT NOT NULL PRIMARY KEY,
  `username` VARCHAR(64) NOT NULL,
  `display_name` VARCHAR(128) NOT NULL,
  `phone` VARCHAR(32) DEFAULT NULL,
  `email` VARCHAR(128) DEFAULT NULL,
  `password_hash` VARCHAR(255) DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  `deleted_at` BIGINT DEFAULT NULL,
  `created_at` BIGINT NOT NULL,
  `updated_at` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台账号';

CREATE TABLE `sys_platform_account_role` (
  `account_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  `created_at` BIGINT NOT NULL,
  PRIMARY KEY (`account_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台账号角色';

CREATE TABLE `sys_platform_role_perm` (
  `role_id` BIGINT NOT NULL,
  `permission_code` VARCHAR(128) NOT NULL,
  `created_at` BIGINT NOT NULL,
  PRIMARY KEY (`role_id`,`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台角色权限';

CREATE TABLE `sys_platform_audit` (
  `id` BIGINT NOT NULL PRIMARY KEY,
  `event` VARCHAR(128) NOT NULL,
  `operator_account_id` BIGINT DEFAULT NULL,
  `target_tenant_id` BIGINT DEFAULT NULL,
  `request_id` VARCHAR(64) DEFAULT NULL,
  `extra_json` JSON DEFAULT NULL,
  `created_at` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台审计';

CREATE TABLE `sys_platform_announcement` (
  `id` BIGINT NOT NULL PRIMARY KEY,
  `title` VARCHAR(256) NOT NULL,
  `content` TEXT NOT NULL,
  `target_type` VARCHAR(32) NOT NULL,
  `tenant_ids_json` JSON DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  `created_at` BIGINT NOT NULL,
  `updated_at` BIGINT NOT NULL,
  `published_at` BIGINT DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台公告';

ALTER TABLE `sys_tenant` ADD UNIQUE KEY `uk_sys_tenant_code` (`tenant_code`);

ALTER TABLE `sys_org` ADD KEY `idx_sys_org_tenant` (`tenant_id`,`deleted_at`);
ALTER TABLE `sys_org` ADD UNIQUE KEY `uk_sys_org_tenant_code` (`tenant_id`,`code`);

ALTER TABLE `sys_user` ADD KEY `idx_sys_user_tenant` (`tenant_id`,`deleted_at`);
ALTER TABLE `sys_user` ADD KEY `idx_sys_user_org` (`tenant_id`,`org_id`);
ALTER TABLE `sys_user` ADD UNIQUE KEY `uk_sys_user_tenant_username` (`tenant_id`,`username`);

ALTER TABLE `sys_role` ADD KEY `idx_sys_role_tenant` (`tenant_id`,`deleted_at`);
ALTER TABLE `sys_role` ADD UNIQUE KEY `uk_sys_role_tenant_code` (`tenant_id`,`code`);

ALTER TABLE `sys_permission` ADD KEY `idx_sys_perm_tenant` (`tenant_id`,`deleted_at`);
ALTER TABLE `sys_permission` ADD UNIQUE KEY `uk_sys_perm_tenant_code` (`tenant_id`,`code`);

ALTER TABLE `sys_role_permission` ADD KEY `idx_sys_rp_role` (`tenant_id`,`role_id`);

ALTER TABLE `sys_user_role` ADD KEY `idx_sys_ur_user` (`tenant_id`,`user_id`);

ALTER TABLE `sys_role_menu` ADD KEY `idx_sys_rm_role` (`tenant_id`,`role_id`);
ALTER TABLE `sys_role_menu` ADD KEY `idx_sys_rm_menu` (`tenant_id`,`menu_id`);

ALTER TABLE `sys_menu` ADD KEY `idx_sys_menu_tenant` (`tenant_id`,`parent_id`,`deleted_at`);

ALTER TABLE `sys_action` ADD KEY `idx_sys_action_tenant_menu` (`tenant_id`,`menu_id`);

ALTER TABLE `sys_dict_type` ADD UNIQUE KEY `uk_sys_dict_type` (`tenant_id`,`code`);
ALTER TABLE `sys_dict_type` ADD KEY `idx_sys_dict_type_tenant` (`tenant_id`,`deleted_at`);

ALTER TABLE `sys_dict_item` ADD KEY `idx_sys_dict_item_type` (`tenant_id`,`dict_type_id`);

ALTER TABLE `sys_config` ADD UNIQUE KEY `uk_sys_config_key` (`tenant_id`,`conf_key`);
ALTER TABLE `sys_config` ADD KEY `idx_sys_config_tenant` (`tenant_id`,`deleted_at`);

ALTER TABLE `sys_file_meta` ADD KEY `idx_sys_file_tenant` (`tenant_id`,`created_at`);
ALTER TABLE `sys_file_meta` ADD KEY `idx_sys_file_sha` (`sha256_hex`);

ALTER TABLE `sys_attachment` ADD KEY `idx_sys_att_biz` (`tenant_id`,`biz_type`,`biz_id`);
ALTER TABLE `sys_attachment` ADD KEY `idx_sys_att_file` (`file_id`);

ALTER TABLE `sys_message` ADD KEY `idx_sys_msg_user` (`tenant_id`,`user_id`,`status`,`created_at`);

ALTER TABLE `sys_tenant_audit` ADD KEY `idx_sys_taudit_tenant_time` (`tenant_id`,`created_at`);
ALTER TABLE `sys_tenant_audit` ADD KEY `idx_sys_taudit_event` (`tenant_id`,`event`);

ALTER TABLE `sys_tenant_log` ADD KEY `idx_sys_tlog_tenant_time` (`tenant_id`,`created_at`);
ALTER TABLE `sys_tenant_log` ADD KEY `idx_sys_tlog_level` (`tenant_id`,`level`);

ALTER TABLE `sys_platform_account` ADD UNIQUE KEY `uk_sys_plat_acc_user` (`username`);
ALTER TABLE `sys_platform_role` ADD UNIQUE KEY `uk_sys_plat_role_code` (`code`);

ALTER TABLE `sys_platform_audit` ADD KEY `idx_sys_paudit_time` (`created_at`);
ALTER TABLE `sys_platform_audit` ADD KEY `idx_sys_paudit_event` (`event`);

ALTER TABLE `sys_platform_announcement` ADD KEY `idx_sys_pann_status` (`status`,`created_at`);
