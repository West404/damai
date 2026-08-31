package com.damai.common;

import com.damai.enums.BaseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @description: ApiResponse 单元测试
 **/
class ApiResponseTest {

    @Test
    @DisplayName("ok()：无参构造成功响应，code为0且无数据")
    void testOk() {
        ApiResponse<String> response = ApiResponse.ok();
        assertEquals(0, response.getCode());
        assertNull(response.getData());
        assertNull(response.getMessage());
    }

    @Test
    @DisplayName("ok(T)：带数据构造成功响应")
    void testOkWithData() {
        ApiResponse<String> response = ApiResponse.ok("成功数据");
        assertEquals(0, response.getCode());
        assertEquals("成功数据", response.getData());
    }

    @Test
    @DisplayName("error(code, message)：按指定code和message构造失败响应")
    void testErrorWithCodeAndMessage() {
        ApiResponse<String> response = ApiResponse.error(40001, "座位不存在");
        assertEquals(40001, response.getCode());
        assertEquals("座位不存在", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    @DisplayName("error(message)：默认code为-100")
    void testErrorWithMessage() {
        ApiResponse<String> response = ApiResponse.error("自定义错误");
        assertEquals(-100, response.getCode());
        assertEquals("自定义错误", response.getMessage());
    }

    @Test
    @DisplayName("error()：无参构造默认系统错误响应")
    void testErrorDefault() {
        ApiResponse<String> response = ApiResponse.error();
        assertEquals(-100, response.getCode());
        assertEquals("系统错误，请稍后重试!", response.getMessage());
    }

    @Test
    @DisplayName("error(BaseCode)：按基础code码枚举构造失败响应")
    void testErrorWithBaseCode() {
        ApiResponse<String> response = ApiResponse.error(BaseCode.SEAT_NOT_EXIST);
        assertEquals(BaseCode.SEAT_NOT_EXIST.getCode(), response.getCode());
        assertEquals(BaseCode.SEAT_NOT_EXIST.getMsg(), response.getMessage());
    }

    @Test
    @DisplayName("error(BaseCode, data)：携带数据的枚举失败响应")
    void testErrorWithBaseCodeAndData() {
        ApiResponse<String> response = ApiResponse.error(BaseCode.SYSTEM_ERROR, "详细堆栈");
        assertEquals(BaseCode.SYSTEM_ERROR.getCode(), response.getCode());
        assertEquals(BaseCode.SYSTEM_ERROR.getMsg(), response.getMessage());
        assertEquals("详细堆栈", response.getData());
    }
}
