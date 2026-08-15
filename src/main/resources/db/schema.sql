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
    base_salary     DECIMAL(10,2) DEFAULT NULL COMMENT '个人底薪（调薪生效后覆盖岗位字典）',
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
    perm_name   VARCHAR(128) NOT NULL COMMENT '权限名称',
    perm_code   VARCHAR(128) NOT NULL COMMENT '权限编码',
    perm_type   TINYINT      NOT NULL COMMENT '类型：1-菜单 2-按钮 3-接口',
    path        VARCHAR(255) DEFAULT NULL COMMENT '路由/接口路径',
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
    position_id   BIGINT        DEFAULT NULL COMMENT '岗位快照',
    base_salary   DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '基本工资',
    task_bonus    DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '任务评分奖金汇总',
    bonus         DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '最终奖金',
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

CREATE TABLE hr_salary_base_dict (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    position_id   BIGINT        NOT NULL COMMENT '岗位ID',
    base_salary   DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '基本工资',
    status        TINYINT       NOT NULL DEFAULT 1 COMMENT '1-启用 0-停用',
    remark        VARCHAR(255)  DEFAULT NULL,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT        DEFAULT NULL,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by    BIGINT        DEFAULT NULL,
    deleted       TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_position (position_id)
) ENGINE=InnoDB COMMENT='岗位底薪字典';

CREATE TABLE hr_task_score_bonus_dict (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    grade         TINYINT       NOT NULL COMMENT '1优 2良 3中 4合格 5差',
    grade_label   VARCHAR(16)   NOT NULL COMMENT '等级名称',
    bonus_amount  DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '奖金金额',
    status        TINYINT       NOT NULL DEFAULT 1 COMMENT '1-启用 0-停用',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT        DEFAULT NULL,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by    BIGINT        DEFAULT NULL,
    deleted       TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_grade (grade)
) ENGINE=InnoDB COMMENT='任务评分奖金字典';

INSERT INTO hr_task_score_bonus_dict (grade, grade_label, bonus_amount, status) VALUES
    (1, '优', 500.00, 1),
    (2, '良', 300.00, 1),
    (3, '中', 150.00, 1),
    (4, '合格', 50.00, 1),
    (5, '差', 0.00, 1);

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

-- ------------------------------------------------------------
-- 7.1 任务管理
-- ------------------------------------------------------------

CREATE TABLE hr_project (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    name            VARCHAR(128)  NOT NULL COMMENT '项目名称',
    description     VARCHAR(1000) DEFAULT NULL COMMENT '项目说明',
    owner_id        BIGINT        NOT NULL COMMENT '负责人（员工ID）',
    dept_id         BIGINT        DEFAULT NULL COMMENT '归属部门',
    status          TINYINT       NOT NULL DEFAULT 1 COMMENT '0规划 1进行中 2已完成 3已关闭',
    progress        TINYINT       NOT NULL DEFAULT 0 COMMENT '进度 0-100',
    progress_locked TINYINT       NOT NULL DEFAULT 0 COMMENT '1=已锁定/手调进度',
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

CREATE TABLE hr_project_member (
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

CREATE TABLE hr_task (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    title         VARCHAR(128)  NOT NULL COMMENT '任务标题',
    content       VARCHAR(2000) DEFAULT NULL COMMENT '任务说明',
    parent_id     BIGINT        NOT NULL DEFAULT 0 COMMENT '父任务ID，0表示根任务',
    project_id    BIGINT        DEFAULT NULL COMMENT '所属项目ID',
    creator_id    BIGINT        NOT NULL COMMENT '创建人（员工ID）',
    dept_id       BIGINT        DEFAULT NULL COMMENT '归属部门ID',
    priority      TINYINT       NOT NULL DEFAULT 2 COMMENT '优先级：1-低 2-中 3-高',
    status        TINYINT       NOT NULL DEFAULT 0 COMMENT '整体状态：0-待接收 1-进行中 2-已完成 3-已关闭',
    start_time    DATETIME      DEFAULT NULL COMMENT '开始时间',
    due_time      DATETIME      DEFAULT NULL COMMENT '截止时间',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT        DEFAULT NULL COMMENT '创建人用户ID',
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by    BIGINT        DEFAULT NULL COMMENT '更新人用户ID',
    deleted       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id),
    KEY idx_project_id (project_id),
    KEY idx_creator_id (creator_id),
    KEY idx_dept_status (dept_id, status),
    KEY idx_due_time (due_time)
) ENGINE=InnoDB COMMENT='任务主表';

CREATE TABLE hr_task_assignee (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_id        BIGINT        NOT NULL COMMENT '任务ID',
    employee_id    BIGINT        NOT NULL COMMENT '执行人（员工ID）',
    status         TINYINT       NOT NULL DEFAULT 0 COMMENT '0-待接收 1-进行中 2-已完成 3-已驳回 4-已关闭',
    progress       TINYINT       NOT NULL DEFAULT 0 COMMENT '进度 0-100',
    feedback       VARCHAR(1000) DEFAULT NULL COMMENT '进度/完成说明',
    reject_reason  VARCHAR(500)  DEFAULT NULL COMMENT '驳回原因',
    accept_time    DATETIME      DEFAULT NULL COMMENT '接收时间',
    finish_time    DATETIME      DEFAULT NULL COMMENT '完成时间',
    score_grade    TINYINT       DEFAULT NULL COMMENT '评分等级 1优~5差',
    score_bonus    DECIMAL(10,2) DEFAULT NULL COMMENT '评分奖金',
    scored_by      BIGINT        DEFAULT NULL COMMENT '评分人员工ID',
    scored_at      DATETIME      DEFAULT NULL COMMENT '评分时间',
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     BIGINT        DEFAULT NULL COMMENT '创建人用户ID',
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by     BIGINT        DEFAULT NULL COMMENT '更新人用户ID',
    deleted        TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_emp (task_id, employee_id),
    KEY idx_employee_status (employee_id, status),
    KEY idx_task_id (task_id)
) ENGINE=InnoDB COMMENT='任务执行人表';

CREATE TABLE hr_task_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_id       BIGINT       NOT NULL COMMENT '任务ID',
    operator_id   BIGINT       NOT NULL COMMENT '操作人（员工ID）',
    action        VARCHAR(32)  NOT NULL COMMENT 'CREATE/ACCEPT/PROGRESS/FINISH/REJECT/URGE/CLOSE',
    remark        VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_task_id (task_id),
    KEY idx_operator_id (operator_id)
) ENGINE=InnoDB COMMENT='任务操作日志表';

CREATE TABLE hr_task_attachment (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_id       BIGINT        NOT NULL COMMENT '任务ID',
    object_key    VARCHAR(512)  NOT NULL COMMENT 'OSS 对象 Key',
    file_name     VARCHAR(255)  NOT NULL COMMENT '原始文件名',
    content_type  VARCHAR(128)  DEFAULT NULL COMMENT 'MIME 类型',
    file_size     BIGINT        NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    uploader_id   BIGINT        NOT NULL COMMENT '上传人（员工ID）',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT        DEFAULT NULL,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by    BIGINT        DEFAULT NULL,
    deleted       TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_task_id (task_id)
) ENGINE=InnoDB COMMENT='任务附件表';

CREATE TABLE hr_employee_document (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    employee_id   BIGINT        NOT NULL COMMENT '所属员工',
    doc_type      TINYINT       NOT NULL DEFAULT 4 COMMENT '1劳动合同 2保密协议 3薪资确认单 4其他',
    title         VARCHAR(128)  DEFAULT NULL COMMENT '文档标题',
    object_key    VARCHAR(512)  NOT NULL COMMENT 'OSS 对象 Key',
    file_name     VARCHAR(255)  NOT NULL COMMENT '原始文件名',
    content_type  VARCHAR(128)  DEFAULT NULL COMMENT 'MIME 类型',
    file_size     BIGINT        NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    effective_date DATE         DEFAULT NULL COMMENT '生效日期',
    expire_date   DATE          DEFAULT NULL COMMENT '到期日期',
    remark        VARCHAR(255)  DEFAULT NULL COMMENT '备注',
    uploader_id   BIGINT        DEFAULT NULL COMMENT '上传人员工ID',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT        DEFAULT NULL,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by    BIGINT        DEFAULT NULL,
    deleted       TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_employee_id (employee_id),
    KEY idx_doc_type (doc_type),
    KEY idx_expire_date (expire_date)
) ENGINE=InnoDB COMMENT='员工文档表';

CREATE TABLE hr_performance_review (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    employee_id     BIGINT        NOT NULL COMMENT '被考核员工',
    period_type     TINYINT       NOT NULL COMMENT '1月度 2季度',
    period_key      VARCHAR(16)   NOT NULL COMMENT '如 2026-08 / 2026-Q3',
    score_grade     TINYINT       DEFAULT NULL COMMENT '1优 2良 3中 4合格 5差',
    comment_text    VARCHAR(1000) DEFAULT NULL COMMENT '评语',
    task_done_count INT           DEFAULT NULL COMMENT '周期内完成任务数快照',
    task_total_count INT          DEFAULT NULL COMMENT '周期内参与任务数快照',
    task_avg_grade  DECIMAL(4,2)  DEFAULT NULL COMMENT '周期内任务评分均分快照',
    reviewer_id     BIGINT        NOT NULL COMMENT '评分人（员工ID）',
    status          TINYINT       NOT NULL DEFAULT 0 COMMENT '0草稿 1已提交 2已确认',
    confirmed_at    DATETIME      DEFAULT NULL,
    confirmed_by    BIGINT        DEFAULT NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT        DEFAULT NULL,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by      BIGINT        DEFAULT NULL,
    deleted         TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_emp_period (employee_id, period_type, period_key),
    KEY idx_period (period_type, period_key),
    KEY idx_reviewer (reviewer_id),
    KEY idx_status (status)
) ENGINE=InnoDB COMMENT='绩效考核单';

CREATE TABLE hr_personnel_change (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    change_type       TINYINT        NOT NULL COMMENT '1调岗 2调薪 3离职 4入职完善',
    employee_id       BIGINT         NOT NULL,
    from_dept_id      BIGINT         DEFAULT NULL,
    to_dept_id        BIGINT         DEFAULT NULL,
    from_position_id  BIGINT         DEFAULT NULL,
    to_position_id    BIGINT         DEFAULT NULL,
    old_salary        DECIMAL(10,2)  DEFAULT NULL,
    new_salary        DECIMAL(10,2)  DEFAULT NULL,
    effective_date    DATE           DEFAULT NULL,
    reason            VARCHAR(500)   DEFAULT NULL,
    status            TINYINT        NOT NULL DEFAULT 0 COMMENT '0待审批 1已通过 2已拒绝 3已撤销 4已生效',
    applicant_id      BIGINT         NOT NULL,
    approver_id       BIGINT         DEFAULT NULL,
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

-- ------------------------------------------------------------
-- 7.2 站内消息
-- ------------------------------------------------------------

CREATE TABLE sys_user_setting (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    user_id               BIGINT       NOT NULL COMMENT 'sys_user.id',
    notify_account        TINYINT      NOT NULL DEFAULT 1 COMMENT '账户类消息提醒',
    notify_system         TINYINT      NOT NULL DEFAULT 1 COMMENT '系统消息提醒',
    notify_todo           TINYINT      NOT NULL DEFAULT 1 COMMENT '待办任务消息提醒',
    phone_secured         TINYINT      NOT NULL DEFAULT 0 COMMENT '启用密保手机',
    email_secured         TINYINT      NOT NULL DEFAULT 0 COMMENT '启用备用邮箱',
    security_question     VARCHAR(128) DEFAULT NULL COMMENT '密保问题',
    security_answer_hash  VARCHAR(128) DEFAULT NULL COMMENT '密保答案',
    mfa_enabled           TINYINT      NOT NULL DEFAULT 0 COMMENT '是否启用 MFA',
    mfa_secret            VARCHAR(64)  DEFAULT NULL COMMENT 'TOTP 密钥',
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            BIGINT       DEFAULT NULL,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by            BIGINT       DEFAULT NULL,
    deleted               TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB COMMENT='用户安全与消息偏好';

CREATE TABLE sys_notification (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id      BIGINT        NOT NULL COMMENT '接收人 sys_user.id',
    title        VARCHAR(128)  NOT NULL COMMENT '标题',
    content      VARCHAR(500)  DEFAULT NULL COMMENT '内容摘要',
    biz_type     VARCHAR(32)   NOT NULL COMMENT '业务类型：TASK_ASSIGN/TASK_URGE/TASK_REJECT',
    biz_id       BIGINT        DEFAULT NULL COMMENT '业务ID（任务ID）',
    link         VARCHAR(255)  DEFAULT '/hr/task' COMMENT '前端跳转路径',
    is_read      TINYINT       NOT NULL DEFAULT 0 COMMENT '0-未读 1-已读',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   BIGINT        DEFAULT NULL,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by   BIGINT        DEFAULT NULL,
    deleted      TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_read_time (user_id, is_read, created_at)
) ENGINE=InnoDB COMMENT='站内消息表';

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
(4, 1, '财务部',     'FIN',  3),
(5, 1, '产品部',     'PROD', 4),
(6, 1, '市场部',     'MKT',  5);

-- 岗位
INSERT INTO sys_position (id, position_name, position_code, dept_id, level) VALUES
(1,  'HR主管',         'HR_MGR',   3, 3),
(2,  '技术经理',       'TECH_MGR', 2, 3),
(3,  '高级开发工程师', 'DEV',      2, 1),
(4,  '初级开发工程师', 'DEV_JR',   2, 1),
(5,  '财务主管',       'FIN_MGR',  4, 3),
(6,  '会计',           'ACCNT',    4, 1),
(7,  '产品经理',       'PM',       5, 3),
(8,  '产品助理',       'PM_ASST',  5, 1),
(9,  '市场经理',       'MKT_MGR',  6, 3),
(10, 'HR专员',         'HR_SPEC',  3, 1),
(11, '测试工程师',     'QA',       2, 1);

-- 岗位底薪字典
INSERT INTO hr_salary_base_dict (position_id, base_salary, status, remark) VALUES
(1,  12000.00, 1, 'HR主管标准底薪'),
(2,  18000.00, 1, '技术经理标准底薪'),
(3,  14000.00, 1, '高级开发工程师'),
(4,   9000.00, 1, '初级开发工程师'),
(5,  15000.00, 1, '财务主管'),
(6,   8500.00, 1, '会计'),
(7,  16000.00, 1, '产品经理'),
(8,   8000.00, 1, '产品助理'),
(9,  15000.00, 1, '市场经理'),
(10,  7500.00, 1, 'HR专员'),
(11, 10000.00, 1, '测试工程师');

-- 员工（核心四角色 + 多部门真实花名册）
INSERT INTO hr_employee (id, emp_no, name, gender, phone, email, dept_id, position_id, hire_date, status, remark) VALUES
(1,  'HR001',  '张人事', 2, '13800000001', 'hr@example.com',         3, 1,  '2024-01-15', 1, NULL),
(2,  'MGR001', '李经理', 1, '13800000002', 'manager@example.com',    2, 2,  '2023-06-01', 1, NULL),
(3,  'EMP001', '王员工', 1, '13800000003', 'employee@example.com',   2, 3,  '2025-03-10', 1, NULL),
(4,  'EMP002', '赵开发', 1, '13800000004', 'dev2@example.com',       2, 4,  '2025-05-20', 1, NULL),
(5,  'FIN001', '钱会计', 2, '13800000005', 'accountant@example.com', 4, 6,  '2024-03-01', 1, NULL),
(6,  'PM001',  '孙产品', 1, '13800000006', 'pm@example.com',         5, 7,  '2023-09-12', 1, NULL),
(7,  'MKT001', '周市场', 2, '13800000007', 'mkt@example.com',        6, 9,  '2023-11-08', 1, NULL),
(8,  'HR002',  '吴专员', 2, '13800000008', 'hr2@example.com',        3, 10, '2025-01-06', 1, NULL),
(9,  'EMP003', '郑开发', 1, '13800000009', 'dev3@example.com',       2, 3,  '2024-08-15', 1, NULL),
(10, 'FIN002', '冯财务', 1, '13800000010', 'finance@example.com',    4, 5,  '2022-05-20', 1, NULL),
(11, 'QA001',  '陈测试', 2, '13800000011', 'qa@example.com',         2, 11, '2025-02-18', 2, '试用期'),
(12, 'PM002',  '褚助理', 1, '13800000012', 'pmasst@example.com',     5, 8,  '2025-04-01', 1, NULL);

UPDATE sys_department SET leader_id = 1  WHERE id = 3;
UPDATE sys_department SET leader_id = 2  WHERE id = 2;
UPDATE sys_department SET leader_id = 10 WHERE id = 4;
UPDATE sys_department SET leader_id = 6  WHERE id = 5;
UPDATE sys_department SET leader_id = 7  WHERE id = 6;

-- 测试账号（各账号独立密码，BCrypt 加密）
-- admin    / Admin@123  → 纯 SUPER_ADMIN，不绑定 employee_id
-- hr       / Hr@2024    → HR_ADMIN，绑定张人事
-- manager  / Mgr@2024   → DEPT_MANAGER，绑定李经理（技术部）
-- employee / Emp@2024   → EMPLOYEE，绑定王员工（技术部）
INSERT INTO sys_user (id, username, password, employee_id, status) VALUES
(1,  'admin',      '$2a$10$L8fR0Dl4uTcjZ8Q8V.33R.O2XQaflsOmnkJBfUWjpzXZ.kt8HcY8y', NULL, 1),
(2,  'hr',         '$2a$10$BC4jmJLwF1pCzhA9puK6buySp5Z3JdMbkorxNG2Gk1KzaCooH8n06', 1,    1),
(3,  'manager',    '$2a$10$UwsJ1gwUGS0UPxkxmMbS7uP3813zdB40j2eehxVOnyqMpeUyGVSaO', 2,    1),
(4,  'employee',   '$2a$10$WSXiv68JniwFaDjW6CsfKenXbcOKVHBqDCOpgHAAKbmR7zRoHhYNm', 3,    1),
(5,  '赵开发', '$2a$10$WSXiv68JniwFaDjW6CsfKenXbcOKVHBqDCOpgHAAKbmR7zRoHhYNm', 4,    1),
(6,  '钱会计', '$2a$10$WSXiv68JniwFaDjW6CsfKenXbcOKVHBqDCOpgHAAKbmR7zRoHhYNm', 5,    1),
(7,  '孙产品', '$2a$10$UwsJ1gwUGS0UPxkxmMbS7uP3813zdB40j2eehxVOnyqMpeUyGVSaO', 6,    1),
(8,  '周市场', '$2a$10$UwsJ1gwUGS0UPxkxmMbS7uP3813zdB40j2eehxVOnyqMpeUyGVSaO', 7,    1),
(9,  '吴专员', '$2a$10$WSXiv68JniwFaDjW6CsfKenXbcOKVHBqDCOpgHAAKbmR7zRoHhYNm', 8,    1),
(10, '郑开发', '$2a$10$WSXiv68JniwFaDjW6CsfKenXbcOKVHBqDCOpgHAAKbmR7zRoHhYNm', 9,    1),
(11, '冯财务', '$2a$10$UwsJ1gwUGS0UPxkxmMbS7uP3813zdB40j2eehxVOnyqMpeUyGVSaO', 10,   1),
(12, '陈测试', '$2a$10$WSXiv68JniwFaDjW6CsfKenXbcOKVHBqDCOpgHAAKbmR7zRoHhYNm', 11,   1),
(13, '褚助理', '$2a$10$WSXiv68JniwFaDjW6CsfKenXbcOKVHBqDCOpgHAAKbmR7zRoHhYNm', 12,   1);

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 4),
(5, 4),
(6, 4),
(7, 3),
(8, 3),
(9, 4),
(10, 4),
(11, 3),
(12, 4),
(13, 4);

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

-- 薪资样例（含岗位快照 / 任务奖金）
INSERT INTO hr_salary (employee_id, salary_month, position_id, base_salary, task_bonus, bonus, deduction, actual_salary, status, pay_date, remark) VALUES
(2,  '2026-06', 2, 18000.00, 0.00, 2000.00, 500.00, 19500.00, 0, NULL, NULL),
(3,  '2026-06', 3, 14000.00, 0.00,  500.00,   0.00, 14500.00, 0, NULL, NULL),
(4,  '2026-06', 4,  9000.00, 0.00,    0.00,   0.00,  9000.00, 0, NULL, NULL),
(2,  '2026-05', 2, 18000.00, 0.00, 3000.00, 500.00, 20500.00, 1, NULL, NULL),
(1,  '2026-07', 1, 12000.00, 300.00, 300.00, 0.00, 12300.00, 1, '2026-08-05', '含任务评分奖金'),
(5,  '2026-07', 6,  8500.00, 0.00, 0.00, 0.00, 8500.00, 1, '2026-08-05', NULL),
(6,  '2026-07', 7, 16000.00, 500.00, 500.00, 200.00, 16300.00, 1, '2026-08-05', NULL),
(10, '2026-07', 5, 15000.00, 0.00, 800.00, 0.00, 15800.00, 1, '2026-08-05', '季度津贴计入奖金'),
(1,  '2026-08', 1, 12000.00, 0.00, 0.00, 0.00, 12000.00, 0, NULL, '待汇总任务奖金后发放'),
(5,  '2026-08', 6,  8500.00, 0.00, 0.00, 0.00, 8500.00, 0, NULL, NULL),
(10, '2026-08', 5, 15000.00, 0.00, 0.00, 0.00, 15000.00, 0, NULL, NULL);

-- 任务与评分样例（供 2026-08 月薪任务奖金预览）
INSERT INTO hr_task (id, title, content, parent_id, creator_id, dept_id, priority, status, start_time, due_time) VALUES
(1, '完成用户权限联调', '联调登录角色切换、菜单权限与数据权限，输出自测清单。', 0, 2, 2, 3, 2, '2026-08-01 09:00:00', '2026-08-08 18:00:00'),
(2, '修复考勤导出字段错位', '导出 Excel 时工号与姓名列错位，需修复并补回归用例。', 0, 2, 2, 2, 2, '2026-08-04 10:00:00', '2026-08-07 18:00:00'),
(3, '整理 8 月入职材料归档', '核对新员工劳动合同、身份证复印件并上传档案库。', 0, 1, 3, 2, 2, '2026-08-02 09:30:00', '2026-08-06 17:30:00'),
(4, '产品需求评审纪要输出', '整理本周需求评审结论，同步技术与市场。', 0, 6, 5, 2, 1, '2026-08-05 14:00:00', '2026-08-12 18:00:00'),
(5, '八月营销活动物料准备', '制作活动海报初稿与投放素材清单。', 0, 7, 6, 2, 0, '2026-08-08 09:00:00', '2026-08-15 18:00:00');

INSERT INTO hr_task_assignee (task_id, employee_id, status, progress, feedback, accept_time, finish_time, score_grade, score_bonus, scored_by, scored_at) VALUES
(1, 3,  2, 100, '权限联调完成，自测清单已提交', '2026-08-01 10:00:00', '2026-08-06 16:20:00', 1, 500.00, 2, '2026-08-06 17:00:00'),
(1, 4,  2, 100, '配合联调并修复两处菜单问题', '2026-08-01 10:05:00', '2026-08-07 11:30:00', 2, 300.00, 2, '2026-08-07 14:00:00'),
(2, 9,  2, 100, '导出字段已对齐，补充单测', '2026-08-04 11:00:00', '2026-08-06 15:40:00', 2, 300.00, 2, '2026-08-06 16:10:00'),
(2, 11, 2, 100, '补充导出回归用例并通过', '2026-08-04 11:10:00', '2026-08-07 09:50:00', 3, 150.00, 2, '2026-08-07 10:20:00'),
(3, 8,  2, 100, '入职材料已归档', '2026-08-02 10:00:00', '2026-08-05 17:10:00', 2, 300.00, 1, '2026-08-05 17:30:00'),
(4, 12, 1, 60,  '纪要草稿已完成 60%', '2026-08-05 15:00:00', NULL, NULL, NULL, NULL, NULL),
(5, 7,  0, 0,   NULL, NULL, NULL, NULL, NULL, NULL, NULL);

-- ============================================================
-- 账号清单（密码约定：Mgr@2024 / Emp@2024 / 原四角色见下表）
-- ============================================================
-- | 登录用户名（可用中文真实姓名） | 密码       | 角色         |
-- |-------------------------------|------------|--------------|
-- | admin                         | Admin@123  | SUPER_ADMIN  |
-- | 张人事 / 李经理 / 王员工      | 见原四角色 | HR/经理/员工 |
-- | 赵开发/钱会计/吴专员/郑开发/陈测试/褚助理 | Emp@2024 | EMPLOYEE |
-- | 孙产品/周市场/冯财务          | Mgr@2024   | DEPT_MANAGER |
-- ============================================================
