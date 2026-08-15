-- 项目管理
CREATE TABLE IF NOT EXISTS hr_project (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    name            VARCHAR(128)  NOT NULL COMMENT '项目名称',
    description     VARCHAR(1000) DEFAULT NULL COMMENT '项目说明',
    owner_id        BIGINT        NOT NULL COMMENT '负责人（员工ID，承接人）',
    dept_id         BIGINT        DEFAULT NULL COMMENT '归属部门',
    status          TINYINT       NOT NULL DEFAULT 1 COMMENT '0规划 1进行中 2已完成 3已关闭',
    progress        TINYINT       NOT NULL DEFAULT 0 COMMENT '进度 0-100（可人工确认）',
    progress_locked TINYINT       NOT NULL DEFAULT 0 COMMENT '1=承接人已锁定/手调进度，不再自动覆盖',
    start_date      DATE          DEFAULT NULL,
    end_date        DATE          DEFAULT NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT        DEFAULT NULL,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by      BIGINT        DEFAULT NULL,
    deleted         TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_owner (owner_id),
    KEY idx_status (status)
) ENGINE=InnoDB COMMENT='项目表';

CREATE TABLE IF NOT EXISTS hr_project_member (
    id            BIGINT   NOT NULL AUTO_INCREMENT,
    project_id    BIGINT   NOT NULL,
    employee_id   BIGINT   NOT NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT   DEFAULT NULL,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by    BIGINT   DEFAULT NULL,
    deleted       TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_emp (project_id, employee_id),
    KEY idx_employee (employee_id)
) ENGINE=InnoDB COMMENT='项目成员';

-- 任务挂项目（可重复执行）
SET @exist := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'hr_task'
      AND COLUMN_NAME = 'project_id'
);
SET @sql := IF(
    @exist = 0,
    'ALTER TABLE hr_task ADD COLUMN project_id BIGINT DEFAULT NULL COMMENT ''所属项目ID'' AFTER parent_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
