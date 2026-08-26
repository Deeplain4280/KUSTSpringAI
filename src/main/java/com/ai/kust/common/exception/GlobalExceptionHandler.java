package com.ai.kust.common.exception;

import com.ai.kust.common.result.Result;
import com.ai.kust.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Objects;
import java.util.stream.Collectors;

/*
*  自定义异常拦截器，拦截全局异常
*  核心原理：
*       拦截 HTTP状态码 能够反应出传输层的语义， 自定义的 状态码 能够反映出 应用层的语义（ if rs.code === 200 {}）
* */
@Slf4j   // 日志打印
@RestControllerAdvice   // 前置处理（当前端 URL 请求发送请求到 后端时，先执行前置处理 @RestControllerAdvice ）
public class GlobalExceptionHandler {

    // TODO：业务异常处理
    /*
    *  获取 HTTP 状态码，ResultCode中的 HTTPStatus 提供
    * */
    @ExceptionHandler(BusinessException.class) // 获取自定义的异常拦截Java类
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

    /*
    * T
    * */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request){
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage) // 提示注解配置的默认 Message
                .filter(Objects::nonNull) // 过滤  null (值返回不为空的message)
                .collect(Collectors.joining("; "));
        log.warn("[{}] 参数校验失败：{}", request.getRequestURI(), msg);
        return ResponseEntity.badRequest().body(Result.fail(ResultCode.BAD_REQUEST, msg));
    }

    /*
    * 请求方法错误的拦截器 GET  --》 POST 请求
    * ODO : 拦截方法不允许的请求
    * */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public  ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request){
        log.warn("[{}] 不支持请求类型：{}" , request.getRequestURI(), e.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).
                body(Result.fail(ResultCode.METHOD_NOT_ALLOWED, ResultCode.METHOD_NOT_ALLOWED.getMessage()));
    }

    /*
    * 请求参数错误（userInput = "你好" & role = “”）
    * @RequestParam(required = true) 搭配一起使用  http://127.0.0.1:8080/api/chat/role?userInput=你是谁，我不喜欢你&
    * */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParam(MissingServletRequestParameterException e, HttpServletRequest request){
        log.warn("[{}] 缺失请求参数/错误：{}:(参数类型type={})", request.getRequestURI(), e.getParameterName(), e.getParameterType());
        return ResponseEntity.badRequest().body(Result.fail(ResultCode.BAD_REQUEST, "缺少请求参数:"+e.getParameterName()));
    }

    /*
    * 404 错误：资源未找到
    * */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFound(NoResourceFoundException e, HttpServletRequest request){
        log.warn("[{}] 资源未找到/请求路径: {}", request.getRequestURI(), e.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.fail(ResultCode.NOT_FOUND));
    }

    /*
     * 兜底异常
     * */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexcepted(Exception e, HttpServletRequest request){
        log.error("[{}] 系统未预期异常：{}", request.getRequestURI(), e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ResultCode.INTERNAL_ERROR));
    }

}
