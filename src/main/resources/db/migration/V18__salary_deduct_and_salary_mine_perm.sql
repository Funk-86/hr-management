-- V18: 考勤扣款规则 + 我的薪资条页面/能力权限

CREATE TABLE IF NOT EXISTS hr_attendance_deduct_rule (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    rule_code    VARCHAR(32)    NOT NULL COMMENT 'LATE/ABSENT/MISSING_CHECK',
    unit_amount  DECIMAL(10,2)  NOT NULL DEFAULT 0 COMMENT '每次/每天扣款金额',
    enabled      TINYINT        NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    remark       VARCHAR(255)   DEFAULT NULL,
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   BIGINT         DEFAULT NULL,
    updated_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by   BIGINT         DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rule_code (rule_code)
) ENGINE=InnoDB COMMENT='考勤扣款规则';

INSERT INTO hr_attendance_deduct_rule (rule_code, unit_amount, enabled, remark) VALUES
('LATE', 50.00, 1, '迟到每天扣款'),
('ABSENT', 200.00, 1, '缺勤每天扣款'),
('MISSING_CHECK', 100.00, 1, '缺卡（缺上班或下班）每天扣款')
ON DUPLICATE KEY UPDATE remark = VALUES(remark);

-- 页面：我的薪资条
INSERT INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, path, sort_order, status)
VALUES (52, 5, '我的薪资条', 'page:/hr/salary-mine', 1, '/hr/salary-mine', 0, 1)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name);

-- 能力
INSERT INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, path, sort_order, status) VALUES
(108, 100, '查看本人薪资条', 'feat.salary.self', 2, NULL, 8, 1),
(306, 300, '假期余额管理', 'feat.leave.balance.manage', 2, NULL, 6, 1)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name);

-- 全员可见我的薪资条页面 + feat.salary.self
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, 52 FROM sys_role r
WHERE r.id IN (1, 2, 3, 4)
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = 52);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, 108 FROM sys_role r
WHERE r.id IN (1, 2, 3, 4)
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = 108);

-- HR/超管：余额管理
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, 306 FROM sys_role r
WHERE r.id IN (1, 2)
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = 306);
