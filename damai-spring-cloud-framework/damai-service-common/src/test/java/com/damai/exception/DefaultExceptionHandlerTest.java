package com.damai.exception;

import com.damai.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 全局异常处理器 {@link DefaultExceptionHandler} 单元测试（HttpServletRequest 等均由 Mockito mock）
 */
class DefaultExceptionHandlerTest {

    private DefaultExceptionHandler handler;

    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new DefaultExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        when(request.getQueryString()).thenReturn("a=1");
    }

    @Test
    @DisplayName("业务异常处理：返回异常携带的错误码和错误信息")
    void toolkitExceptionHandler() {
        DaMaiFrameException exception = new DaMaiFrameException(70000, "order_number的值不存在");

        ApiResponse<String> response = handler.toolkitExceptionHandler(request, exception);

        assertEquals(70000, response.getCode());
        assertEquals("order_number的值不存在", response.getMessage());
    }

    @Test
    @DisplayName("参数验证异常处理：返回每个校验失败字段的名称和提示信息")
    void validExceptionHandler() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(exception.getMessage()).thenReturn("参数验证失败");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("dto", "pageNumber", "页码不能为空"),
                new FieldError("dto", "pageSize", "页大小不能为空")
        ));

        ApiResponse<List<ArgumentError>> response = handler.validExceptionHandler(request, exception);

        List<ArgumentError> errors = response.getData();
        assertEquals(2, errors.size());
        assertEquals("pageNumber", errors.get(0).getArgumentName());
        assertEquals("页码不能为空", errors.get(0).getMessage());
        assertEquals("pageSize", errors.get(1).getArgumentName());
        assertEquals("页大小不能为空", errors.get(1).getMessage());
    }

    @Test
    @DisplayName("兜底异常处理：返回固定的系统错误响应")
    void defaultErrorHandler() {
        ApiResponse<String> response = handler.defaultErrorHandler(request, new RuntimeException("boom"));

        assertEquals(-100, response.getCode());
        assertEquals("系统错误，请稍后重试!", response.getMessage());
    }
}
