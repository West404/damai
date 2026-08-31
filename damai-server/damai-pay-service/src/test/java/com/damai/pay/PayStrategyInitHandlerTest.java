package com.damai.pay;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

/**
 * 支付策略初始化处理器单元测试
 */
@ExtendWith(MockitoExtension.class)
class PayStrategyInitHandlerTest {

    @Mock
    private ConfigurableApplicationContext applicationContext;

    @Mock
    private PayStrategyHandler alipayHandler;

    @Test
    @DisplayName("executeOrder 返回固定的执行顺序 1")
    void executeOrderReturnsOne() {
        PayStrategyInitHandler initHandler = new PayStrategyInitHandler(new PayStrategyContext());

        assertEquals(1, initHandler.executeOrder());
    }

    @Test
    @DisplayName("executeInit 将容器中的支付策略按渠道注册到上下文")
    void executeInitRegistersHandlersByChannel() {
        Map<String, PayStrategyHandler> handlerMap = new HashMap<>();
        handlerMap.put("alipayStrategyHandler", alipayHandler);
        when(applicationContext.getBeansOfType(PayStrategyHandler.class)).thenReturn(handlerMap);
        when(alipayHandler.getChannel()).thenReturn("alipay");

        PayStrategyContext payStrategyContext = new PayStrategyContext();
        PayStrategyInitHandler initHandler = new PayStrategyInitHandler(payStrategyContext);
        initHandler.executeInit(applicationContext);

        assertSame(alipayHandler, payStrategyContext.get("alipay"));
    }
}
