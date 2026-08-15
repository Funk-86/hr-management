-- hr_employee_face 结构修复（可重复执行，已存在的列会自动跳过）
-- 适用于手工建表缺字段、或 Unknown column 'created_at' 等报错

USE hr_management;

CREATE TABLE IF NOT EXISTS hr_employee_face (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    employee_id   BIGINT       NOT NULL COMMENT '员工ID',
    descriptor    JSON         NOT NULL COMMENT '128维特征向量',
    model_version VARCHAR(32)  NOT NULL DEFAULT 'face-api-v1',
    sample_count  INT          NOT NULL DEFAULT 1,
    enrolled_by   BIGINT       DEFAULT NULL COMMENT '录入操作人 userId',
    enrolled_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_employee_id (employee_id)
) ENGINE=InnoDB COMMENT='员工人脸特征表';

DROP PROCEDURE IF EXISTS migrate_hr_employee_face;

DELIMITER $$
CREATE PROCEDURE migrate_hr_employee_face()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hr_employee_face' AND COLUMN_NAME = 'model_version'
    ) THEN
        ALTER TABLE hr_employee_face
            ADD COLUMN model_version VARCHAR(32) NOT NULL DEFAULT 'face-api-v1' COMMENT '模型版本' AFTER descriptor;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hr_employee_face' AND COLUMN_NAME = 'sample_count'
    ) THEN
        ALTER TABLE hr_employee_face
            ADD COLUMN sample_count INT NOT NULL DEFAULT 1 COMMENT '采样帧数' AFTER model_version;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hr_employee_face' AND COLUMN_NAME = 'enrolled_by'
    ) THEN
        ALTER TABLE hr_employee_face
            ADD COLUMN enrolled_by BIGINT DEFAULT NULL COMMENT '录入操作人 userId' AFTER sample_count;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hr_employee_face' AND COLUMN_NAME = 'enrolled_at'
    ) THEN
        ALTER TABLE hr_employee_face
            ADD COLUMN enrolled_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间' AFTER enrolled_by;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hr_employee_face' AND COLUMN_NAME = 'status'
    ) THEN
        ALTER TABLE hr_employee_face
            ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用' AFTER enrolled_at;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hr_employee_face' AND COLUMN_NAME = 'created_at'
    ) THEN
        ALTER TABLE hr_employee_face
            ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER status;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hr_employee_face' AND COLUMN_NAME = 'updated_at'
    ) THEN
        ALTER TABLE hr_employee_face
            ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER created_at;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hr_employee_face' AND COLUMN_NAME = 'deleted'
    ) THEN
        ALTER TABLE hr_employee_face
            ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删' AFTER updated_at;
    END IF;
END$$
DELIMITER ;

CALL migrate_hr_employee_face();
DROP PROCEDURE IF EXISTS migrate_hr_employee_face;
