-- 站内消息/通知
CREATE TABLE IF NOT EXISTS sys_notification (
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
