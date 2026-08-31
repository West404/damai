package com.damai.service.composite;

import com.damai.dto.ProgramGetDto;
import com.damai.enums.BaseCode;
import com.damai.exception.DaMaiFrameException;
import com.damai.handler.BloomFilterHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * ProgramBloomFilterCheckHandler 节目详情布隆过滤器校验单元测试
 */
@ExtendWith(MockitoExtension.class)
class ProgramBloomFilterCheckHandlerTest {

    @Mock
    private BloomFilterHandler bloomFilterHandler;

    @InjectMocks
    private ProgramBloomFilterCheckHandler handler;

    private ProgramGetDto programGetDto;

    @BeforeEach
    void setUp() {
        programGetDto = new ProgramGetDto();
        programGetDto.setId(1001L);
    }

    @Test
    @DisplayName("布隆过滤器包含节目ID时校验通过")
    void executePassWhenBloomFilterContains() {
        when(bloomFilterHandler.contains("1001")).thenReturn(true);

        assertDoesNotThrow(() -> handler.execute(programGetDto));
    }

    @Test
    @DisplayName("布隆过滤器不包含节目ID时抛出 PROGRAM_NOT_EXIST 异常，防止缓存穿透")
    void executeThrowsWhenBloomFilterNotContains() {
        when(bloomFilterHandler.contains("1001")).thenReturn(false);

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> handler.execute(programGetDto));
        assertEquals(BaseCode.PROGRAM_NOT_EXIST.getCode(), exception.getCode());
    }
}
