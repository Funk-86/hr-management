-- 页面字典（perm_type=1）与用户权限字典（perm_type=2）种子，并按角色默认勾选
-- 角色：1 SUPER_ADMIN / 2 HR_ADMIN / 3 DEPT_MANAGER / 4 EMPLOYEE

ALTER TABLE sys_permission
    MODIFY COLUMN perm_name VARCHAR(128) NOT NULL COMMENT '权限名称',
    MODIFY COLUMN perm_code VARCHAR(128) NOT NULL COMMENT '权限编码',
    MODIFY COLUMN path VARCHAR(255) DEFAULT NULL COMMENT '路由/接口路径';

DELETE FROM sys_role_permission;
DELETE FROM sys_permission;

-- ========== 页面字典（菜单） ==========
INSERT INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, path, sort_order, status) VALUES
(1, 0, '工作台',     'menu.workspace',      1, '/dashboard',           0, 1),
(2, 0, '组织人事',   'menu.org',            1, '/org',                 1, 1),
(3, 0, '考勤假期',   'menu.attendance',     1, '/attendance-module',   2, 1),
(4, 0, '项目协作',   'menu.collaboration',  1, '/collaboration',       3, 1),
(5, 0, '薪酬绩效',   'menu.comp',           1, '/comp-perf',           4, 1),
(6, 0, '数据分析',   'menu.analytics',      1, '/data-analytics',      5, 1),
(7, 0, '字典管理',   'menu.dict',           1, '/dict',                8, 1),
(8, 0, '系统审计',   'menu.audit',          1, '/system-audit',        9, 1),

(10, 1, '工作台首页', 'page:/workspace',           1, '/workspace',           1, 1),

(20, 2, '部门管理',   'page:/hr/department',       1, '/hr/department',       1, 1),
(21, 2, '岗位管理',   'page:/hr/position',         1, '/hr/position',         2, 1),
(22, 2, '员工管理',   'page:/hr/employee',         1, '/hr/employee',         3, 1),
(23, 2, '入转调离',   'page:/hr/personnel',        1, '/hr/personnel',        4, 1),
(24, 2, '文档管理',   'page:/hr/document',         1, '/hr/document',         5, 1),

(30, 3, '考勤管理',   'page:/hr/attendance',       1, '/hr/attendance',       1, 1),
(31, 3, '请假管理',   'page:/hr/leave',            1, '/hr/leave',            2, 1),

(40, 4, '项目管理',   'page:/hr/project',          1, '/hr/project',          1, 1),
(41, 4, '任务管理',   'page:/hr/task',             1, '/hr/task',             2, 1),

(50, 5, '薪资管理',   'page:/hr/salary',           1, '/hr/salary',           1, 1),
(51, 5, '绩效考核',   'page:/hr/performance',      1, '/hr/performance',      2, 1),

(60, 6, '统计看板',   'page:/hr/stats',            1, '/hr/stats',            1, 1),

(70, 7, '薪资字典',       'page:/dict/salary',          1, '/dict/salary',          1, 1),
(71, 7, '用户权限字典',   'page:/dict/role-permission', 1, '/dict/role-permission', 2, 1),
(72, 7, '页面字典',       'page:/dict/page-permission', 1, '/dict/page-permission', 3, 1),

(80, 8, '操作日志',   'page:/hr/operation-log',    1, '/hr/operation-log',    1, 1);

-- ========== 用户权限字典（能力） ==========
INSERT INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, path, sort_order, status) VALUES
(100, 0, '员工能力',     'feat.group.employee',   2, NULL, 1, 1),
(101, 100, '本人考勤',   'feat.attendance.self',  2, NULL, 1, 1),
(102, 100, '请假申请',   'feat.leave.apply',      2, NULL, 2, 1),
(103, 100, '任务执行',   'feat.task.execute',     2, NULL, 3, 1),
(104, 100, '本人文档',   'feat.document.self',    2, NULL, 4, 1),
(105, 100, '查看绩效',   'feat.performance.view', 2, NULL, 5, 1),
(106, 100, '异动申请',   'feat.personnel.apply',  2, NULL, 6, 1),
(107, 100, '参与项目',   'feat.project.join',     2, NULL, 7, 1),

(200, 0, '经理能力',     'feat.group.manager',      2, NULL, 2, 1),
(201, 200, '员工管理',   'feat.employee.manage',    2, NULL, 1, 1),
(202, 200, '请假审批',   'feat.leave.approve',      2, NULL, 2, 1),
(203, 200, '任务下发',   'feat.task.create',        2, NULL, 3, 1),
(204, 200, '项目管理',   'feat.project.manage',     2, NULL, 4, 1),
(205, 200, '绩效评分',   'feat.performance.score',  2, NULL, 5, 1),
(206, 200, '异动审批',   'feat.personnel.approve',  2, NULL, 6, 1),

(300, 0, 'HR能力',       'feat.group.hr',           2, NULL, 3, 1),
(301, 300, '组织维护',   'feat.org.manage',         2, NULL, 1, 1),
(302, 300, '薪资管理',   'feat.salary.manage',      2, NULL, 2, 1),
(303, 300, '统计看板',   'feat.stats.view',         2, NULL, 3, 1),
(304, 300, '操作审计',   'feat.audit.view',         2, NULL, 4, 1),
(305, 300, '异动生效',   'feat.personnel.effect',   2, NULL, 5, 1),

(400, 0, '超管能力',     'feat.group.admin',        2, NULL, 4, 1),
(401, 400, '字典管理',   'feat.dict.manage',        2, NULL, 1, 1),
(402, 400, '权限配置',   'feat.role.manage',        2, NULL, 2, 1);

-- ========== 默认勾选：页面（各角色互不重复插入） ==========
-- 员工
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 4, id FROM sys_permission WHERE id IN (1,10, 2,23,24, 3,30,31, 4,40,41, 5,51);

-- 经理
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE id IN (1,10, 2,22,23,24, 3,30,31, 4,40,41, 5,51);

-- HR
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE id IN (1,10, 2,20,21,22,23,24, 3,30,31, 4,40,41, 5,50,51, 6,60, 8,80);

-- 超管：全部页面
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE perm_type = 1;

-- ========== 默认勾选：能力 ==========
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 4, id FROM sys_permission WHERE id BETWEEN 100 AND 107;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE id BETWEEN 100 AND 107 OR id BETWEEN 200 AND 206;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE id BETWEEN 100 AND 107 OR id BETWEEN 200 AND 206 OR id BETWEEN 300 AND 305;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE perm_type = 2;
