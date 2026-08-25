package com.ai.kust.common.exception;
//自定义异常拦截器，拦截全局的异常

import com.ai.kust.common.result.Result;
import com.ai.kust.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j //日志打印
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handlerBusiness(BusinessException e, HttpServletRequest request){
        // HttpServletRequest ： 前端发送的请求信息
        // 获取请求的状态码 HTTPStatus 提供的
        ResultCode resultCode = e.getResultCode();
        // request.getRequestURI() URL 请求地址（127.0.01:8080/api/chat）
        log.warn("[{}] 业务异常 ｜ code = {} | msg = {} ", request.getRequestURI(), resultCode.getCode(), e.getMessage());
        Result<Void> body = Result.fail(resultCode, e.getMessage());
        HttpStatus httpStatus = HttpStatus.resolve(resultCode.getHTTPStatus());
        if (httpStatus == null){
            log.error("ResponseCode {} 配置无效 httpStatus：{} ", resultCode.name(), resultCode.getHTTPStatus());
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(httpStatus).body(body);
    }
}

