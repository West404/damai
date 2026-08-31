package com.damai.threadlocal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @description: BaseParameterHolder 单元测试
 **/
class BaseParameterHolderTest {

    @AfterEach
    void tearDown() {
        // 每个用例结束后清理线程本地变量，避免相互污染
        BaseParameterHolder.removeParameterMap();
    }

    @Test
    @DisplayName("setParameter/getParameter：设置的参数可正确读取")
    void testSetAndGetParameter() {
        BaseParameterHolder.setParameter("code", "0001");
        BaseParameterHolder.setParameter("channel", "app");
        assertEquals("0001", BaseParameterHolder.getParameter("code"));
        assertEquals("app", BaseParameterHolder.getParameter("channel"));
    }

    @Test
    @DisplayName("getParameter：未设置的参数返回null")
    void testGetParameterNotExist() {
        assertNull(BaseParameterHolder.getParameter("not-exist"));
    }

    @Test
    @DisplayName("removeParameter：删除指定参数后读取返回null，其余参数不受影响")
    void testRemoveParameter() {
        BaseParameterHolder.setParameter("code", "0001");
        BaseParameterHolder.setParameter("channel", "app");
        BaseParameterHolder.removeParameter("code");
        assertNull(BaseParameterHolder.getParameter("code"));
        assertEquals("app", BaseParameterHolder.getParameter("channel"));
    }

    @Test
    @DisplayName("removeParameterMap：清空整个Map后所有参数不可读取")
    void testRemoveParameterMap() {
        BaseParameterHolder.setParameter("code", "0001");
        BaseParameterHolder.removeParameterMap();
        assertNull(BaseParameterHolder.getParameter("code"));
    }

    @Test
    @DisplayName("setParameterMap/getParameterMap：整体Map设置与读取")
    void testSetAndGetParameterMap() {
        Map<String, String> map = new HashMap<>(4);
        map.put("key1", "value1");
        BaseParameterHolder.setParameterMap(map);
        assertEquals("value1", BaseParameterHolder.getParameter("key1"));
        assertEquals(map, BaseParameterHolder.getParameterMap());
    }

    @Test
    @DisplayName("getParameterMap：未设置时返回空Map而非null")
    void testGetParameterMapWhenEmpty() {
        BaseParameterHolder.removeParameterMap();
        Map<String, String> map = BaseParameterHolder.getParameterMap();
        assertTrue(map.isEmpty());
    }

    @Test
    @DisplayName("线程隔离：子线程读取不到主线程设置的参数")
    void testThreadIsolation() throws InterruptedException {
        BaseParameterHolder.setParameter("code", "main-thread");

        AtomicReference<String> valueInOtherThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Thread thread = new Thread(() -> {
            valueInOtherThread.set(BaseParameterHolder.getParameter("code"));
            latch.countDown();
        });
        thread.start();
        latch.await();

        assertNull(valueInOtherThread.get());
        assertEquals("main-thread", BaseParameterHolder.getParameter("code"));
    }
}
