-- V17: 假期余额台账；已通过请假回填 used_days；考勤 status 注释含外勤预留

CREATE TABLE IF NOT EXISTS hr_leave_balance (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '余额ID',
    employee_id   BIGINT        NOT NULL COMMENT '员工ID',
    leave_type_id BIGINT        NOT NULL COMMENT '假期类型ID',
    year          INT           NOT NULL COMMENT '年度',
    quota_days    DECIMAL(6,1)  NOT NULL COMMENT '年度额度',
    used_days     DECIMAL(6,1)  NOT NULL DEFAULT 0 COMMENT '已通过占用',
    pending_days  DECIMAL(6,1)  NOT NULL DEFAULT 0 COMMENT '待审批占用',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT        DEFAULT NULL,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by    BIGINT        DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_emp_type_year (employee_id, leave_type_id, year),
    KEY idx_employee_year (employee_id, year)
) ENGINE=InnoDB COMMENT='假期余额台账';

-- 回填：按已通过请假累计 used_days（不限额类型跳过；无余额行则创建）
INSERT INTO hr_leave_balance (employee_id, leave_type_id, year, quota_days, used_days, pending_days)
SELECT
    r.employee_id,
    r.leave_type_id,
    YEAR(r.start_time) AS year,
    CAST(t.max_days AS DECIMAL(6,1)) AS quota_days,
    SUM(r.days) AS used_days,
    0 AS pending_days
FROM hr_leave_request r
INNER JOIN hr_leave_type t ON t.id = r.leave_type_id
WHERE r.status = 1
  AND t.max_days IS NOT NULL
GROUP BY r.employee_id, r.leave_type_id, YEAR(r.start_time), t.max_days
ON DUPLICATE KEY UPDATE
    used_days = VALUES(used_days);

-- 回填待审 pending
INSERT INTO hr_leave_balance (employee_id, leave_type_id, year, quota_days, used_days, pending_days)
SELECT
    r.employee_id,
    r.leave_type_id,
    YEAR(r.start_time) AS year,
    CAST(t.max_days AS DECIMAL(6,1)) AS quota_days,
    0 AS used_days,
    SUM(r.days) AS pending_days
FROM hr_leave_request r
INNER JOIN hr_leave_type t ON t.id = r.leave_type_id
WHERE r.status = 0
  AND t.max_days IS NOT NULL
GROUP BY r.employee_id, r.leave_type_id, YEAR(r.start_time), t.max_days
ON DUPLICATE KEY UPDATE
    pending_days = VALUES(pending_days);
