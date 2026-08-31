package com.damai.base;

import com.damai.threadlocal.BaseParameterHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * BaseThreadPool 的 ThreadLocal/MDC 上下文传递逻辑单元测试
 */
class BaseThreadPoolTest {

    @AfterEach
    void cleanUp() {
        // 每个用例结束后清理线程上下文，避免用例间相互污染
        MDC.clear();
        BaseParameterHolder.removeParameterMap();
    }

    @Test
    @DisplayName("wrapTask(Runnable)：任务执行期间应切换到父线程上下文，执行完后恢复当前线程原上下文")
    void wrapRunnableShouldSwitchToParentContextAndRestore() {
        // 父线程上下文
        Map<String, String> parentMdc = new HashMap<>();
        parentMdc.put("traceId", "parent-trace-id");
        Map<String, String> parentHold = new HashMap<>();
        parentHold.put("parentKey", "parentValue");

        // 模拟工作线程自身已有的上下文
        MDC.put("workerKey", "workerValue");
        BaseParameterHolder.setParameter("workerHoldKey", "workerHoldValue");

        AtomicReference<String> mdcInTask = new AtomicReference<>();
        AtomicReference<String> workerMdcInTask = new AtomicReference<>();
        AtomicReference<String> holdInTask = new AtomicReference<>();
        AtomicReference<String> workerHoldInTask = new AtomicReference<>();

        Runnable wrapped = BaseThreadPool.wrapTask(() -> {
            mdcInTask.set(MDC.get("traceId"));
            workerMdcInTask.set(MDC.get("workerKey"));
            holdInTask.set(BaseParameterHolder.getParameter("parentKey"));
            workerHoldInTask.set(BaseParameterHolder.getParameter("workerHoldKey"));
        }, parentMdc, parentHold);

        wrapped.run();

        // 任务内应看到父线程上下文，而不是工作线程自身上下文
        assertEquals("parent-trace-id", mdcInTask.get());
        assertNull(workerMdcInTask.get());
        assertEquals("parentValue", holdInTask.get());
        assertNull(workerHoldInTask.get());

        // 任务结束后，当前线程上下文应恢复为执行前的状态
        assertEquals("workerValue", MDC.get("workerKey"));
        assertNull(MDC.get("traceId"));
        assertEquals("workerHoldValue", BaseParameterHolder.getParameter("workerHoldKey"));
        assertNull(BaseParameterHolder.getParameter("parentKey"));
    }

    @Test
    @DisplayName("wrapTask(Callable)：应返回任务结果，并在执行期间切换上下文、执行后恢复")
    void wrapCallableShouldReturnResultAndPropagateContext() throws Exception {
        Map<String, String> parentMdc = new HashMap<>();
        parentMdc.put("traceId", "callable-trace-id");
        Map<String, String> parentHold = new HashMap<>();
        parentHold.put("holdKey", "holdValue");

        AtomicReference<String> mdcInTask = new AtomicReference<>();
        AtomicReference<String> holdInTask = new AtomicReference<>();

        Callable<String> wrapped = BaseThreadPool.wrapTask(() -> {
            mdcInTask.set(MDC.get("traceId"));
            holdInTask.set(BaseParameterHolder.getParameter("holdKey"));
            return "task-result";
        }, parentMdc, parentHold);

        String result = wrapped.call();

        assertEquals("task-result", result);
        assertEquals("callable-trace-id", mdcInTask.get());
        assertEquals("holdValue", holdInTask.get());
        // 当前线程原本无上下文，执行后应保持无上下文
        assertNull(MDC.get("traceId"));
        assertNull(BaseParameterHolder.getParameter("holdKey"));
    }

    @Test
    @DisplayName("wrapTask：父线程上下文为 null 时，任务内应清空 MDC 并移除 BaseParameterHolder")
    void wrapTaskWithNullParentContextShouldClearContext() {
        // 工作线程已有上下文
        MDC.put("traceId", "worker-trace-id");
        BaseParameterHolder.setParameter("key", "value");

        AtomicReference<String> mdcInTask = new AtomicReference<>();
        AtomicReference<String> holdInTask = new AtomicReference<>();

        Runnable wrapped = BaseThreadPool.wrapTask(() -> {
            mdcInTask.set(MDC.get("traceId"));
            holdInTask.set(BaseParameterHolder.getParameter("key"));
        }, null, null);

        wrapped.run();

        // 任务内上下文被清空
        assertNull(mdcInTask.get());
        assertNull(holdInTask.get());

        // 执行后工作线程上下文恢复
        assertEquals("worker-trace-id", MDC.get("traceId"));
        assertEquals("value", BaseParameterHolder.getParameter("key"));
    }

    @Test
    @DisplayName("wrapTask：任务抛异常时异常应向外抛出，且当前线程上下文仍被恢复")
    void wrapTaskShouldRestoreContextEvenWhenTaskThrows() {
        MDC.put("workerKey", "workerValue");
        BaseParameterHolder.setParameter("workerHoldKey", "workerHoldValue");

        Map<String, String> parentMdc = new HashMap<>();
        parentMdc.put("traceId", "parent-trace-id");

        Runnable wrapped = BaseThreadPool.wrapTask((Runnable) () -> {
            throw new IllegalStateException("task failed");
        }, parentMdc, new HashMap<>());

        assertThrows(IllegalStateException.class, wrapped::run);

        // 即使任务异常，当前线程上下文也必须恢复
        assertEquals("workerValue", MDC.get("workerKey"));
        assertNull(MDC.get("traceId"));
        assertEquals("workerHoldValue", BaseParameterHolder.getParameter("workerHoldKey"));
    }
}
