-- V6: 任务表补齐 BaseEntity 审计字段 created_by / updated_by

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

CALL add_audit_column('hr_task', 'created_by');
CALL add_audit_column('hr_task', 'updated_by');
CALL add_audit_column('hr_task_assignee', 'created_by');
CALL add_audit_column('hr_task_assignee', 'updated_by');

DROP PROCEDURE IF EXISTS add_audit_column;
