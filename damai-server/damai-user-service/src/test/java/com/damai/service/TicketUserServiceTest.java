package com.damai.service;

import com.baidu.fsg.uid.UidGenerator;
import com.damai.dto.TicketUserDto;
import com.damai.dto.TicketUserIdDto;
import com.damai.dto.TicketUserListDto;
import com.damai.entity.TicketUser;
import com.damai.entity.User;
import com.damai.enums.BaseCode;
import com.damai.exception.DaMaiFrameException;
import com.damai.mapper.TicketUserMapper;
import com.damai.mapper.UserMapper;
import com.damai.redis.RedisCache;
import com.damai.redis.RedisKeyBuild;
import com.damai.core.SpringUtil;
import com.damai.vo.TicketUserVo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TicketUserService 单元测试：Mapper 与 Redis 全部用 Mockito mock，验证购票人增删查逻辑
 */
@ExtendWith(MockitoExtension.class)
class TicketUserServiceTest {

    @Mock
    private TicketUserMapper ticketUserMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UidGenerator uidGenerator;

    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private TicketUserService ticketUserService;

    @BeforeAll
    static void initSpringUtil() {
        // RedisKeyBuild 依赖 SpringUtil 静态环境读取 key 前缀
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        MockEnvironment environment = new MockEnvironment().withProperty("prefix.distinction.name", "test");
        when(context.getEnvironment()).thenReturn(environment);
        new SpringUtil().initialize(context);
    }

    @Test
    @DisplayName("查询购票人列表：缓存命中时直接返回且不查询数据库")
    void listCacheHit() {
        TicketUserListDto dto = new TicketUserListDto();
        dto.setUserId(1001L);
        TicketUserVo cachedVo = new TicketUserVo();
        cachedVo.setId(2001L);
        when(redisCache.getValueIsList(any(RedisKeyBuild.class), eq(TicketUserVo.class)))
                .thenReturn(List.of(cachedVo));

        List<TicketUserVo> result = ticketUserService.list(dto);

        assertEquals(1, result.size());
        assertEquals(2001L, result.get(0).getId());
        verify(ticketUserMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("查询购票人列表：缓存未命中时查询数据库并转换返回")
    void listCacheMiss() {
        TicketUserListDto dto = new TicketUserListDto();
        dto.setUserId(1001L);
        when(redisCache.getValueIsList(any(RedisKeyBuild.class), eq(TicketUserVo.class)))
                .thenReturn(List.of());
        TicketUser ticketUser = new TicketUser();
        ticketUser.setId(2002L);
        ticketUser.setUserId(1001L);
        when(ticketUserMapper.selectList(any())).thenReturn(List.of(ticketUser));

        List<TicketUserVo> result = ticketUserService.list(dto);

        assertEquals(1, result.size());
        assertEquals(2002L, result.get(0).getId());
        verify(ticketUserMapper).selectList(any());
    }

    @Test
    @DisplayName("新增购票人：所属用户不存在时抛出 USER_EMPTY 异常")
    void addUserNotExist() {
        TicketUserDto dto = new TicketUserDto();
        dto.setUserId(1001L);
        when(userMapper.selectById(1001L)).thenReturn(null);

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> ticketUserService.add(dto));
        assertEquals(BaseCode.USER_EMPTY.getCode(), exception.getCode());
        verify(ticketUserMapper, never()).insert(any(TicketUser.class));
    }

    @Test
    @DisplayName("新增购票人：相同证件类型与证件号已存在时抛出 TICKET_USER_EXIST 异常")
    void addTicketUserAlreadyExist() {
        TicketUserDto dto = new TicketUserDto();
        dto.setUserId(1001L);
        dto.setIdType(1);
        dto.setIdNumber("110101199001010011");
        when(userMapper.selectById(1001L)).thenReturn(new User());
        when(ticketUserMapper.selectOne(any())).thenReturn(new TicketUser());

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> ticketUserService.add(dto));
        assertEquals(BaseCode.TICKET_USER_EXIST.getCode(), exception.getCode());
        verify(ticketUserMapper, never()).insert(any(TicketUser.class));
    }

    @Test
    @DisplayName("新增购票人成功：插入数据库并删除列表缓存")
    void addSuccess() {
        TicketUserDto dto = new TicketUserDto();
        dto.setUserId(1001L);
        dto.setRelName("张三");
        dto.setIdType(1);
        dto.setIdNumber("110101199001010011");
        when(userMapper.selectById(1001L)).thenReturn(new User());
        when(ticketUserMapper.selectOne(any())).thenReturn(null);
        when(uidGenerator.getUid()).thenReturn(2003L);

        ticketUserService.add(dto);

        verify(ticketUserMapper).insert(org.mockito.ArgumentMatchers.argThat((TicketUser t) ->
                Long.valueOf(2003L).equals(t.getId())
                        && "张三".equals(t.getRelName())));
        verify(redisCache).del(any(RedisKeyBuild.class));
    }

    @Test
    @DisplayName("删除购票人：购票人不存在时抛出 TICKET_USER_EMPTY 异常")
    void deleteNotExist() {
        TicketUserIdDto dto = new TicketUserIdDto();
        dto.setId(2001L);
        when(ticketUserMapper.selectById(2001L)).thenReturn(null);

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> ticketUserService.delete(dto));
        assertEquals(BaseCode.TICKET_USER_EMPTY.getCode(), exception.getCode());
        verify(ticketUserMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("删除购票人成功：删除数据库记录并清除列表缓存")
    void deleteSuccess() {
        TicketUserIdDto dto = new TicketUserIdDto();
        dto.setId(2001L);
        TicketUser ticketUser = new TicketUser();
        ticketUser.setId(2001L);
        ticketUser.setUserId(1001L);
        when(ticketUserMapper.selectById(2001L)).thenReturn(ticketUser);

        ticketUserService.delete(dto);

        verify(ticketUserMapper).deleteById(2001L);
        verify(redisCache).del(any(RedisKeyBuild.class));
    }
}
