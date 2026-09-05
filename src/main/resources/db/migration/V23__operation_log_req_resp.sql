ALTER TABLE sys_operation_log
    ADD COLUMN request_info  MEDIUMTEXT NULL COMMENT 'request JSON sanitized' AFTER params,
    ADD COLUMN response_info MEDIUMTEXT NULL COMMENT 'response JSON sanitized' AFTER request_info;
