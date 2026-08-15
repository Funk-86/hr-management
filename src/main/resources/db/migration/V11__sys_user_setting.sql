-- 用户安全与消息提醒偏好
CREATE TABLE IF NOT EXISTS sys_user_setting (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    user_id               BIGINT       NOT NULL COMMENT 'sys_user.id',
    notify_account        TINYINT      NOT NULL DEFAULT 1 COMMENT '账户类消息提醒 1开 0关',
    notify_system         TINYINT      NOT NULL DEFAULT 1 COMMENT '系统消息提醒',
    notify_todo           TINYINT      NOT NULL DEFAULT 1 COMMENT '待办任务消息提醒',
    phone_secured         TINYINT      NOT NULL DEFAULT 0 COMMENT '启用密保手机',
    email_secured         TINYINT      NOT NULL DEFAULT 0 COMMENT '启用备用邮箱',
    security_question     VARCHAR(128) DEFAULT NULL COMMENT '密保问题',
    security_answer_hash  VARCHAR(128) DEFAULT NULL COMMENT '密保答案 BCrypt',
    mfa_enabled           TINYINT      NOT NULL DEFAULT 0 COMMENT '是否启用 MFA',
    mfa_secret            VARCHAR(64)  DEFAULT NULL COMMENT 'TOTP 密钥（启用后保存）',
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            BIGINT       DEFAULT NULL,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by            BIGINT       DEFAULT NULL,
    deleted               TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB COMMENT='用户安全与消息偏好';
