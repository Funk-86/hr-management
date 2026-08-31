-- V19: 加班 / 异常申诉 / 外勤申请表 + 审批能力

CREATE TABLE IF NOT EXISTS hr_overtime_request (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    employee_id    BIGINT        NOT NULL,
    work_date      DATE          NOT NULL,
    start_time     TIME          NOT NULL,
    end_time       TIME          NOT NULL,
    hours          DECIMAL(4,1)  NOT NULL,
    reason         VARCHAR(500)  NOT NULL,
    status         TINYINT       NOT NULL DEFAULT 0 COMMENT '0待审 1通过 2拒绝 3撤销',
    approver_id    BIGINT        DEFAULT NULL,
    approve_time   DATETIME      DEFAULT NULL,
    approve_remark VARCHAR(255)  DEFAULT NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     BIGINT        DEFAULT NULL,
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by     BIGINT        DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_ot_emp (employee_id),
    KEY idx_ot_status (status)
) ENGINE=InnoDB COMMENT='加班申请';

CREATE TABLE IF NOT EXISTS hr_attendance_appeal (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    employee_id    BIGINT        NOT NULL,
    attend_date    DATE          NOT NULL,
    attendance_id  BIGINT        DEFAULT NULL,
    from_status    TINYINT       DEFAULT NULL,
    to_status      TINYINT       NOT NULL,
    check_in       TIME          DEFAULT NULL,
    check_out      TIME          DEFAULT NULL,
    reason         VARCHAR(500)  NOT NULL,
    status         TINYINT       NOT NULL DEFAULT 0 COMMENT '0待审 1通过 2拒绝 3撤销',
    approver_id    BIGINT        DEFAULT NULL,
    approve_time   DATETIME      DEFAULT NULL,
    approve_remark VARCHAR(255)  DEFAULT NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     BIGINT        DEFAULT NULL,
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by     BIGINT        DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_appeal_emp (employee_id),
    KEY idx_appeal_status (status)
) ENGINE=InnoDB COMMENT='考勤异常申诉';

CREATE TABLE IF NOT EXISTS hr_field_work_request (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    employee_id    BIGINT        NOT NULL,
    work_date      DATE          NOT NULL,
    location       VARCHAR(255)  NOT NULL,
    reason         VARCHAR(500)  NOT NULL,
    status         TINYINT       NOT NULL DEFAULT 0 COMMENT '0待审 1通过 2拒绝 3撤销',
    approver_id    BIGINT        DEFAULT NULL,
    approve_time   DATETIME      DEFAULT NULL,
    approve_remark VARCHAR(255)  DEFAULT NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     BIGINT        DEFAULT NULL,
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by     BIGINT        DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_fw_emp (employee_id),
    KEY idx_fw_status (status)
) ENGINE=InnoDB COMMENT='外勤申请';

INSERT INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, path, sort_order, status)
VALUES (207, 200, '考勤进阶审批', 'feat.attendance.approve', 2, NULL, 7, 1)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, 207 FROM sys_role r
WHERE r.id IN (1, 2, 3)
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = 207);
