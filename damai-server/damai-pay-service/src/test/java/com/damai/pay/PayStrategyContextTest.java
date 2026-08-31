package com.damai.pay;

import com.damai.enums.BaseCode;
import com.damai.exception.DaMaiFrameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * 支付策略上下文单元测试
 */
@ExtendWith(MockitoExtension.class)
class PayStrategyContextTest {

    @Mock
    private PayStrategyHandler alipayHandler;

    @Test
    @DisplayName("put 后按渠道 get 能取到同一个策略处理器")
    void putAndGetReturnsSameHandler() {
        PayStrategyContext context = new PayStrategyContext();
        when(alipayHandler.getChannel()).thenReturn("alipay");

        context.put(alipayHandler.getChannel(), alipayHandler);

        assertSame(alipayHandler, context.get("alipay"));
    }

    @Test
    @DisplayName("get 不存在的渠道时抛出 PAY_STRATEGY_NOT_EXIST 异常")
    void getUnknownChannelThrowsException() {
        PayStrategyContext context = new PayStrategyContext();

        DaMaiFrameException exception =
                assertThrows(DaMaiFrameException.class, () -> context.get("wx"));
        assertEquals(BaseCode.PAY_STRATEGY_NOT_EXIST.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("同一渠道重复 put 后 get 返回后注册的处理器")
    void putSameChannelOverridesPreviousHandler() {
        PayStrategyContext context = new PayStrategyContext();
        PayStrategyHandler anotherHandler = org.mockito.Mockito.mock(PayStrategyHandler.class);
        context.put("alipay", alipayHandler);
        context.put("alipay", anotherHandler);

        assertSame(anotherHandler, context.get("alipay"));
    }
}
