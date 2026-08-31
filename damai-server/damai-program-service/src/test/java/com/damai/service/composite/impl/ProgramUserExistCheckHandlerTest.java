package com.damai.service.composite.impl;

import com.damai.client.OrderClient;
import com.damai.client.UserClient;
import com.damai.common.ApiResponse;
import com.damai.core.SpringUtil;
import com.damai.dto.AccountOrderCountDto;
import com.damai.dto.ProgramOrderCreateDto;
import com.damai.enums.BaseCode;
import com.damai.exception.DaMaiFrameException;
import com.damai.redis.RedisCache;
import com.damai.redis.RedisKeyBuild;
import com.damai.service.ProgramService;
import com.damai.service.tool.TokenExpireManager;
import com.damai.vo.AccountOrderCountVo;
import com.damai.vo.ProgramVo;
import com.damai.vo.TicketUserVo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ProgramUserExistCheckHandler 下单用户校验单元测试（Redis、Feign客户端、节目服务全部 mock）
 */
@ExtendWith(MockitoExtension.class)
class ProgramUserExistCheckHandlerTest {

    @Mock
    private UserClient userClient;

    @Mock
    private RedisCache redisCache;

    @Mock
    private OrderClient orderClient;

    @Mock
    private ProgramService programService;

    @Mock
    private TokenExpireManager tokenExpireManager;

    @InjectMocks
    private ProgramUserExistCheckHandler handler;

    private ProgramOrderCreateDto dto;

    @BeforeAll
    static void initSpringUtil() {
        // RedisKeyBuild 依赖 SpringUtil 静态环境读取 key 前缀
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        MockEnvironment environment = new MockEnvironment().withProperty("prefix.distinction.name", "test");
        when(context.getEnvironment()).thenReturn(environment);
        new SpringUtil().initialize(context);
    }

    @BeforeEach
    void setUp() {
        dto = new ProgramOrderCreateDto();
        dto.setUserId(1L);
        dto.setProgramId(100L);
        dto.setTicketUserIdList(List.of(2001L));
        dto.setTicketCount(2);
    }

    private TicketUserVo buildTicketUser(Long id) {
        TicketUserVo vo = new TicketUserVo();
        vo.setId(id);
        return vo;
    }

    @Test
    @DisplayName("购票人列表缓存命中且合法时校验通过，不调用用户服务")
    void executePassWithCachedTicketUsers() {
        when(redisCache.getValueIsList(any(RedisKeyBuild.class), eq(TicketUserVo.class)))
                .thenReturn(List.of(buildTicketUser(2001L)));
        when(programService.detailV2(any())).thenReturn(new ProgramVo());
        when(redisCache.hasKey(any(RedisKeyBuild.class))).thenReturn(true);
        when(redisCache.get(any(RedisKeyBuild.class), eq(Integer.class))).thenReturn(0);

        assertDoesNotThrow(() -> handler.execute(dto));
        verify(userClient, never()).list(any());
        verify(orderClient, never()).accountOrderCount(any());
    }

    @Test
    @DisplayName("购票人列表缓存未命中时走用户服务远程调用")
    void executePassWithTicketUsersFromClient() {
        when(redisCache.getValueIsList(any(RedisKeyBuild.class), eq(TicketUserVo.class)))
                .thenReturn(Collections.emptyList());
        when(userClient.list(any())).thenReturn(ApiResponse.ok(List.of(buildTicketUser(2001L))));
        when(programService.detailV2(any())).thenReturn(new ProgramVo());
        when(redisCache.hasKey(any(RedisKeyBuild.class))).thenReturn(true);
        when(redisCache.get(any(RedisKeyBuild.class), eq(Integer.class))).thenReturn(0);

        assertDoesNotThrow(() -> handler.execute(dto));
        verify(userClient).list(any());
    }

    @Test
    @DisplayName("用户服务返回失败码时抛出携带响应体的异常")
    void executeThrowsWhenUserClientFails() {
        when(redisCache.getValueIsList(any(RedisKeyBuild.class), eq(TicketUserVo.class)))
                .thenReturn(Collections.emptyList());
        when(userClient.list(any())).thenReturn(ApiResponse.error(BaseCode.SYSTEM_ERROR));

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> handler.execute(dto));
        assertEquals(BaseCode.SYSTEM_ERROR.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("购票人列表为空时抛出 TICKET_USER_EMPTY 异常")
    void executeThrowsWhenTicketUserEmpty() {
        when(redisCache.getValueIsList(any(RedisKeyBuild.class), eq(TicketUserVo.class)))
                .thenReturn(Collections.emptyList());
        when(userClient.list(any())).thenReturn(ApiResponse.ok(Collections.emptyList()));

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> handler.execute(dto));
        assertEquals(BaseCode.TICKET_USER_EMPTY.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("下单的购票人ID不在用户购票人列表中时抛出 TICKET_USER_EMPTY 异常")
    void executeThrowsWhenTicketUserNotInList() {
        dto.setTicketUserIdList(List.of(9999L));
        when(redisCache.getValueIsList(any(RedisKeyBuild.class), eq(TicketUserVo.class)))
                .thenReturn(List.of(buildTicketUser(2001L)));

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> handler.execute(dto));
        assertEquals(BaseCode.TICKET_USER_EMPTY.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("节目不存在时抛出 PROGRAM_NOT_EXIST 异常")
    void executeThrowsWhenProgramNotExist() {
        when(redisCache.getValueIsList(any(RedisKeyBuild.class), eq(TicketUserVo.class)))
                .thenReturn(List.of(buildTicketUser(2001L)));
        when(programService.detailV2(any())).thenReturn(null);

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> handler.execute(dto));
        assertEquals(BaseCode.PROGRAM_NOT_EXIST.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("账户下单数缓存未命中时走订单服务远程调用并回写缓存")
    void executeLoadsAccountOrderCountFromClient() {
        when(redisCache.getValueIsList(any(RedisKeyBuild.class), eq(TicketUserVo.class)))
                .thenReturn(List.of(buildTicketUser(2001L)));
        when(programService.detailV2(any())).thenReturn(new ProgramVo());
        when(redisCache.hasKey(any(RedisKeyBuild.class))).thenReturn(false);
        AccountOrderCountVo countVo = new AccountOrderCountVo();
        countVo.setCount(1);
        when(orderClient.accountOrderCount(any(AccountOrderCountDto.class)))
                .thenReturn(ApiResponse.ok(countVo));
        when(tokenExpireManager.getTokenExpireTime()).thenReturn(30L);

        assertDoesNotThrow(() -> handler.execute(dto));
        verify(orderClient).accountOrderCount(any(AccountOrderCountDto.class));
        // 回写缓存，过期时间为 tokenExpireTime + 1
        verify(redisCache).set(any(RedisKeyBuild.class), eq(1), eq(31L), eq(TimeUnit.MINUTES));
    }
}
