-- ============================================================
-- HR 人事管理系统 - 数据库初始化脚本
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS hr_management
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE hr_management;

-- ------------------------------------------------------------
-- 1. 组织架构
-- ------------------------------------------------------------

-- 部门表（支持树形结构）
CREATE TABLE sys_department (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '部门ID',
    parent_id   BIGINT       DEFAULT 0 COMMENT '父部门ID，0表示根节点',
    dept_name   VARCHAR(64)  NOT NULL COMMENT '部门名称',
    dept_code   VARCHAR(32)  NOT NULL COMMENT '部门编码',
    leader_id   BIGINT       DEFAULT NULL COMMENT '部门负责人（员工ID）',
    sort_order  INT          DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dept_code (dept_code),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB COMMENT='部门表';

-- 岗位表
CREATE TABLE sys_position (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
    position_name VARCHAR(64)  NOT NULL COMMENT '岗位名称',
    position_code VARCHAR(32)  NOT NULL COMMENT '岗位编码',
    dept_id       BIGINT       NOT NULL COMMENT '所属部门ID',
    level         TINYINT      DEFAULT 1 COMMENT '职级：1-普通 2-主管 3-经理 4-总监',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_position_code (position_code),
    KEY idx_dept_id (dept_id)
) ENGINE=InnoDB COMMENT='岗位表';

-- ------------------------------------------------------------
-- 2. 员工管理
-- ------------------------------------------------------------

CREATE TABLE hr_employee (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '员工ID',
    emp_no          VARCHAR(32)  NOT NULL COMMENT '工号',
    name            VARCHAR(64)  NOT NULL COMMENT '姓名',
    gender          TINYINT      DEFAULT 1 COMMENT '性别：1-男 2-女',
    phone           VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    email           VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    id_card         VARCHAR(18)  DEFAULT NULL COMMENT '身份证号',
    dept_id         BIGINT       NOT NULL COMMENT '所属部门ID',
    position_id     BIGINT       NOT NULL COMMENT '岗位ID',
    hire_date       DATE         NOT NULL COMMENT '入职日期',
    probation_end   DATE         DEFAULT NULL COMMENT '试用期结束日期',
    employment_type TINYINT      DEFAULT 1 COMMENT '用工类型：1-全职 2-兼职 3-实习',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-在职 2-试用期 3-离职',
    leave_date      DATE         DEFAULT NULL COMMENT '离职日期',
    avatar          VARCHAR(512) DEFAULT NULL COMMENT '头像 OSS 对象 Key',
    remark          VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_emp_no (emp_no),
    KEY idx_dept_id (dept_id),
    KEY idx_position_id (position_id),
    KEY idx_status (status)
) ENGINE=InnoDB COMMENT='员工表';

-- ------------------------------------------------------------
-- 3. 用户与权限（RBAC）
-- ------------------------------------------------------------

CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(64)  NOT NULL COMMENT '登录用户名',
    password    VARCHAR(128) NOT NULL COMMENT '密码（BCrypt加密）',
    employee_id BIGINT       DEFAULT NULL COMMENT '关联员工ID',
    avatar      VARCHAR(512) DEFAULT NULL COMMENT '头像 OSS 对象 Key（无关联员工时使用）',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    last_login  DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_employee_id (employee_id)
) ENGINE=InnoDB COMMENT='系统用户表';

CREATE TABLE sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_name   VARCHAR(64)  NOT NULL COMMENT '角色名称',
    role_code   VARCHAR(32)  NOT NULL COMMENT '角色编码',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB COMMENT='角色表';

CREATE TABLE sys_permission (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    parent_id   BIGINT       DEFAULT 0 COMMENT '父权限ID',
    perm_name   VARCHAR(64)  NOT NULL COMMENT '权限名称',
    perm_code   VARCHAR(64)  NOT NULL COMMENT '权限编码',
    perm_type   TINYINT      NOT NULL COMMENT '类型：1-菜单 2-按钮 3-接口',
    path        VARCHAR(128) DEFAULT NULL COMMENT '路由/接口路径',
    method      VARCHAR(16)  DEFAULT NULL COMMENT 'HTTP方法',
    sort_order  INT          DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_perm_code (perm_code)
) ENGINE=InnoDB COMMENT='权限表';

CREATE TABLE sys_user_role (
    id      BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB COMMENT='用户角色关联表';

CREATE TABLE sys_role_permission (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE=InnoDB COMMENT='角色权限关联表';

-- ------------------------------------------------------------
-- 4. 考勤管理
-- ------------------------------------------------------------

CREATE TABLE hr_attendance (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '考勤ID',
    employee_id  BIGINT      NOT NULL COMMENT '员工ID',
    attend_date  DATE        NOT NULL COMMENT '考勤日期',
    check_in     TIME        DEFAULT NULL COMMENT '上班打卡时间',
    check_out    TIME        DEFAULT NULL COMMENT '下班打卡时间',
    status       TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1-正常 2-迟到 3-早退 4-缺勤 5-请假',
    work_hours   DECIMAL(4,1) DEFAULT NULL COMMENT '工作时长（小时）',
    remark       VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_emp_date (employee_id, attend_date),
    KEY idx_attend_date (attend_date)
) ENGINE=InnoDB COMMENT='考勤记录表';

-- ------------------------------------------------------------
-- 5. 请假管理
-- ------------------------------------------------------------

CREATE TABLE hr_leave_type (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '假期类型ID',
    type_name   VARCHAR(32)  NOT NULL COMMENT '类型名称（年假/病假/事假等）',
    type_code   VARCHAR(16)  NOT NULL COMMENT '类型编码',
    max_days    INT          DEFAULT NULL COMMENT '每年最大天数，NULL表示不限',
    status      TINYINT      NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_type_code (type_code)
) ENGINE=InnoDB COMMENT='假期类型表';

CREATE TABLE hr_leave_request (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '请假ID',
    employee_id   BIGINT       NOT NULL COMMENT '申请人ID',
    leave_type_id BIGINT       NOT NULL COMMENT '假期类型ID',
    start_time    DATETIME     NOT NULL COMMENT '开始时间',
    end_time      DATETIME     NOT NULL COMMENT '结束时间',
    days          DECIMAL(4,1) NOT NULL COMMENT '请假天数',
    reason        VARCHAR(500) NOT NULL COMMENT '请假原因',
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0-待审批 1-已通过 2-已拒绝 3-已撤销',
    approver_id   BIGINT       DEFAULT NULL COMMENT '审批人ID',
    approve_time  DATETIME     DEFAULT NULL COMMENT '审批时间',
    approve_remark VARCHAR(255) DEFAULT NULL COMMENT '审批备注',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_employee_id (employee_id),
    KEY idx_status (status)
) ENGINE=InnoDB COMMENT='请假申请表';

-- ------------------------------------------------------------
-- 6. 薪资管理（简化版）
-- ------------------------------------------------------------

CREATE TABLE hr_salary (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '薪资记录ID',
    employee_id   BIGINT        NOT NULL COMMENT '员工ID',
    salary_month  CHAR(7)       NOT NULL COMMENT '薪资月份，如 2026-06',
    base_salary   DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '基本工资',
    bonus         DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '奖金',
    deduction     DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '扣款',
    actual_salary DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '实发工资',
    status        TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0-待发放 1-已发放',
    pay_date      DATE          DEFAULT NULL COMMENT '发放日期',
    remark        VARCHAR(255)  DEFAULT NULL,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_emp_month (employee_id, salary_month)
) ENGINE=InnoDB COMMENT='薪资表';

-- ------------------------------------------------------------
-- 7. 操作日志（审计）
-- ------------------------------------------------------------

CREATE TABLE sys_operation_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       DEFAULT NULL COMMENT '操作人ID',
    module      VARCHAR(64)  DEFAULT NULL COMMENT '模块名称',
    operation   VARCHAR(64)  DEFAULT NULL COMMENT '操作类型',
    method      VARCHAR(255) DEFAULT NULL COMMENT '请求方法',
    params      TEXT         DEFAULT NULL COMMENT '请求参数',
    ip          VARCHAR(64)  DEFAULT NULL COMMENT 'IP地址',
    status      TINYINT      DEFAULT 1 COMMENT '0-失败 1-成功',
    error_msg   TEXT         DEFAULT NULL,
    duration    BIGINT       DEFAULT NULL COMMENT '耗时（毫秒）',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='操作日志表';

-- 员工人脸特征（与 EmployeeFace extends BaseEntity 一致）
CREATE TABLE hr_employee_face (
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

-- ------------------------------------------------------------
-- 8. 初始化数据
-- ------------------------------------------------------------

-- 默认角色
INSERT INTO sys_role (id, role_name, role_code, description) VALUES
(1, '超级管理员', 'SUPER_ADMIN', '拥有全部权限'),
(2, 'HR管理员',   'HR_ADMIN',    '人事管理权限'),
(3, '部门经理',   'DEPT_MANAGER','部门管理权限'),
(4, '普通员工',   'EMPLOYEE',    '基础自助权限');

-- 默认假期类型
INSERT INTO hr_leave_type (id, type_name, type_code, max_days) VALUES
(1, '年假',   'ANNUAL',    10),
(2, '病假',   'SICK',      NULL),
(3, '事假',   'PERSONAL',  NULL),
(4, '婚假',   'MARRIAGE',  3),
(5, '产假',   'MATERNITY', 128);

-- 组织架构（parent_id：0=根，其余挂总公司 id=1）
INSERT INTO sys_department (id, parent_id, dept_name, dept_code, sort_order) VALUES
(1, 0, '总公司',     'ROOT', 0),
(2, 1, '技术部',     'TECH', 1),
(3, 1, '人力资源部', 'HR',   2),
(4, 1, '财务部',     'FIN',  3);

-- 岗位
INSERT INTO sys_position (id, position_name, position_code, dept_id, level) VALUES
(1, 'HR主管',     'HR_MGR',   3, 3),
(2, '技术经理',   'TECH_MGR', 2, 3),
(3, '开发工程师', 'DEV',      2, 1),
(4, '开发工程师', 'DEV_JR',   2, 1);

-- 员工（与四角色测试账号一一对应，另加 1 名同部门下属供经理审批演示）
INSERT INTO hr_employee (id, emp_no, name, gender, phone, email, dept_id, position_id, hire_date, status) VALUES
(1, 'HR001',  '张人事', 2, '13800000001', 'hr@example.com',       3, 1, '2024-01-15', 1),
(2, 'MGR001', '李经理', 1, '13800000002', 'manager@example.com',  2, 2, '2023-06-01', 1),
(3, 'EMP001', '王员工', 1, '13800000003', 'employee@example.com', 2, 3, '2025-03-10', 1),
(4, 'EMP002', '赵开发', 1, '13800000004', 'dev2@example.com',     2, 4, '2025-05-20', 1);

UPDATE sys_department SET leader_id = 1 WHERE id = 3;
UPDATE sys_department SET leader_id = 2 WHERE id = 2;

-- 测试账号（各账号独立密码，BCrypt 加密）
-- admin    / Admin@123  → 纯 SUPER_ADMIN，不绑定 employee_id
-- hr       / Hr@2024    → HR_ADMIN，绑定张人事
-- manager  / Mgr@2024   → DEPT_MANAGER，绑定李经理（技术部）
-- employee / Emp@2024   → EMPLOYEE，绑定王员工（技术部）
INSERT INTO sys_user (id, username, password, employee_id, status) VALUES
(1, 'admin',    '$2a$10$L8fR0Dl4uTcjZ8Q8V.33R.O2XQaflsOmnkJBfUWjpzXZ.kt8HcY8y', NULL, 1),
(2, 'hr',       '$2a$10$BC4jmJLwF1pCzhA9puK6buySp5Z3JdMbkorxNG2Gk1KzaCooH8n06', 1,    1),
(3, 'manager',  '$2a$10$UwsJ1gwUGS0UPxkxmMbS7uP3813zdB40j2eehxVOnyqMpeUyGVSaO', 2,    1),
(4, 'employee', '$2a$10$WSXiv68JniwFaDjW6CsfKenXbcOKVHBqDCOpgHAAKbmR7zRoHhYNm', 3,    1);

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 4);

-- 考勤样例（2026-06，供工作台月历与列表演示）
-- 王员工 EMP001：正常 / 迟到混合
INSERT INTO hr_attendance (employee_id, attend_date, check_in, check_out, status, work_hours) VALUES
(3, '2026-06-02', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-03', '09:22:00', '18:05:00', 2, 7.7),
(3, '2026-06-04', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-05', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-06', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-09', '09:18:00', '18:00:00', 2, 7.8),
(3, '2026-06-10', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-11', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-12', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-13', '09:35:00', '18:00:00', 2, 7.5),
(3, '2026-06-16', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-17', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-18', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-19', '09:00:00', '18:00:00', 1, 8.0),
(3, '2026-06-20', '09:10:00', '18:00:00', 2, 7.8),
(3, '2026-06-23', '09:00:00', '12:00:00', 1, 3.0);

-- 李经理 MGR001：少量记录
INSERT INTO hr_attendance (employee_id, attend_date, check_in, check_out, status, work_hours) VALUES
(2, '2026-06-23', '08:55:00', '18:30:00', 1, 8.5),
(2, '2026-06-20', '09:00:00', '18:00:00', 1, 8.0),
(2, '2026-06-19', '09:00:00', '18:00:00', 1, 8.0);

-- 请假样例
-- 待审批 ×2（经理登录后可审批；HR 可看全部）
INSERT INTO hr_leave_request (employee_id, leave_type_id, start_time, end_time, days, reason, status) VALUES
(3, 3, '2026-06-25 09:00:00', '2026-06-26 18:00:00', 2.0, '家中有事需处理', 0),
(4, 1, '2026-06-27 09:00:00', '2026-06-27 18:00:00', 1.0, '申请年假休息一天', 0);

-- 已处理记录（历史）
INSERT INTO hr_leave_request (employee_id, leave_type_id, start_time, end_time, days, reason, status, approver_id, approve_time, approve_remark) VALUES
(3, 2, '2026-05-10 09:00:00', '2026-05-10 18:00:00', 1.0, '感冒发烧', 1, 3, '2026-05-09 16:30:00', '注意休息'),
(4, 3, '2026-05-15 09:00:00', '2026-05-16 18:00:00', 2.0, '个人事务', 2, 3, '2026-05-14 10:00:00', '项目紧张，请改期');

-- 薪资样例（HR 工作台「待发薪资」统计）
INSERT INTO hr_salary (employee_id, salary_month, base_salary, bonus, deduction, actual_salary, status) VALUES
(2, '2026-06', 15000.00, 2000.00,  500.00, 16500.00, 0),
(3, '2026-06', 10000.00,  500.00,    0.00, 10500.00, 0),
(4, '2026-06',  9500.00,    0.00,    0.00,  9500.00, 0),
(2, '2026-05', 15000.00, 3000.00,  500.00, 17500.00, 1);

-- ============================================================
-- 四角色全流程测试清单（各账号密码不同，见下表）
-- ============================================================
-- | 账号     | 密码       | 角色          | 员工   | 验证要点 |
-- |----------|------------|---------------|--------|----------|
-- | admin    | Admin@123  | SUPER_ADMIN   | 无     | 可选员工代打卡；无个人月历 |
-- | hr       | Hr@2024    | HR_ADMIN      | 张人事 | 部门/薪资/考勤补录/请假全量 |
-- | manager  | Mgr@2024   | DEPT_MANAGER  | 李经理 | 技术部员工；审批待办 |
-- | employee | Emp@2024   | EMPLOYEE      | 王员工 | 自助打卡/请假/个人月历 |
-- ============================================================
