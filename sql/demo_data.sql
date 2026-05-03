    -- =============================================================================
    -- 演示数据脚本（demo_data.sql）
    -- =============================================================================
    -- 用途：
    --   在 init_schema.sql + init_data.sql 执行完毕之后，再执行本文件，
    --   向「默认租户」(tenant_id = 1) 追加机构树、用户、演示角色与字典演示数据等；
    --   租户菜单由 init_data.sql 提供（菜单 id 1～8、10～12，无 id=9），本文件仅通过 sys_role_menu 引用。
    --
    -- 执行方式（示例）：
    --   mysql -h127.0.0.1 -uroot -p baseproject < sql/demo_data.sql
    --
    -- 与 init_data.sql 的隔离约定（避免主键 / 唯一键冲突）：
    --   1. 不使用 sys_user.id = 1、username = 'admin'（系统管理员仅由 init_data 创建）。
    --   2. 不插入 / 不修改 sys_tenant、sys_role(id≤2)、sys_permission、sys_role_permission、
    --      sys_platform_*、sys_config 等 init 已占用的种子数据；不修改 init 已有 sys_dict_type id=1。
    --   3. 本文件追加 sys_org（约 100～199）、sys_user（约 20001～）、sys_role（21001～）、
    --      sys_user_role、sys_role_menu、sys_dict_type（约 50～）、sys_dict_item（约 500～）；不插入 sys_menu。
    --   4. 机构 parent_org_id 允许指向 init 的根机构 id=1，使演示树挂在「默认机构」下，
    --      与 id=2 的「演示子机构」并列，互不覆盖。
    --
    -- 登录说明：
    --   下列演示用户密码与 init_data 中一致（BCrypt 哈希对应明文 Admin@123456），
    --   实际环境请导入后尽快修改或通过「重置密码」功能更新。
    -- =============================================================================

    USE `baseproject`;

    /* 统一时间戳：与 init 区分即可 */
    SET @demo_ts = 1710000000000;

    /* 与 init_data.sql 相同的演示密码哈希（明文：Admin@123456） */
    SET @demo_password_hash = '$2a$10$rRMC6.NuB2a5VWDHHP3AquE7wufr0Lneb54CqKJCqsgfOrp6uW8NW';

    -- -----------------------------------------------------------------------------
    -- 一、机构（sys_org）— 模拟「华东创新科技有限公司」常规组织架构
    -- -----------------------------------------------------------------------------
    -- 设计说明：
    --   * 根节点：挂在租户已有根机构 id=1 之下，形成「公司总部」实体，便于在
    --     前端树中与「默认机构」「演示子机构(init id=2)」并列展示。
    --   * 一级部门：总经办、研发中心、市场营销中心、人力资源部、财务部、法务与合规部。
    --   * 二级部门：研发下设产品一部/二部、质量与测试、运维与基础设施；营销下设
    --     品牌与数字化、渠道与区域销售、战略客户与大客户部。
    --   * 额外两条用于界面联调：一条 status=DISABLED（已停用未删），一条 deleted_at
    --     非空（软删机构，用于「含已删除」筛选与恢复流程演示）。
    -- 注意：INSERT 顺序为先父后子，以满足人工阅读与部分外键习惯（本表 parent 为
    --       逻辑引用，无数据库级 FK）。
    -- -----------------------------------------------------------------------------

    INSERT INTO `sys_org` (`id`,`tenant_id`,`parent_org_id`,`code`,`name`,`status`,`sort`,`deleted_at`,`created_at`,`updated_at`) VALUES
    /* 公司主体（一级虚拟总部） */
    (100, 1, 0, 'DEMO_HZCX', '华东创新科技有限公司', 'ENABLED', 10, NULL, @demo_ts, @demo_ts),

    /* 一级中心 / 部门 */
    (101, 1, 100, 'DEMO_BOARD', '总经办', 'ENABLED', 20, NULL, @demo_ts, @demo_ts),
    (102, 1, 100, 'DEMO_RD', '研发中心', 'ENABLED', 30, NULL, @demo_ts, @demo_ts),
    (103, 1, 100, 'DEMO_MKT', '市场营销中心', 'ENABLED', 40, NULL, @demo_ts, @demo_ts),
    (104, 1, 100, 'DEMO_HR', '人力资源部', 'ENABLED', 50, NULL, @demo_ts, @demo_ts),
    (105, 1, 100, 'DEMO_FIN', '财务部', 'ENABLED', 60, NULL, @demo_ts, @demo_ts),
    (106, 1, 100, 'DEMO_LEGAL', '法务与合规部', 'ENABLED', 70, NULL, @demo_ts, @demo_ts),

    /* 研发中心下属 */
    (107, 1, 102, 'DEMO_RD_P1', '产品研发一部', 'ENABLED', 10, NULL, @demo_ts, @demo_ts),
    (108, 1, 102, 'DEMO_RD_P2', '产品研发二部', 'ENABLED', 20, NULL, @demo_ts, @demo_ts),
    (109, 1, 102, 'DEMO_RD_QA', '质量与测试部', 'ENABLED', 30, NULL, @demo_ts, @demo_ts),
    (110, 1, 102, 'DEMO_RD_OPS', '运维与基础设施组', 'ENABLED', 40, NULL, @demo_ts, @demo_ts),

    /* 市场营销中心下属 */
    (111, 1, 103, 'DEMO_MKT_BRAND', '品牌与数字化营销部', 'ENABLED', 10, NULL, @demo_ts, @demo_ts),
    (112, 1, 103, 'DEMO_MKT_CH', '渠道与区域销售部', 'ENABLED', 20, NULL, @demo_ts, @demo_ts),
    (113, 1, 103, 'DEMO_MKT_KA', '战略客户与大客户部', 'ENABLED', 30, NULL, @demo_ts, @demo_ts),

    /* 停用未删：用于启用/禁用切换演示 */
    (114, 1, 100, 'DEMO_BRANCH_OFF', '华东创新·驻沪联络处（停用）', 'DISABLED', 900, NULL, @demo_ts, @demo_ts),

    /* 软删：用于「含已删除」与恢复演示；code 仍唯一 */
    (115, 1, 100, 'DEMO_PRJ_LEGACY', '历史项目部（已撤销）', 'ENABLED', 910, @demo_ts, @demo_ts, @demo_ts);

    -- -----------------------------------------------------------------------------
    -- 二、用户（sys_user）— 拟真姓名 / 手机 / 邮箱 / 分布到各部门
    -- -----------------------------------------------------------------------------
    -- 设计说明：
    --   * username 采用「拼音加点」风格，与 admin 及 init 用户绝不重复。
    --   * display_name 为常见中文姓名；手机号为 138/139 号段虚构号；邮箱使用
    --     子域 demo-corp.local（保留域名，无真实投递）。
    --   * 覆盖状态：多数 ENABLED + UNLOCKED；个别 DISABLED、LOCKED；一名软删用户，
    --     便于列表筛选、启停、锁定、恢复等用例。
    --   * org_id 指向上述演示机构；两名用户挂在「已停用机构」114 上，用于目标机构
    --     DISABLED 策略联调（若业务禁止迁入，可在迁移接口中体现）。
    --   * 故意保留 1～2 人 org_id 为 NULL（仅租户归属、未分配部门场景），视产品是否
    --     允许；若后端强制 org 必填，可改为挂到 101 总经办。
    -- -----------------------------------------------------------------------------

    INSERT INTO `sys_user` (`id`,`tenant_id`,`org_id`,`username`,`display_name`,`phone`,`email`,`password_hash`,`status`,`lock_status`,`deleted_at`,`created_at`,`updated_at`) VALUES
    (20001, 1, 101, 'chen.yifan', '陈一凡', '13816710001', 'chen.yifan@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20002, 1, 101, 'lin.jiaqi', '林佳琪', '13816710002', 'lin.jiaqi@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20003, 1, 104, 'zhou.min', '周敏', '13901880003', 'zhou.min@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20004, 1, 104, 'xu.hao', '徐浩', '13901880004', 'xu.hao@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20005, 1, 105, 'jiang.li', '蒋丽', '13801920005', 'jiang.li@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20006, 1, 105, 'dong.lei', '董磊', '13801920006', 'dong.lei@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20007, 1, 106, 'pan.jing', '潘婧', '13916550007', 'pan.jing@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20008, 1, 102, 'wei.zong', '魏总工', '13817770008', 'wei.zong@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20009, 1, 107, 'luo.yang', '罗阳', '13817770009', 'luo.yang@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20010, 1, 107, 'fang.qian', '方倩', '13817770010', 'fang.qian@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20011, 1, 108, 'shi.jun', '石峻', '13921110011', 'shi.jun@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20012, 1, 108, 'tan.yue', '谭玥', '13921110012', 'tan.yue@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20013, 1, 109, 'ceng.kai', '曾凯', '13801660013', 'ceng.kai@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20014, 1, 110, 'qian.liang', '钱亮', '13801660014', 'qian.liang@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20015, 1, 111, 'feng.nan', '冯楠', '13917550015', 'feng.nan@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20016, 1, 112, 'deng.tao', '邓涛', '13917550016', 'deng.tao@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20017, 1, 113, 'cao.ying', '曹颖', '13818880017', 'cao.ying@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20018, 1, 113, 'yuan.cheng', '袁程', '13818880018', 'yuan.cheng@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20019, 1, 114, 'ren.qi', '任琦', '13818880019', 'ren.qi@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20020, 1, 114, 'shao.bin', '邵彬', '13818880020', 'shao.bin@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20021, 1, 112, 'meng.xin', '孟欣', '13901990021', 'meng.xin@demo-corp.local', @demo_password_hash, 'DISABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20022, 1, 110, 'long.fei', '龙飞', '13901990022', 'long.fei@demo-corp.local', @demo_password_hash, 'ENABLED', 'LOCKED', NULL, @demo_ts, @demo_ts),
    (20023, 1, NULL, 'bai.rui', '白蕊', '13901990023', 'bai.rui@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20024, 1, NULL, 'hao.yu', '郝宇', '13901990024', 'hao.yu@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', NULL, @demo_ts, @demo_ts),
    (20025, 1, 109, 'kang.lei', '康蕾', '13822330025', 'kang.lei@demo-corp.local', @demo_password_hash, 'ENABLED', 'UNLOCKED', @demo_ts, @demo_ts, @demo_ts);

    -- =============================================================================
    -- 三、角色管理演示数据（ROLE-001~008 联调）
    -- =============================================================================
    -- 说明：
    --   1) 不使用 init_data 里的 roleId=1（TENANT_ADMIN），避免和内置管理员叙事冲突。
    --   2) role id 使用 21001+ 段；user_role 仅绑定 demo 用户 20001+，不包含 admin(1)。
    --   3) 包含：启用角色、禁用角色、软删角色，便于演示筛选/禁用分配/恢复等流程。

    SET @demo_role_ts = 1710000005000;

    INSERT INTO `sys_role` (`id`,`tenant_id`,`code`,`name`,`status`,`deleted_at`,`created_at`,`updated_at`) VALUES
    (21001, 1, 'DEMO_HR_ADMIN', '人事管理员', 'ENABLED', NULL, @demo_role_ts, @demo_role_ts),
    (21002, 1, 'DEMO_SALES_MANAGER', '销售主管', 'ENABLED', NULL, @demo_role_ts, @demo_role_ts),
    (21003, 1, 'DEMO_RD_OBSERVER', '研发观察员', 'DISABLED', NULL, @demo_role_ts, @demo_role_ts),
    (21004, 1, 'DEMO_FIN_ARCHIVE', '财务归档角色', 'ENABLED', @demo_role_ts, @demo_role_ts, @demo_role_ts);

    INSERT INTO `sys_role_permission` (`tenant_id`,`role_id`,`permission_code`,`created_at`) VALUES
    (1, 21001, 'tenant.user.read', @demo_role_ts),
    (1, 21001, 'tenant.user.update', @demo_role_ts),
    (1, 21001, 'tenant.user.role.assign', @demo_role_ts),
    (1, 21001, 'tenant.org.read', @demo_role_ts),
    (1, 21002, 'tenant.user.read', @demo_role_ts),
    (1, 21002, 'tenant.org.read', @demo_role_ts),
    (1, 21002, 'tenant.role.read', @demo_role_ts),
    (1, 21003, 'tenant.user.read', @demo_role_ts),
    (1, 21004, 'tenant.audit.read', @demo_role_ts);

    INSERT INTO `sys_user_role` (`tenant_id`,`user_id`,`role_id`,`created_at`) VALUES
    (1, 20001, 21001, @demo_role_ts),
    (1, 20002, 21002, @demo_role_ts),
    (1, 20005, 21002, @demo_role_ts),
    (1, 20008, 21003, @demo_role_ts);

    -- 角色菜单（sys_role_menu）：引用 init_data 菜单 id 1～8、10～12
    INSERT INTO `sys_role_menu` (`tenant_id`,`role_id`,`menu_id`,`created_at`) VALUES
    (1, 21001, 1, @demo_role_ts),
    (1, 21001, 2, @demo_role_ts),
    (1, 21001, 3, @demo_role_ts),
    (1, 21001, 4, @demo_role_ts),
    (1, 21001, 5, @demo_role_ts),
    (1, 21001, 8, @demo_role_ts),
    (1, 21002, 2, @demo_role_ts),
    (1, 21002, 3, @demo_role_ts),
    (1, 21003, 2, @demo_role_ts),
    (1, 21003, 5, @demo_role_ts);

    -- =============================================================================
    -- 四、字典管理演示数据（字典类型 + 字典项）
    -- =============================================================================
    -- 说明：与 init 的 DEMO_STATUS(id=1) 不冲突；含启用/停用类型、软删类型、停用项、软删项，便于列表/筛选/恢复联调。

    SET @demo_dict_ts = 1710000004000;

    INSERT INTO `sys_dict_type` (`id`,`tenant_id`,`code`,`name`,`status`,`deleted_at`,`created_at`,`updated_at`) VALUES
    (50, 1, 'DEMO_EDUCATION', '学历', 'ENABLED', NULL, @demo_dict_ts, @demo_dict_ts),
    (51, 1, 'DEMO_PRIORITY', '工单优先级', 'ENABLED', NULL, @demo_dict_ts, @demo_dict_ts),
    (52, 1, 'DEMO_CONTRACT_STAGE', '合同阶段', 'ENABLED', NULL, @demo_dict_ts, @demo_dict_ts),
    (53, 1, 'DEMO_LEGACY_CHANNEL', '已停用-获客渠道', 'DISABLED', NULL, @demo_dict_ts, @demo_dict_ts),
    (54, 1, 'DEMO_REMOVED_SETTLE', '已删除-结算方式（类型软删演示）', 'ENABLED', @demo_dict_ts, @demo_dict_ts, @demo_dict_ts);

    INSERT INTO `sys_dict_item` (`id`,`tenant_id`,`dict_type_id`,`code`,`label`,`item_value`,`sort`,`status`,`deleted_at`,`created_at`,`updated_at`) VALUES
    (500, 1, 50, 'SEC', '中等教育及以下', 'SEC', 5, 'DISABLED', NULL, @demo_dict_ts, @demo_dict_ts),
    (501, 1, 50, 'COLLEGE', '大专', 'COLLEGE', 10, 'ENABLED', NULL, @demo_dict_ts, @demo_dict_ts),
    (502, 1, 50, 'UNDERGRAD', '本科', 'UNDERGRAD', 20, 'ENABLED', NULL, @demo_dict_ts, @demo_dict_ts),
    (503, 1, 50, 'MASTER', '硕士研究生', 'MASTER', 30, 'ENABLED', NULL, @demo_dict_ts, @demo_dict_ts),
    (504, 1, 50, 'PHD', '博士研究生', 'PHD', 40, 'ENABLED', NULL, @demo_dict_ts, @demo_dict_ts),
    (510, 1, 51, 'P0', '紧急', 'P0', 10, 'ENABLED', NULL, @demo_dict_ts, @demo_dict_ts),
    (511, 1, 51, 'P1', '高', 'P1', 20, 'ENABLED', NULL, @demo_dict_ts, @demo_dict_ts),
    (512, 1, 51, 'P2', '中', 'P2', 30, 'ENABLED', NULL, @demo_dict_ts, @demo_dict_ts),
    (513, 1, 51, 'P3', '低', 'P3', 40, 'ENABLED', NULL, @demo_dict_ts, @demo_dict_ts),
    (520, 1, 52, 'DRAFT', '草稿', 'DRAFT', 10, 'ENABLED', NULL, @demo_dict_ts, @demo_dict_ts),
    (521, 1, 52, 'SIGN', '签署中', 'SIGN', 20, 'ENABLED', NULL, @demo_dict_ts, @demo_dict_ts),
    (522, 1, 52, 'EXEC', '履约中', 'EXEC', 30, 'ENABLED', NULL, @demo_dict_ts, @demo_dict_ts),
    (523, 1, 52, 'CLOSE', '已完结', 'CLOSE', 40, 'ENABLED', NULL, @demo_dict_ts, @demo_dict_ts),
    (524, 1, 52, 'ABORT', '已废止', 'ABORT', 50, 'ENABLED', @demo_dict_ts, @demo_dict_ts, @demo_dict_ts),
    (530, 1, 53, 'ONLINE', '线上活动', 'ONLINE', 10, 'ENABLED', NULL, @demo_dict_ts, @demo_dict_ts),
    (531, 1, 53, 'OFFLINE', '线下展会', 'OFFLINE', 20, 'ENABLED', NULL, @demo_dict_ts, @demo_dict_ts);

    -- =============================================================================
    -- 五、租户配置中心演示数据（tenant_id=1）
    -- =============================================================================
    SET @demo_cfg_ts = 1710000005000;

    INSERT INTO `sys_config` (`id`,`tenant_id`,`conf_key`,`conf_value`,`value_type`,`version`,`updated_by`,`deleted_at`,`created_at`,`updated_at`) VALUES
    (910001, 2, 'auth.captcha.enabled', 'false', 'STRING', 2, 10001, NULL, @demo_cfg_ts, @demo_cfg_ts),
    (910002, 2, 'security.password.minLength', '12', 'STRING', 2, 10001, NULL, @demo_cfg_ts, @demo_cfg_ts),
    (910003, 2, 'ui.theme.default', 'dark', 'STRING', 2, 10001, NULL, @demo_cfg_ts, @demo_cfg_ts),
    (910004, 2, 'ui.page.defaultSize', '25', 'STRING', 2, 10001, NULL, @demo_cfg_ts, @demo_cfg_ts),
    (910005, 2, 'file.upload.maxSizeMB', '100', 'STRING', 2, 10001, NULL, @demo_cfg_ts, @demo_cfg_ts),
    (910006, 2, 'file.upload.allowedTypes', 'jpg,png,pdf,xlsx', 'STRING', 2, 10001, NULL, @demo_cfg_ts, @demo_cfg_ts),
    (910007, 2, 'notify.email.enabled', 'false', 'STRING', 2, 10001, NULL, @demo_cfg_ts, @demo_cfg_ts),
    (910008, 2, 'notify.retry.maxAttempts', '5', 'STRING', 2, 10001, NULL, @demo_cfg_ts, @demo_cfg_ts),
    (910009, 2, 'auth.session.idleTimeoutSeconds', '3600', 'STRING', 2, 10001, NULL, @demo_cfg_ts, @demo_cfg_ts);

    -- =============================================================================
    -- 脚本结束。重复执行会因主键冲突失败；清理时可按 id 段删除本文件插入的数据。
    -- =============================================================================
