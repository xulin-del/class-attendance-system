package com.attendance.common;

public class Result<T> {
    private boolean success;
    private String message;
    private T data;
    
    public Result() {}
    
    public Result(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public Result(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
    
    public static <T> Result<T> success(String message) {
        return new Result<>(true, message);
    }
    
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(true, message, data);
    }
    
    public static <T> Result<T> error(String message) {
        return new Result<>(false, message);
    }

    public static <T> Result<T> error(String message, T data) {
        return new Result<>(false, message, data);
    }
    
    public boolean isSuccess() {
        return success;
    }
    public void setSuccess(boolean success) {
        this.success = success;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public T getData() {
        return data;
    }
    public void setData(T data) {
        this.data = data;
    }
}