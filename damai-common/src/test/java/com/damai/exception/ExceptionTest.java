package com.damai.exception;

import com.damai.common.ApiResponse;
import com.damai.enums.BaseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @description: 异常体系单元测试
 **/
class ExceptionTest {

    @Test
    @DisplayName("DaMaiFrameException(BaseCode)：code和message取自枚举")
    void testDaMaiFrameExceptionWithBaseCode() {
        DaMaiFrameException exception = new DaMaiFrameException(BaseCode.ORDER_NOT_EXIST);
        assertEquals(BaseCode.ORDER_NOT_EXIST.getCode(), exception.getCode());
        assertEquals(BaseCode.ORDER_NOT_EXIST.getMsg(), exception.getMessage());
    }

    @Test
    @DisplayName("DaMaiFrameException(code, message)：自定义code和message")
    void testDaMaiFrameExceptionWithCodeAndMessage() {
        DaMaiFrameException exception = new DaMaiFrameException(12345, "自定义异常");
        assertEquals(12345, exception.getCode());
        assertEquals("自定义异常", exception.getMessage());
    }

    @Test
    @DisplayName("DaMaiFrameException(String code, String message)：字符串code被解析为整数")
    void testDaMaiFrameExceptionWithStringCode() {
        DaMaiFrameException exception = new DaMaiFrameException("40015", "订单不存在");
        assertEquals(40015, exception.getCode());
        assertEquals("订单不存在", exception.getMessage());
    }

    @Test
    @DisplayName("DaMaiFrameException(ApiResponse)：code和message取自响应体")
    void testDaMaiFrameExceptionWithApiResponse() {
        ApiResponse<String> apiResponse = ApiResponse.error(40016, "订单已取消");
        DaMaiFrameException exception = new DaMaiFrameException(apiResponse);
        assertEquals(40016, exception.getCode());
        assertEquals("订单已取消", exception.getMessage());
    }

    @Test
    @DisplayName("DaMaiFrameException(cause)：作为RuntimeException传递原因")
    void testDaMaiFrameExceptionWithCause() {
        RuntimeException cause = new RuntimeException("root cause");
        DaMaiFrameException exception = new DaMaiFrameException(cause);
        assertSame(cause, exception.getCause());
        // 该构造器不设置code字段
        assertNull(exception.getCode());
    }

    @Test
    @DisplayName("ArgumentException：code与参数错误列表正确保存")
    void testArgumentException() {
        ArgumentError error = new ArgumentError();
        error.setArgumentName("userId");
        error.setMessage("用户id为空");
        List<ArgumentError> errorList = Collections.singletonList(error);

        ArgumentException exception = new ArgumentException(10051, errorList);
        assertEquals(10051, exception.getCode());
        assertEquals(1, exception.getArgumentErrorList().size());
        assertEquals("userId", exception.getArgumentErrorList().get(0).getArgumentName());
        assertEquals("用户id为空", exception.getArgumentErrorList().get(0).getMessage());
    }

    @Test
    @DisplayName("ArgumentException(code, message)：message传递给父类")
    void testArgumentExceptionWithCodeAndMessage() {
        ArgumentException exception = new ArgumentException(10054, "参数验证异常");
        assertEquals(10054, exception.getCode());
        assertEquals("参数验证异常", exception.getMessage());
    }

    @Test
    @DisplayName("BaseException：各构造器行为符合RuntimeException语义")
    void testBaseException() {
        assertNull(new BaseException().getMessage());
        assertEquals("msg", new BaseException("msg").getMessage());
        RuntimeException cause = new RuntimeException("cause");
        assertSame(cause, new BaseException(cause).getCause());
        BaseException exception = new BaseException("msg", cause);
        assertEquals("msg", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
