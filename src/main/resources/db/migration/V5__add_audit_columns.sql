-- V5: 为所有业务表添加审计字段 created_by / updated_by
-- 使用 INFORMATION_SCHEMA 检查避免重复添加

-- 存储过程：仅在列不存在时添加
DROP PROCEDURE IF EXISTS add_audit_column;
CREATE PROCEDURE add_audit_column(IN tbl VARCHAR(64), IN col VARCHAR(64))
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tbl
          AND COLUMN_NAME = col
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', tbl, ' ADD COLUMN ', col, ' BIGINT NULL COMMENT ''审计字段''');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END;

-- 为所有表添加 created_by
CALL add_audit_column('sys_user', 'created_by');
CALL add_audit_column('sys_user', 'updated_by');
CALL add_audit_column('sys_role', 'created_by');
CALL add_audit_column('sys_role', 'updated_by');
CALL add_audit_column('sys_permission', 'created_by');
CALL add_audit_column('sys_permission', 'updated_by');
CALL add_audit_column('sys_department', 'created_by');
CALL add_audit_column('sys_department', 'updated_by');
CALL add_audit_column('sys_position', 'created_by');
CALL add_audit_column('sys_position', 'updated_by');
CALL add_audit_column('hr_employee', 'created_by');
CALL add_audit_column('hr_employee', 'updated_by');
CALL add_audit_column('hr_attendance', 'created_by');
CALL add_audit_column('hr_attendance', 'updated_by');
CALL add_audit_column('hr_leave_type', 'created_by');
CALL add_audit_column('hr_leave_type', 'updated_by');
CALL add_audit_column('hr_leave_request', 'created_by');
CALL add_audit_column('hr_leave_request', 'updated_by');
CALL add_audit_column('hr_salary', 'created_by');
CALL add_audit_column('hr_salary', 'updated_by');
CALL add_audit_column('hr_employee_face', 'created_by');
CALL add_audit_column('hr_employee_face', 'updated_by');
CALL add_audit_column('sys_operation_log', 'created_by');
CALL add_audit_column('sys_operation_log', 'updated_by');

DROP PROCEDURE IF EXISTS add_audit_column;
