package com.damai.filter;

import com.damai.constant.Constant;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 链路过滤器 RequestParamContextFilter 单元测试
 */
class RequestParamContextFilterTest {

    private final RequestParamContextFilter filter = new RequestParamContextFilter();

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    @DisplayName("请求头携带 traceId 时：过滤链执行期间 MDC 中应能取到 traceId，且过滤结束后被移除")
    void shouldPutTraceIdIntoMdcDuringChainAndRemoveAfter() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(Constant.TRACE_ID)).thenReturn("trace-abc-123");

        AtomicReference<String> traceIdInChain = new AtomicReference<>();
        doAnswer(invocation -> {
            traceIdInChain.set(MDC.get(Constant.TRACE_ID));
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        // 过滤链执行期间 MDC 中有 traceId
        assertEquals("trace-abc-123", traceIdInChain.get());
        // 过滤结束后 traceId 被移除
        assertNull(MDC.get(Constant.TRACE_ID));
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("请求头无 traceId 时：不应向 MDC 写入 traceId，但过滤链仍正常执行")
    void shouldNotPutTraceIdWhenHeaderMissing() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(Constant.TRACE_ID)).thenReturn(null);

        AtomicReference<String> traceIdInChain = new AtomicReference<>();
        doAnswer(invocation -> {
            traceIdInChain.set(MDC.get(Constant.TRACE_ID));
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        assertNull(traceIdInChain.get());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("请求头 traceId 为空字符串时：不应向 MDC 写入 traceId")
    void shouldNotPutTraceIdWhenHeaderEmpty() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(Constant.TRACE_ID)).thenReturn("");

        AtomicReference<String> traceIdInChain = new AtomicReference<>();
        doAnswer(invocation -> {
            traceIdInChain.set(MDC.get(Constant.TRACE_ID));
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        assertNull(traceIdInChain.get());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("过滤链抛异常时：异常应向外抛出，且 MDC 中的 traceId 仍被移除")
    void shouldRemoveTraceIdEvenWhenChainThrows() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(Constant.TRACE_ID)).thenReturn("trace-exception");
        doThrow(new ServletException("chain failed")).when(chain).doFilter(any(), any());

        assertThrows(ServletException.class, () -> filter.doFilter(request, response, chain));

        // finally 中必须清理 MDC
        assertNull(MDC.get(Constant.TRACE_ID));
    }

    @Test
    @DisplayName("FilterConfig：应创建 RequestParamContextFilter 实例")
    void filterConfigShouldCreateFilterBean() {
        FilterConfig filterConfig = new FilterConfig();
        assertEquals(RequestParamContextFilter.class,
                filterConfig.requestParamContextFilter().getClass());
    }
}
