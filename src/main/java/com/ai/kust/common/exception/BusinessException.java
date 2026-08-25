package com.ai.kust.common.exception;

import com.ai.kust.common.result.ResultCode;
import lombok.Getter;

//自定义全局异常处理
@Getter
public class BusinessException extends RuntimeException {
    //注入Result的code状态码
    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode, String detail, Throwable cause) {
        super(detail, cause);
        this.resultCode = resultCode;
    }

    //直接使用枚举类中的错误信息
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    //动态覆盖ResultCode的默认值
    public BusinessException(ResultCode resultCode, String detail) {
        super(detail);
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.resultCode = resultCode;
    }

}
