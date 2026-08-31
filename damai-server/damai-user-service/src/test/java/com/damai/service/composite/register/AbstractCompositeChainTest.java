package com.damai.service.composite.register;

import com.damai.enums.CompositeCheckType;
import com.damai.initialize.impl.composite.AbstractComposite;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AbstractComposite 责任链执行逻辑单元测试：验证广度优先按层级执行与异常中断传播
 */
class AbstractCompositeChainTest {

    /**
     * 测试用桩组件：执行时将名字记录到共享列表中
     */
    private static class StubComposite extends AbstractComposite<String> {

        private final String name;
        private final List<String> executeLog;
        private final boolean throwException;

        StubComposite(String name, List<String> executeLog, boolean throwException) {
            this.name = name;
            this.executeLog = executeLog;
            this.throwException = throwException;
        }

        @Override
        protected void execute(final String param) {
            if (throwException) {
                throw new RuntimeException("组件" + name + "执行失败");
            }
            executeLog.add(name);
        }

        @Override
        public String type() {
            return CompositeCheckType.USER_REGISTER_CHECK.getValue();
        }

        @Override
        public Integer executeParentOrder() {
            return 0;
        }

        @Override
        public Integer executeTier() {
            return 1;
        }

        @Override
        public Integer executeOrder() {
            return 1;
        }
    }

    @Test
    @DisplayName("allExecute 按广度优先顺序依次执行父节点和子节点")
    void allExecuteBreadthFirstOrder() {
        List<String> executeLog = new ArrayList<>();
        StubComposite root = new StubComposite("root", executeLog, false);
        StubComposite childA = new StubComposite("childA", executeLog, false);
        StubComposite childB = new StubComposite("childB", executeLog, false);
        StubComposite grandChild = new StubComposite("grandChild", executeLog, false);
        root.add(childA);
        root.add(childB);
        childA.add(grandChild);

        root.allExecute("param");

        assertEquals(List.of("root", "childA", "childB", "grandChild"), executeLog);
    }

    @Test
    @DisplayName("某个节点抛出异常时中断执行并向上传播，后续节点不再执行")
    void allExecuteStopsOnException() {
        List<String> executeLog = new ArrayList<>();
        StubComposite root = new StubComposite("root", executeLog, false);
        StubComposite failChild = new StubComposite("failChild", executeLog, true);
        StubComposite laterChild = new StubComposite("laterChild", executeLog, false);
        root.add(failChild);
        root.add(laterChild);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> root.allExecute("param"));
        assertEquals("组件failChild执行失败", exception.getMessage());
        // failChild 之后的 laterChild 不应被执行
        assertEquals(List.of("root"), executeLog);
    }

    @Test
    @DisplayName("叶子节点单独执行 allExecute 时只执行自身")
    void allExecuteSingleNode() {
        List<String> executeLog = new ArrayList<>();
        StubComposite leaf = new StubComposite("leaf", executeLog, false);

        leaf.allExecute("param");

        assertEquals(List.of("leaf"), executeLog);
    }
}
