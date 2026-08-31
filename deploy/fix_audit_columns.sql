-- 线上补齐 BaseEntity 审计字段（可重复执行：列已存在会报错可忽略）
-- 用法见说明，或：
-- docker exec -i hr-mysql mysql -uroot -p'你的ROOT密码' hr_management < fix_audit_columns.sql

ALTER TABLE sys_user          ADD COLUMN created_by BIGINT NULL, ADD COLUMN updated_by BIGINT NULL;
ALTER TABLE sys_role          ADD COLUMN created_by BIGINT NULL, ADD COLUMN updated_by BIGINT NULL;
ALTER TABLE sys_permission    ADD COLUMN created_by BIGINT NULL, ADD COLUMN updated_by BIGINT NULL;
ALTER TABLE sys_department    ADD COLUMN created_by BIGINT NULL, ADD COLUMN updated_by BIGINT NULL;
ALTER TABLE sys_position      ADD COLUMN created_by BIGINT NULL, ADD COLUMN updated_by BIGINT NULL;
ALTER TABLE hr_employee       ADD COLUMN created_by BIGINT NULL, ADD COLUMN updated_by BIGINT NULL;
ALTER TABLE hr_attendance     ADD COLUMN created_by BIGINT NULL, ADD COLUMN updated_by BIGINT NULL;
ALTER TABLE hr_leave_type     ADD COLUMN created_by BIGINT NULL, ADD COLUMN updated_by BIGINT NULL;
ALTER TABLE hr_leave_request  ADD COLUMN created_by BIGINT NULL, ADD COLUMN updated_by BIGINT NULL;
ALTER TABLE hr_salary         ADD COLUMN created_by BIGINT NULL, ADD COLUMN updated_by BIGINT NULL;
ALTER TABLE hr_employee_face  ADD COLUMN created_by BIGINT NULL, ADD COLUMN updated_by BIGINT NULL;
ALTER TABLE sys_operation_log ADD COLUMN created_by BIGINT NULL, ADD COLUMN updated_by BIGINT NULL;
ALTER TABLE hr_task           ADD COLUMN created_by BIGINT NULL, ADD COLUMN updated_by BIGINT NULL;
ALTER TABLE hr_task_assignee  ADD COLUMN created_by BIGINT NULL, ADD COLUMN updated_by BIGINT NULL;
