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
    INTERNAL_ERROR(500, "内部系统错误", 500);


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
