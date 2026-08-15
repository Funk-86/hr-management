-- ============================================================
-- 增量补数脚本（已有库执行，无需重建）
-- 用法: mysql -u root -p hr_management < seed-test-data.sql
-- 说明: 会清理 id 1-4 的测试员工及相关业务数据后重新写入
-- ============================================================

USE hr_management;

-- 1. 各账号独立密码与 employee_id 绑定
UPDATE sys_user SET password = '$2a$10$L8fR0Dl4uTcjZ8Q8V.33R.O2XQaflsOmnkJBfUWjpzXZ.kt8HcY8y', employee_id = NULL WHERE username = 'admin';
UPDATE sys_user SET password = '$2a$10$BC4jmJLwF1pCzhA9puK6buySp5Z3JdMbkorxNG2Gk1KzaCooH8n06', employee_id = 1    WHERE username = 'hr';
UPDATE sys_user SET password = '$2a$10$UwsJ1gwUGS0UPxkxmMbS7uP3813zdB40j2eehxVOnyqMpeUyGVSaO', employee_id = 2    WHERE username = 'manager';
UPDATE sys_user SET password = '$2a$10$WSXiv68JniwFaDjW6CsfKenXbcOKVHBqDCOpgHAAKbmR7zRoHhYNm', employee_id = 3    WHERE username = 'employee';

-- 2. 清理旧测试业务数据（保留角色/用户/假期类型）
DELETE FROM hr_salary      WHERE employee_id IN (1, 2, 3, 4);
DELETE FROM hr_leave_request WHERE employee_id IN (1, 2, 3, 4);
DELETE FROM hr_attendance  WHERE employee_id IN (1, 2, 3, 4);
DELETE FROM hr_employee    WHERE id IN (1, 2, 3, 4);
DELETE FROM sys_position   WHERE id IN (1, 2, 3, 4);
DELETE FROM sys_department WHERE id IN (1, 2, 3, 4);

-- 3. 组织架构
INSERT INTO sys_department (id, parent_id, dept_name, dept_code, sort_order) VALUES
(1, 0, '总公司',     'ROOT', 0),
(2, 1, '技术部',     'TECH', 1),
(3, 1, '人力资源部', 'HR',   2),
(4, 1, '财务部',     'FIN',  3);

INSERT INTO sys_position (id, position_name, position_code, dept_id, level) VALUES
(1, 'HR主管',     'HR_MGR',   3, 3),
(2, '技术经理',   'TECH_MGR', 2, 3),
(3, '开发工程师', 'DEV',      2, 1),
(4, '开发工程师', 'DEV_JR',   2, 1);

INSERT INTO hr_employee (id, emp_no, name, gender, phone, email, dept_id, position_id, hire_date, status) VALUES
(1, 'HR001',  '张人事', 2, '13800000001', 'hr@example.com',       3, 1, '2024-01-15', 1),
(2, 'MGR001', '李经理', 1, '13800000002', 'manager@example.com',  2, 2, '2023-06-01', 1),
(3, 'EMP001', '王员工', 1, '13800000003', 'employee@example.com', 2, 3, '2025-03-10', 1),
(4, 'EMP002', '赵开发', 1, '13800000004', 'dev2@example.com',     2, 4, '2025-05-20', 1);

UPDATE sys_department SET leader_id = 1 WHERE id = 3;
UPDATE sys_department SET leader_id = 2 WHERE id = 2;

-- 4. 考勤 / 请假 / 薪资样例（与 schema.sql 一致）
INSERT INTO hr_attendance (employee_id, attend_date, check_in, check_out, status, work_hours) VALUES
(3, '2026-06-02', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-03', '09:22:00', '18:05:00', 2, 7.7),
(3, '2026-06-04', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-05', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-06', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-09', '09:18:00', '18:00:00', 2, 7.8),
(3, '2026-06-10', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-11', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-12', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-13', '09:35:00', '18:00:00', 2, 7.5),
(3, '2026-06-16', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-17', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-18', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-19', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-20', '09:10:00', '18:00:00', 2, 7.8),
(3, '2026-06-23', '09:00:00', '12:00:00', 1, 3.0),
(2, '2026-06-23', '08:55:00', '18:30:00', 1, 8.5),
(2, '2026-06-20', '09:00:00', '18:00:00', 1, 8.0),
(2, '2026-06-19', '09:00:00', '18:00:00', 1, 8.0);

INSERT INTO hr_leave_request (employee_id, leave_type_id, start_time, end_time, days, reason, status) VALUES
(3, 3, '2026-06-25 09:00:00', '2026-06-26 18:00:00', 2.0, '家中有事需处理', 0),
(4, 1, '2026-06-27 09:00:00', '2026-06-27 18:00:00', 1.0, '申请年假休息一天', 0);

INSERT INTO hr_leave_request (employee_id, leave_type_id, start_time, end_time, days, reason, status, approver_id, approve_time, approve_remark) VALUES
(3, 2, '2026-05-10 09:00:00', '2026-05-10 18:00:00', 1.0, '感冒发烧', 1, 3, '2026-05-09 16:30:00', '注意休息'),
(4, 3, '2026-05-15 09:00:00', '2026-05-16 18:00:00', 2.0, '个人事务', 2, 3, '2026-05-14 10:00:00', '项目紧张，请改期');

INSERT INTO hr_salary (employee_id, salary_month, base_salary, bonus, deduction, actual_salary, status) VALUES
(2, '2026-06', 15000.00, 2000.00,  500.00, 16500.00, 0),
(3, '2026-06', 10000.00,  500.00,    0.00, 10500.00, 0),
(4, '2026-06',  9500.00,    0.00,    0.00,  9500.00, 0),
(2, '2026-05', 15000.00, 3000.00,  500.00, 17500.00, 1);

SELECT 'seed-test-data 执行完成' AS message;
SELECT username, employee_id FROM sys_user WHERE username IN ('admin','hr','manager','employee');
