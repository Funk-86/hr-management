package org.example.hrmanagement.common.result;

import lombok.Data;

import java.io.Serializable;

@Data
public class Result <T> implements Serializable {
    private int code;
    private String message;
    private T data;
    private long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public static<T> Result<T> success(T data) {
        Result<T> result = new Result<T>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    public static<T> Result<T> success() {
        return success(null);
    }

    public static<T> Result<T> success(String message,T data) {
        Result<T> result = new Result<T>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    public static<T> Result<T> fail(ResultCode resultCode) {
        Result<T> result = new Result<T>();
        result.setCode(resultCode.getCode());
        result.setMessage(resultCode.getMessage());
        return result;
    }

    public static<T> Result<T> fail(int code,String message) {
        Result<T> result = new Result<T>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }

}
