-- 员工个人底薪覆盖（调薪生效写入；发薪优先于岗位底薪字典）
SET @exist := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'hr_employee'
      AND COLUMN_NAME = 'base_salary'
);
SET @sql := IF(
    @exist = 0,
    'ALTER TABLE hr_employee ADD COLUMN base_salary DECIMAL(10,2) DEFAULT NULL COMMENT ''个人底薪（调薪生效后覆盖岗位字典）'' AFTER position_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 人事异动：调岗 / 调薪 / 离职 / 入职完善
CREATE TABLE IF NOT EXISTS hr_personnel_change (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    change_type       TINYINT        NOT NULL COMMENT '1调岗 2调薪 3离职 4入职完善',
    employee_id       BIGINT         NOT NULL,
    from_dept_id      BIGINT         DEFAULT NULL,
    to_dept_id        BIGINT         DEFAULT NULL,
    from_position_id  BIGINT         DEFAULT NULL,
    to_position_id    BIGINT         DEFAULT NULL,
    old_salary        DECIMAL(10,2)  DEFAULT NULL,
    new_salary        DECIMAL(10,2)  DEFAULT NULL,
    effective_date    DATE           DEFAULT NULL COMMENT '计划/实际生效日',
    reason            VARCHAR(500)   DEFAULT NULL,
    status            TINYINT        NOT NULL DEFAULT 0 COMMENT '0待审批 1已通过 2已拒绝 3已撤销 4已生效',
    applicant_id      BIGINT         NOT NULL COMMENT '申请人（员工ID）',
    approver_id       BIGINT         DEFAULT NULL COMMENT '审批人（员工ID）',
    approve_remark    VARCHAR(500)   DEFAULT NULL,
    approved_at       DATETIME       DEFAULT NULL,
    effected_at       DATETIME       DEFAULT NULL,
    effected_by       BIGINT         DEFAULT NULL,
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        BIGINT         DEFAULT NULL,
    updated_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by        BIGINT         DEFAULT NULL,
    deleted           TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_employee (employee_id),
    KEY idx_type_status (change_type, status),
    KEY idx_applicant (applicant_id)
) ENGINE=InnoDB COMMENT='人事异动单';
