# 菜单 V1 与功能规格差异说明

## 我的菜单（MENU-007）

- **规格**：按用户有效权限集合 + 菜单绑定权限（MENU-006）过滤可见树。
- **实现**：`GET /tenant/me/menus` 使用 **`sys_role_menu`**（用户角色 → 角色菜单 → 启用且未删除的菜单），**不**读取菜单侧权限绑定。
- **侧栏**：前端对 `hidden=1` 的节点不渲染该项，其子项提升到同级展示（见 `App.jsx`）。

## MENU-006

- **规格**：`POST /tenant/menus/{menuId}/permissions:replace`。
- **V1**：未实现；`sys_menu` 无菜单侧权限标识列。

## 操作点（产品 4.6）

- **ACT-*** 与 `sys_action` 全链路另立里程碑，本迭代不交付。

## 审计

- 菜单写操作在 `MenuService` 中写入 `sys_tenant_audit`（事件如 `MENU_CREATE`、`MENU_UPDATE`、`MENU_REORDER`、`MENU_ENABLE`、`MENU_DISABLE`、`MENU_DELETE`、`MENU_MOVE`、`MENU_RESTORE`），`context_text` 为 `menu`。
- 全局 **tenant.menu.*** 注解鉴权若未启用，仍以会话登录为准；与规格差异属技术债。
