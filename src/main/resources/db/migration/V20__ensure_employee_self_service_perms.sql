-- V20: 确保普通员工具备自助页所需的页面权限与能力码
-- 覆盖场景：库已跑过 V16/V18，但角色权限被手动改乱；或升级后接口改为 hasAuthority(feat.*) 后员工缺能力码

-- 先确保「我的薪资条」权限字典存在（幂等）
INSERT INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, path, sort_order, status)
VALUES (52, 5, '我的薪资条', 'page:/hr/salary-mine', 1, '/hr/salary-mine', 0, 1)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name), perm_code = VALUES(perm_code), path = VALUES(path);

INSERT INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, path, sort_order, status)
VALUES (108, 100, '查看本人薪资条', 'feat.salary.self', 2, NULL, 8, 1)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name), perm_code = VALUES(perm_code);

-- 页面：组织人事自助 / 考勤假期 / 我的薪资条 / 绩效 / 协作
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 4, p.id
FROM sys_permission p
WHERE p.id IN (1, 10, 2, 23, 24, 3, 30, 31, 4, 40, 41, 5, 51, 52)
  AND p.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = 4 AND rp.permission_id = p.id
  );

-- 能力：员工自助 feat（含 feat.salary.self=108）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 4, p.id
FROM sys_permission p
WHERE p.id IN (100, 101, 102, 103, 104, 105, 106, 107, 108)
  AND p.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = 4 AND rp.permission_id = p.id
  );

-- 全角色再次确保「我的薪资条」页面 + feat.salary.self
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, 52 FROM sys_role r
WHERE r.id IN (1, 2, 3, 4) AND r.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = 52);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, 108 FROM sys_role r
WHERE r.id IN (1, 2, 3, 4) AND r.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = 108);
