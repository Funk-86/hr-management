-- 头像 OSS 字段迁移（已有库执行一次即可）
USE hr_management;

ALTER TABLE hr_employee
    MODIFY COLUMN avatar VARCHAR(512) DEFAULT NULL
    COMMENT '头像 OSS 对象 Key，如 avatar/employee/3/20260624_abc123.jpg';

ALTER TABLE sys_user
    ADD COLUMN avatar VARCHAR(512) DEFAULT NULL
    COMMENT '头像 OSS 对象 Key（无关联员工时使用，如 admin）'
    AFTER employee_id;
