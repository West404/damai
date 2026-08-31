package com.damai.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @description: BaseCode / OrderStatus 枚举查询方法单元测试
 **/
class EnumCodeTest {

    @Test
    @DisplayName("BaseCode.getRc：按code命中枚举实例")
    void testBaseCodeGetRc() {
        assertEquals(BaseCode.SUCCESS, BaseCode.getRc(0));
        assertEquals(BaseCode.SYSTEM_ERROR, BaseCode.getRc(-1));
        assertEquals(BaseCode.TOKEN_EXPIRE, BaseCode.getRc(10055));
    }

    @Test
    @DisplayName("BaseCode.getRc：不存在的code返回null")
    void testBaseCodeGetRcNotExist() {
        assertNull(BaseCode.getRc(999999));
    }

    @Test
    @DisplayName("BaseCode.getMsg：按code取描述，不存在的code返回空串")
    void testBaseCodeGetMsg() {
        assertEquals("OK", BaseCode.getMsg(0));
        assertEquals("系统异常，请稍后重试", BaseCode.getMsg(-1));
        assertEquals("", BaseCode.getMsg(999999));
    }

    @Test
    @DisplayName("BaseCode：getCode/getMsg返回构造时的值")
    void testBaseCodeGetter() {
        assertEquals(0, BaseCode.SUCCESS.getCode());
        assertEquals("OK", BaseCode.SUCCESS.getMsg());
    }

    @Test
    @DisplayName("OrderStatus.getRc：按code命中订单状态枚举")
    void testOrderStatusGetRc() {
        assertEquals(OrderStatus.NO_PAY, OrderStatus.getRc(1));
        assertEquals(OrderStatus.CANCEL, OrderStatus.getRc(2));
        assertEquals(OrderStatus.PAY, OrderStatus.getRc(3));
        assertEquals(OrderStatus.REFUND, OrderStatus.getRc(4));
    }

    @Test
    @DisplayName("OrderStatus.getRc/getMsg：不存在的code分别返回null和空串")
    void testOrderStatusNotExist() {
        assertNull(OrderStatus.getRc(99));
        assertEquals("", OrderStatus.getMsg(99));
    }

    @Test
    @DisplayName("OrderStatus.getMsg：按code取中文描述")
    void testOrderStatusGetMsg() {
        assertEquals("未支付", OrderStatus.getMsg(1));
        assertEquals("已取消", OrderStatus.getMsg(2));
        assertEquals("已支付", OrderStatus.getMsg(3));
        assertEquals("已退单", OrderStatus.getMsg(4));
    }
}
