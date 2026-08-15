CREATE TABLE IF NOT EXISTS hr_task_attachment (
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
