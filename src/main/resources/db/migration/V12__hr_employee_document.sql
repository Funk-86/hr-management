-- 员工文档（合同 / 协议 / 薪资确认单等）
CREATE TABLE IF NOT EXISTS hr_employee_document (
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
    uploader_id   BIGINT        DEFAULT NULL COMMENT '上传人员工ID（纯超管可空）',
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
