package com.ai.kust.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
//@Data,可以一次性完成所有四个注解
//统一封装返回值
@JsonInclude(JsonInclude.Include.NON_NULL) //序列化时省略空字段
public class Result<T> {
    private Integer code;
    private String message;
    private T data; //T表示泛型

    //成功返回时，返回空字符
    public static <T> Result<T> success() {
        return null;
    }

    //成功时返回数据
    public static <T> Result<T> success(T data) {
        Result<T>  r = new Result<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMessage(ResultCode.SUCCESS.getMessage());
        r.setData(data);
        return r;
    }

    //返回失败的返回值
    public static <T> Result<T> fail(ResultCode resultCode) {
        Result<T>  r = new Result<>();
        r.setCode(resultCode.getCode());
        r.setMessage(resultCode.getMessage());
        return r;
    }

    //动态显示错误信息
    public static <T> Result<T> fail(ResultCode resultCode, String detail) {
        Result<T>  r = new Result<>();
        r.setCode(resultCode.getCode());
        r.setMessage(detail);
        return r;
    }

    //生成的无参构造函数 == @NoArgsConstructor
    public Result() {
    }
    // == @AllArgsConstructor
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    //@Getter
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }
    //@Setter
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