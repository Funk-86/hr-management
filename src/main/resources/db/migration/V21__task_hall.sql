-- V21: 任务大厅（开放认领、协作名额、逾期策略、扣款流水、权限）

ALTER TABLE hr_task
    ADD COLUMN claim_mode VARCHAR(16) NOT NULL DEFAULT 'ASSIGNED'
        COMMENT 'ASSIGNED指派 / OPEN大厅' AFTER priority,
    ADD COLUMN claim_quota INT DEFAULT NULL
        COMMENT '大厅名额：1独占，N协作' AFTER claim_mode,
    ADD COLUMN difficulty TINYINT DEFAULT NULL
        COMMENT '难度1-5' AFTER claim_quota,
    ADD COLUMN suggest_bonus DECIMAL(10, 2) DEFAULT NULL
        COMMENT '建议奖金展示用' AFTER difficulty,
    ADD COLUMN overdue_policy VARCHAR(16) DEFAULT NULL
        COMMENT 'MARK_ONLY/ZERO_BONUS/DEDUCT' AFTER suggest_bonus,
    ADD COLUMN deduct_amount DECIMAL(10, 2) DEFAULT NULL
        COMMENT 'DEDUCT时发布手填扣款' AFTER overdue_policy,
    ADD COLUMN claimed_count INT NOT NULL DEFAULT 0
        COMMENT '当前有效认领数缓存' AFTER deduct_amount,
    ADD COLUMN version INT NOT NULL DEFAULT 0
        COMMENT '乐观锁' AFTER claimed_count;

CREATE INDEX idx_task_hall_open ON hr_task (claim_mode, status, dept_id);

ALTER TABLE hr_task_assignee
    ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'ASSIGN'
        COMMENT 'ASSIGN指派 / CLAIM抢单' AFTER employee_id;

CREATE TABLE IF NOT EXISTS hr_task_hall_deduct (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    task_id       BIGINT         NOT NULL,
    employee_id   BIGINT         NOT NULL,
    amount        DECIMAL(10, 2) NOT NULL,
    reason        VARCHAR(255)   DEFAULT NULL,
    deduct_month  CHAR(7)        NOT NULL COMMENT 'YYYY-MM',
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT         DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_emp_deduct (task_id, employee_id),
    KEY idx_emp_month (employee_id, deduct_month)
) ENGINE = InnoDB COMMENT ='任务大厅未完成扣款（按人）';

-- 页面：任务大厅（挂在项目协作 menu id=4）
INSERT INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, path, sort_order, status)
VALUES (53, 4, '任务大厅', 'page:/hr/task-hall', 1, '/hr/task-hall', 0, 1)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name), perm_code = VALUES(perm_code), path = VALUES(path);

-- 能力
INSERT INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, path, sort_order, status)
VALUES (109, 100, '大厅接取', 'feat.task.hall.claim', 2, NULL, 9, 1)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name), perm_code = VALUES(perm_code);

INSERT INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, path, sort_order, status)
VALUES (208, 200, '大厅发布', 'feat.task.hall.publish', 2, NULL, 8, 1)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name), perm_code = VALUES(perm_code);

-- 全角色可见大厅页面
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, 53 FROM sys_role r
WHERE r.id IN (1, 2, 3, 4) AND r.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = 53);

-- 仅员工可接取
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 4, 109 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 4 AND rp.permission_id = 109);

-- 仅 HR / 经理可发布（不含超管）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, 208 FROM sys_role r
WHERE r.id IN (2, 3) AND r.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = 208);
