package com.baseproject.config.common;

public class ApiResponse<T> {

    private int code;
    private String message;
    private String requestId;
    private T data;

    public static <T> ApiResponse<T> success(T data, String requestId) {
        ApiResponse<T> response = new ApiResponse<T>();
        response.setCode(0);
        response.setMessage("success");
        response.setRequestId(requestId);
        response.setData(data);
        return response;
    }

    public static <T> ApiResponse<T> failure(int code, String message, String requestId) {
        ApiResponse<T> response = new ApiResponse<T>();
        response.setCode(code);
        response.setMessage(message);
        response.setRequestId(requestId);
        response.setData(null);
        return response;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

