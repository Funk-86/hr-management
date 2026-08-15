-- ============================================================
-- 扩充真实演示数据：组织/岗位/底薪字典/员工账号/任务评分样例
-- 登录用户名使用中文真实姓名（utf8mb4 支持）
-- 密码约定（与现有一致，BCrypt）：
--   Mgr@2024 → $2a$10$UwsJ1gwUGS0UPxkxmMbS7uP3813zdB40j2eehxVOnyqMpeUyGVSaO
--   Emp@2024 → $2a$10$WSXiv68JniwFaDjW6CsfKenXbcOKVHBqDCOpgHAAKbmR7zRoHhYNm
-- ============================================================

-- 1) 理清重名岗位
UPDATE sys_position
SET position_name = '初级开发工程师'
WHERE id = 4 AND position_code = 'DEV_JR';

UPDATE sys_position
SET position_name = '高级开发工程师'
WHERE id = 3 AND position_code = 'DEV';

-- 2) 扩充部门
INSERT INTO sys_department (id, parent_id, dept_name, dept_code, sort_order, status)
VALUES
    (5, 1, '产品部', 'PROD', 4, 1),
    (6, 1, '市场部', 'MKT',  5, 1)
ON DUPLICATE KEY UPDATE
    dept_name = VALUES(dept_name),
    dept_code = VALUES(dept_code),
    sort_order = VALUES(sort_order);

-- 3) 扩充岗位
INSERT INTO sys_position (id, position_name, position_code, dept_id, level, status)
VALUES
    (5,  '财务主管',       'FIN_MGR',  4, 3, 1),
    (6,  '会计',           'ACCNT',    4, 1, 1),
    (7,  '产品经理',       'PM',       5, 3, 1),
    (8,  '产品助理',       'PM_ASST',  5, 1, 1),
    (9,  '市场经理',       'MKT_MGR',  6, 3, 1),
    (10, 'HR专员',         'HR_SPEC',  3, 1, 1),
    (11, '测试工程师',     'QA',       2, 1, 1)
ON DUPLICATE KEY UPDATE
    position_name = VALUES(position_name),
    dept_id = VALUES(dept_id),
    level = VALUES(level);

-- 4) 岗位底薪字典（一岗一标准）
INSERT INTO hr_salary_base_dict (position_id, base_salary, status, remark)
VALUES
    (1,  12000.00, 1, 'HR主管标准底薪'),
    (2,  18000.00, 1, '技术经理标准底薪'),
    (3,  14000.00, 1, '高级开发工程师'),
    (4,   9000.00, 1, '初级开发工程师'),
    (5,  15000.00, 1, '财务主管'),
    (6,   8500.00, 1, '会计'),
    (7,  16000.00, 1, '产品经理'),
    (8,   8000.00, 1, '产品助理'),
    (9,  15000.00, 1, '市场经理'),
    (10,  7500.00, 1, 'HR专员'),
    (11, 10000.00, 1, '测试工程师')
ON DUPLICATE KEY UPDATE
    base_salary = VALUES(base_salary),
    status = 1,
    remark = VALUES(remark);

-- 5) 扩充员工（保留原 1~4）
INSERT INTO hr_employee (
    id, emp_no, name, gender, phone, email, dept_id, position_id,
    hire_date, employment_type, status, remark
) VALUES
    (5,  'FIN001', '钱会计', 2, '13800000005', 'accountant@example.com', 4, 6,  '2024-03-01', 1, 1, NULL),
    (6,  'PM001',  '孙产品', 1, '13800000006', 'pm@example.com',         5, 7,  '2023-09-12', 1, 1, NULL),
    (7,  'MKT001', '周市场', 2, '13800000007', 'mkt@example.com',        6, 9,  '2023-11-08', 1, 1, NULL),
    (8,  'HR002',  '吴专员', 2, '13800000008', 'hr2@example.com',        3, 10, '2025-01-06', 1, 1, NULL),
    (9,  'EMP003', '郑开发', 1, '13800000009', 'dev3@example.com',       2, 3,  '2024-08-15', 1, 1, NULL),
    (10, 'FIN002', '冯财务', 1, '13800000010', 'finance@example.com',    4, 5,  '2022-05-20', 1, 1, NULL),
    (11, 'QA001',  '陈测试', 2, '13800000011', 'qa@example.com',         2, 11, '2025-02-18', 1, 2, '试用期'),
    (12, 'PM002',  '褚助理', 1, '13800000012', 'pmasst@example.com',     5, 8,  '2025-04-01', 1, 1, NULL)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    dept_id = VALUES(dept_id),
    position_id = VALUES(position_id),
    status = VALUES(status);

UPDATE sys_department SET leader_id = 10 WHERE id = 4;
UPDATE sys_department SET leader_id = 6  WHERE id = 5;
UPDATE sys_department SET leader_id = 7  WHERE id = 6;

-- 6) 登录账号（新员工）
INSERT INTO sys_user (id, username, password, employee_id, status) VALUES
    (5,  '赵开发', '$2a$10$WSXiv68JniwFaDjW6CsfKenXbcOKVHBqDCOpgHAAKbmR7zRoHhYNm', 4,  1),
    (6,  '钱会计', '$2a$10$WSXiv68JniwFaDjW6CsfKenXbcOKVHBqDCOpgHAAKbmR7zRoHhYNm', 5,  1),
    (7,  '孙产品', '$2a$10$UwsJ1gwUGS0UPxkxmMbS7uP3813zdB40j2eehxVOnyqMpeUyGVSaO', 6,  1),
    (8,  '周市场', '$2a$10$UwsJ1gwUGS0UPxkxmMbS7uP3813zdB40j2eehxVOnyqMpeUyGVSaO', 7,  1),
    (9,  '吴专员', '$2a$10$WSXiv68JniwFaDjW6CsfKenXbcOKVHBqDCOpgHAAKbmR7zRoHhYNm', 8,  1),
    (10, '郑开发', '$2a$10$WSXiv68JniwFaDjW6CsfKenXbcOKVHBqDCOpgHAAKbmR7zRoHhYNm', 9,  1),
    (11, '冯财务', '$2a$10$UwsJ1gwUGS0UPxkxmMbS7uP3813zdB40j2eehxVOnyqMpeUyGVSaO', 10, 1),
    (12, '陈测试', '$2a$10$WSXiv68JniwFaDjW6CsfKenXbcOKVHBqDCOpgHAAKbmR7zRoHhYNm', 11, 1),
    (13, '褚助理', '$2a$10$WSXiv68JniwFaDjW6CsfKenXbcOKVHBqDCOpgHAAKbmR7zRoHhYNm', 12, 1)
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    employee_id = VALUES(employee_id),
    status = 1;

INSERT INTO sys_user_role (user_id, role_id) VALUES
    (5,  4),
    (6,  4),
    (7,  3),
    (8,  3),
    (9,  4),
    (10, 4),
    (11, 3),
    (12, 4),
    (13, 4)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 7) 回填历史薪资岗位快照，并按字典对齐底薪（保留原奖金/扣款习惯）
UPDATE hr_salary s
    JOIN hr_employee e ON e.id = s.employee_id
SET s.position_id = e.position_id
WHERE s.position_id IS NULL;

UPDATE hr_salary SET base_salary = 18000.00, actual_salary = 18000.00 + bonus - deduction WHERE employee_id = 2 AND salary_month = '2026-06';
UPDATE hr_salary SET base_salary = 14000.00, actual_salary = 14000.00 + bonus - deduction WHERE employee_id = 3 AND salary_month = '2026-06';
UPDATE hr_salary SET base_salary =  9000.00, actual_salary =  9000.00 + bonus - deduction WHERE employee_id = 4 AND salary_month = '2026-06';
UPDATE hr_salary SET base_salary = 18000.00, actual_salary = 18000.00 + bonus - deduction WHERE employee_id = 2 AND salary_month = '2026-05';

-- 补几条已发放/待发放样例（含 HR、财务、产品）
INSERT INTO hr_salary (
    employee_id, salary_month, position_id, base_salary, task_bonus, bonus, deduction, actual_salary, status, pay_date, remark
) VALUES
    (1,  '2026-07', 1,  12000.00, 300.00, 300.00, 0.00,    12300.00, 1, '2026-08-05', '含任务评分奖金'),
    (5,  '2026-07', 6,   8500.00,   0.00,   0.00, 0.00,     8500.00, 1, '2026-08-05', NULL),
    (6,  '2026-07', 7,  16000.00, 500.00, 500.00, 200.00,  16300.00, 1, '2026-08-05', NULL),
    (10, '2026-07', 5,  15000.00,   0.00, 800.00, 0.00,    15800.00, 1, '2026-08-05', '季度津贴计入奖金'),
    (1,  '2026-08', 1,  12000.00,   0.00,   0.00, 0.00,    12000.00, 0, NULL, '待汇总任务奖金后发放'),
    (5,  '2026-08', 6,   8500.00,   0.00,   0.00, 0.00,     8500.00, 0, NULL, NULL),
    (10, '2026-08', 5,  15000.00,   0.00,   0.00, 0.00,    15000.00, 0, NULL, NULL)
ON DUPLICATE KEY UPDATE
    base_salary = VALUES(base_salary),
    task_bonus = VALUES(task_bonus),
    bonus = VALUES(bonus),
    deduction = VALUES(deduction),
    actual_salary = VALUES(actual_salary);

-- 8) 2026-08 已完成并评分的任务（供月薪「任务奖金」预览）
INSERT INTO hr_task (
    id, title, content, parent_id, creator_id, dept_id, priority, status,
    start_time, due_time, created_at, deleted
) VALUES
    (1, '完成用户权限联调',
     '联调登录角色切换、菜单权限与数据权限，输出自测清单。',
     0, 2, 2, 3, 2,
     '2026-08-01 09:00:00', '2026-08-08 18:00:00', '2026-08-01 09:00:00', 0),
    (2, '修复考勤导出字段错位',
     '导出 Excel 时工号与姓名列错位，需修复并补回归用例。',
     0, 2, 2, 2, 2,
     '2026-08-04 10:00:00', '2026-08-07 18:00:00', '2026-08-04 10:00:00', 0),
    (3, '整理 8 月入职材料归档',
     '核对新员工劳动合同、身份证复印件并上传档案库。',
     0, 1, 3, 2, 2,
     '2026-08-02 09:30:00', '2026-08-06 17:30:00', '2026-08-02 09:30:00', 0),
    (4, '产品需求评审纪要输出',
     '整理本周需求评审结论，同步技术与市场。',
     0, 6, 5, 2, 1,
     '2026-08-05 14:00:00', '2026-08-12 18:00:00', '2026-08-05 14:00:00', 0),
    (5, '八月营销活动物料准备',
     '制作活动海报初稿与投放素材清单。',
     0, 7, 6, 2, 0,
     '2026-08-08 09:00:00', '2026-08-15 18:00:00', '2026-08-08 09:00:00', 0)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    status = VALUES(status);

INSERT INTO hr_task_assignee (
    task_id, employee_id, status, progress, feedback,
    accept_time, finish_time, score_grade, score_bonus, scored_by, scored_at
) VALUES
    (1, 3, 2, 100, '权限联调完成，自测清单已提交',
     '2026-08-01 10:00:00', '2026-08-06 16:20:00', 1, 500.00, 2, '2026-08-06 17:00:00'),
    (1, 4, 2, 100, '配合联调并修复两处菜单问题',
     '2026-08-01 10:05:00', '2026-08-07 11:30:00', 2, 300.00, 2, '2026-08-07 14:00:00'),
    (2, 9, 2, 100, '导出字段已对齐，补充单测',
     '2026-08-04 11:00:00', '2026-08-06 15:40:00', 2, 300.00, 2, '2026-08-06 16:10:00'),
    (2, 11, 2, 100, '补充导出回归用例并通过',
     '2026-08-04 11:10:00', '2026-08-07 09:50:00', 3, 150.00, 2, '2026-08-07 10:20:00'),
    (3, 8, 2, 100, '入职材料已归档，缺件名单已同步 HR',
     '2026-08-02 10:00:00', '2026-08-05 17:10:00', 2, 300.00, 1, '2026-08-05 17:30:00'),
    (4, 12, 1, 60, '纪要草稿已完成 60%',
     '2026-08-05 15:00:00', NULL, NULL, NULL, NULL, NULL),
    (5, 7, 0, 0, NULL,
     NULL, NULL, NULL, NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    progress = VALUES(progress),
    finish_time = VALUES(finish_time),
    score_grade = VALUES(score_grade),
    score_bonus = VALUES(score_bonus),
    scored_by = VALUES(scored_by),
    scored_at = VALUES(scored_at);

INSERT INTO hr_task_log (task_id, operator_id, action, remark, created_at) VALUES
    (1, 2, 'CREATE',  '创建任务：用户权限联调', '2026-08-01 09:00:00'),
    (1, 3, 'ACCEPT',  '接收任务', '2026-08-01 10:00:00'),
    (1, 3, 'FINISH',  '完成并提交自测清单', '2026-08-06 16:20:00'),
    (1, 2, 'SCORE',   '评分：优，奖金 500', '2026-08-06 17:00:00'),
    (2, 2, 'CREATE',  '创建任务：考勤导出修复', '2026-08-04 10:00:00'),
    (2, 9, 'FINISH',  '修复完成', '2026-08-06 15:40:00'),
    (2, 2, 'SCORE',   '评分：良，奖金 300', '2026-08-06 16:10:00'),
    (3, 1, 'CREATE',  '创建任务：入职材料归档', '2026-08-02 09:30:00'),
    (3, 8, 'FINISH',  '归档完成', '2026-08-05 17:10:00'),
    (3, 1, 'SCORE',   '评分：良，奖金 300', '2026-08-05 17:30:00');

-- 9) 补充部分考勤（8 月，忽略已存在）
INSERT IGNORE INTO hr_attendance (employee_id, attend_date, check_in, check_out, status, work_hours) VALUES
    (3,  '2026-08-04', '09:00:00', '18:05:00', 1, 8.0),
    (3,  '2026-08-05', '09:12:00', '18:00:00', 2, 7.8),
    (3,  '2026-08-06', '09:00:00', '18:20:00', 1, 8.3),
    (9,  '2026-08-04', '08:58:00', '18:00:00', 1, 8.0),
    (9,  '2026-08-05', '09:00:00', '18:00:00', 1, 8.0),
    (9,  '2026-08-06', '09:00:00', '18:10:00', 1, 8.1),
    (11, '2026-08-04', '09:00:00', '18:00:00', 1, 8.0),
    (11, '2026-08-05', '09:00:00', '18:00:00', 1, 8.0),
    (8,  '2026-08-04', '09:00:00', '17:50:00', 1, 7.8),
    (8,  '2026-08-05', '09:00:00', '18:00:00', 1, 8.0),
    (5,  '2026-08-05', '09:00:00', '18:00:00', 1, 8.0),
    (6,  '2026-08-05', '08:50:00', '18:30:00', 1, 8.6);
