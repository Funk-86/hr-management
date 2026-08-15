-- 岗位底薪字典
CREATE TABLE IF NOT EXISTS hr_salary_base_dict (
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

-- 任务评分奖金字典（五级）
CREATE TABLE IF NOT EXISTS hr_task_score_bonus_dict (
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

INSERT INTO hr_task_score_bonus_dict (grade, grade_label, bonus_amount, status)
VALUES
    (1, '优', 500.00, 1),
    (2, '良', 300.00, 1),
    (3, '中', 150.00, 1),
    (4, '合格', 50.00, 1),
    (5, '差', 0.00, 1)
ON DUPLICATE KEY UPDATE grade_label = VALUES(grade_label);

-- 任务执行人评分字段
ALTER TABLE hr_task_assignee
    ADD COLUMN score_grade TINYINT DEFAULT NULL COMMENT '评分等级 1优~5差' AFTER finish_time,
    ADD COLUMN score_bonus DECIMAL(10,2) DEFAULT NULL COMMENT '评分奖金' AFTER score_grade,
    ADD COLUMN scored_by BIGINT DEFAULT NULL COMMENT '评分人员工ID' AFTER score_bonus,
    ADD COLUMN scored_at DATETIME DEFAULT NULL COMMENT '评分时间' AFTER scored_by;

-- 月薪快照字段
ALTER TABLE hr_salary
    ADD COLUMN position_id BIGINT DEFAULT NULL COMMENT '岗位快照' AFTER salary_month,
    ADD COLUMN task_bonus DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '任务评分奖金汇总' AFTER base_salary;
