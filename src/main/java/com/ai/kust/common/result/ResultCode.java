package com.ai.kust.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

//统一状态码，AI相关使用1开头
@Getter
@AllArgsConstructor
public enum ResultCode {
    SUCCESS(200, "操作成功", 200),
    CREATED(201, "创建成功", 201),
    BAD_REQUEST(400, "请求参数错误",  400),
    NOT_FOUND(404, "请求资源不存在", 404),
    METHOD_NOT_ALLOWED(405, "方法不允许",405),
    INTERNAL_ERROR(500, "内部系统错误", 500),

    AI_CALL_FAILED(1001, "AI服务器繁忙，请稍后重试", 502),
    CHAT_CONVERSATION_NOT_FOUND(1002, "对话不存在", 404),

    //登录相关
    VERIFY_CODE_EXPIRED(40101, "验证码已过期，请重新获取", 401),
    VERIFY_CODE_ERROR(40201, "验证码输入错误，请重新输入", 402),
    VERIFY_CODE_SUCCESS(20001, "正在登录", 200);

    private final int code;
    private final  String message;
    private final int HTTPStatus;

    public static ResultCode formCode(int code) {
        for (ResultCode rc : values()) {
            if (code == rc.HTTPStatus) {
                return rc;
            }
        }
        return null;
    }
}
