package org.example.hrmanagement.common.exception;

import lombok.Getter;
import org.example.hrmanagement.common.result.ResultCode;

@Getter
public class BusinessException extends RuntimeException {
    private int code;

    public BusinessException(String message) {
        super(message);
        this.code= ResultCode.BAD_REQUEST.getCode();
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code= resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code= code;
    }
}
