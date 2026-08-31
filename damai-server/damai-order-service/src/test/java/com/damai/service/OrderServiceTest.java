package com.damai.service;

import com.baidu.fsg.uid.UidGenerator;
import com.damai.core.SpringUtil;
import com.damai.dto.OrderCreateDto;
import com.damai.dto.OrderTicketUserCreateDto;
import com.damai.entity.Order;
import com.damai.entity.OrderTicketUser;
import com.damai.enums.BaseCode;
import com.damai.enums.OrderStatus;
import com.damai.exception.DaMaiFrameException;
import com.damai.mapper.OrderMapper;
import com.damai.redis.RedisCache;
import com.damai.redis.RedisKeyBuild;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrderService 单元测试：Mapper/Redis/ID生成器全部用 Mockito mock，验证订单创建与状态校验逻辑
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderTicketUserService orderTicketUserService;

    @Mock
    private RedisCache redisCache;

    @Mock
    private UidGenerator uidGenerator;

    @InjectMocks
    private OrderService orderService;

    @BeforeAll
    static void initSpringUtil() {
        // RedisKeyBuild 依赖 SpringUtil 静态环境读取 key 前缀
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        MockEnvironment environment = new MockEnvironment().withProperty("prefix.distinction.name", "test");
        when(context.getEnvironment()).thenReturn(environment);
        new SpringUtil().initialize(context);
    }

    private OrderCreateDto buildOrderCreateDto() {
        OrderCreateDto dto = new OrderCreateDto();
        dto.setOrderNumber(20240101001L);
        dto.setUserId(1001L);
        dto.setProgramId(100L);
        dto.setOrderPrice(new BigDecimal("380.00"));
        dto.setCreateOrderTime(new Date());

        OrderTicketUserCreateDto ticketUser = new OrderTicketUserCreateDto();
        ticketUser.setTicketUserId(2001L);
        ticketUser.setSeatId(3001L);
        dto.setOrderTicketUserCreateDtoList(List.of(ticketUser));
        return dto;
    }

    @Nested
    @DisplayName("创建订单")
    class CreateTest {

        private OrderCreateDto dto;

        @BeforeEach
        void setUp() {
            dto = buildOrderCreateDto();
        }

        @Test
        @DisplayName("订单号已存在时抛出 ORDER_EXIST 异常，不重复插入")
        void createWithExistingOrderNumber() {
            when(orderMapper.selectOne(any())).thenReturn(new Order());

            DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                    () -> orderService.create(dto));
            assertEquals(BaseCode.ORDER_EXIST.getCode(), exception.getCode());
            verify(orderMapper, never()).insert(any(Order.class));
        }

        @Test
        @DisplayName("创建成功：插入订单、批量保存购票人、累加账户下单数缓存")
        void createSuccess() {
            when(orderMapper.selectOne(any())).thenReturn(null);
            when(uidGenerator.getUid()).thenReturn(9001L);

            String orderNumber = orderService.create(dto);

            assertEquals(String.valueOf(dto.getOrderNumber()), orderNumber);
            verify(orderMapper).insert(argThat((Order o) ->
                    dto.getOrderNumber().equals(o.getOrderNumber())
                            && "电子票".equals(o.getDistributionMode())));
            verify(orderTicketUserService).saveBatch(argThat((List<OrderTicketUser> list) ->
                    list.size() == 1 && Long.valueOf(9001L).equals(list.get(0).getId())));
            // 账户下单数缓存按购票人数量累加
            verify(redisCache).incrBy(any(RedisKeyBuild.class), eq(1L));
        }
    }

    @Nested
    @DisplayName("订单状态校验")
    class CheckOrderStatusTest {

        @Test
        @DisplayName("订单不存在时抛出 ORDER_NOT_EXIST 异常")
        void orderNotExist() {
            DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                    () -> orderService.checkOrderStatus(null));
            assertEquals(BaseCode.ORDER_NOT_EXIST.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("已取消订单抛出 ORDER_CANCEL 异常")
        void orderCancelled() {
            Order order = new Order();
            order.setOrderStatus(OrderStatus.CANCEL.getCode());

            DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                    () -> orderService.checkOrderStatus(order));
            assertEquals(BaseCode.ORDER_CANCEL.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("已支付订单抛出 ORDER_PAY 异常")
        void orderPaid() {
            Order order = new Order();
            order.setOrderStatus(OrderStatus.PAY.getCode());

            DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                    () -> orderService.checkOrderStatus(order));
            assertEquals(BaseCode.ORDER_PAY.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("已退款订单抛出 ORDER_REFUND 异常")
        void orderRefunded() {
            Order order = new Order();
            order.setOrderStatus(OrderStatus.REFUND.getCode());

            DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                    () -> orderService.checkOrderStatus(order));
            assertEquals(BaseCode.ORDER_REFUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("待支付订单校验通过")
        void orderNoPayPass() {
            Order order = new Order();
            order.setOrderStatus(OrderStatus.NO_PAY.getCode());

            orderService.checkOrderStatus(order);
        }
    }
}
