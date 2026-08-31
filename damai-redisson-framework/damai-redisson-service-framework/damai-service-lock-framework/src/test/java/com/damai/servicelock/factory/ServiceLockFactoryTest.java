package com.damai.servicelock.factory;

import com.damai.core.ManageLocker;
import com.damai.servicelock.LockType;
import com.damai.servicelock.ServiceLocker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ServiceLockFactory 单元测试：验证不同锁类型返回对应的ServiceLocker
 */
@ExtendWith(MockitoExtension.class)
class ServiceLockFactoryTest {

    @Mock
    private ManageLocker manageLocker;

    @InjectMocks
    private ServiceLockFactory serviceLockFactory;

    @Test
    @DisplayName("Reentrant类型应返回可重入锁")
    void reentrantTypeReturnsReentrantLocker() {
        ServiceLocker reentrantLocker = mock(ServiceLocker.class);
        when(manageLocker.getReentrantLocker()).thenReturn(reentrantLocker);

        assertSame(reentrantLocker, serviceLockFactory.getLock(LockType.Reentrant));
    }

    @Test
    @DisplayName("Fair类型应返回公平锁")
    void fairTypeReturnsFairLocker() {
        ServiceLocker fairLocker = mock(ServiceLocker.class);
        when(manageLocker.getFairLocker()).thenReturn(fairLocker);

        assertSame(fairLocker, serviceLockFactory.getLock(LockType.Fair));
    }

    @Test
    @DisplayName("Write类型应返回写锁")
    void writeTypeReturnsWriteLocker() {
        ServiceLocker writeLocker = mock(ServiceLocker.class);
        when(manageLocker.getWriteLocker()).thenReturn(writeLocker);

        assertSame(writeLocker, serviceLockFactory.getLock(LockType.Write));
    }

    @Test
    @DisplayName("Read类型应返回读锁")
    void readTypeReturnsReadLocker() {
        ServiceLocker readLocker = mock(ServiceLocker.class);
        when(manageLocker.getReadLocker()).thenReturn(readLocker);

        assertSame(readLocker, serviceLockFactory.getLock(LockType.Read));
    }
}
