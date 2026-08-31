package com.damai.service;

import com.baidu.fsg.uid.UidGenerator;
import com.damai.client.BaseDataClient;
import com.damai.common.ApiResponse;
import com.damai.dto.UserAuthenticationDto;
import com.damai.dto.UserExistDto;
import com.damai.dto.UserGetAndTicketUserListDto;
import com.damai.dto.UserIdDto;
import com.damai.dto.UserLoginDto;
import com.damai.dto.UserMobileDto;
import com.damai.dto.UserUpdatePasswordDto;
import com.damai.entity.TicketUser;
import com.damai.entity.User;
import com.damai.entity.UserEmail;
import com.damai.entity.UserMobile;
import com.damai.enums.BaseCode;
import com.damai.enums.BusinessStatus;
import com.damai.exception.DaMaiFrameException;
import com.damai.handler.BloomFilterHandler;
import com.damai.initialize.impl.composite.CompositeContainer;
import com.damai.jwt.TokenUtil;
import com.damai.mapper.TicketUserMapper;
import com.damai.mapper.UserEmailMapper;
import com.damai.mapper.UserMapper;
import com.damai.mapper.UserMobileMapper;
import com.damai.redis.RedisCache;
import com.damai.redis.RedisKeyBuild;
import com.damai.core.SpringUtil;
import com.damai.vo.GetChannelDataVo;
import com.damai.vo.UserGetAndTicketUserListVo;
import com.damai.vo.UserLoginVo;
import com.damai.vo.UserVo;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * UserService 单元测试：Mapper、Redis、布隆过滤器、远程客户端全部用 Mockito mock，
 * 仅验证用户服务的纯业务逻辑分支
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserMobileMapper userMobileMapper;

    @Mock
    private UserEmailMapper userEmailMapper;

    @Mock
    private UidGenerator uidGenerator;

    @Mock
    private RedisCache redisCache;

    @Mock
    private TicketUserMapper ticketUserMapper;

    @Mock
    private BloomFilterHandler bloomFilterHandler;

    @Mock
    private CompositeContainer compositeContainer;

    @Mock
    private BaseDataClient baseDataClient;

    @InjectMocks
    private UserService userService;

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
        ReflectionTestUtils.setField(userService, "tokenExpireTime", 40L);
    }

    @Nested
    @DisplayName("doExist 用户存在性检查")
    class DoExistTest {

        @Test
        @DisplayName("布隆过滤器判定不存在时直接通过且不查询数据库")
        void doExistBloomFilterNotContains() {
            when(bloomFilterHandler.contains("13800138000")).thenReturn(false);

            assertDoesNotThrow(() -> userService.doExist("13800138000"));
            verify(userMobileMapper, never()).selectOne(any());
        }

        @Test
        @DisplayName("布隆过滤器命中但数据库无记录时正常通过")
        void doExistBloomFilterHitButDbMiss() {
            when(bloomFilterHandler.contains("13800138000")).thenReturn(true);
            when(userMobileMapper.selectOne(any())).thenReturn(null);

            assertDoesNotThrow(() -> userService.doExist("13800138000"));
        }

        @Test
        @DisplayName("布隆过滤器命中且数据库存在记录时抛出 USER_EXIST 异常")
        void doExistUserAlreadyExists() {
            when(bloomFilterHandler.contains("13800138000")).thenReturn(true);
            when(userMobileMapper.selectOne(any())).thenReturn(new UserMobile());

            DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                    () -> userService.doExist("13800138000"));
            assertEquals(BaseCode.USER_EXIST.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("exist 方法委托给 doExist 处理")
        void existDelegatesToDoExist() {
            when(bloomFilterHandler.contains("13800138000")).thenReturn(true);
            when(userMobileMapper.selectOne(any())).thenReturn(new UserMobile());

            UserExistDto dto = new UserExistDto();
            dto.setMobile("13800138000");
            DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                    () -> userService.exist(dto));
            assertEquals(BaseCode.USER_EXIST.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("login 登录逻辑")
    class LoginTest {

        @Test
        @DisplayName("手机号和邮箱都为空时抛出 USER_MOBILE_AND_EMAIL_NOT_EXIST 异常")
        void loginWithoutMobileAndEmail() {
            UserLoginDto dto = new UserLoginDto();
            dto.setPassword("pwd");

            DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                    () -> userService.login(dto));
            assertEquals(BaseCode.USER_MOBILE_AND_EMAIL_NOT_EXIST.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("手机号登录错误次数达到阈值时抛出 MOBILE_ERROR_COUNT_TOO_MANY 异常")
        void loginMobileErrorCountTooMany() {
            UserLoginDto dto = new UserLoginDto();
            dto.setMobile("13800138000");
            dto.setPassword("pwd");
            when(redisCache.get(any(RedisKeyBuild.class), eq(String.class))).thenReturn("5");

            DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                    () -> userService.login(dto));
            assertEquals(BaseCode.MOBILE_ERROR_COUNT_TOO_MANY.getCode(), exception.getCode());
            // 不应再查询数据库
            verify(userMobileMapper, never()).selectOne(any());
        }

        @Test
        @DisplayName("手机号不存在时抛出 USER_MOBILE_EMPTY 异常并记录错误次数")
        void loginMobileNotExist() {
            UserLoginDto dto = new UserLoginDto();
            dto.setMobile("13800138000");
            dto.setPassword("pwd");
            when(userMobileMapper.selectOne(any())).thenReturn(null);

            DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                    () -> userService.login(dto));
            assertEquals(BaseCode.USER_MOBILE_EMPTY.getCode(), exception.getCode());
            // 验证错误计数增加并设置过期时间
            verify(redisCache).incrBy(any(RedisKeyBuild.class), eq(1L));
            verify(redisCache).expire(any(RedisKeyBuild.class), eq(1L), eq(TimeUnit.MINUTES));
        }

        @Test
        @DisplayName("密码错误时抛出 NAME_PASSWORD_ERROR 异常")
        void loginWithWrongPassword() {
            UserLoginDto dto = new UserLoginDto();
            dto.setMobile("13800138000");
            dto.setPassword("wrong-password");
            UserMobile userMobile = new UserMobile();
            userMobile.setUserId(1001L);
            when(userMobileMapper.selectOne(any())).thenReturn(userMobile);
            when(userMapper.selectOne(any())).thenReturn(null);

            DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                    () -> userService.login(dto));
            assertEquals(BaseCode.NAME_PASSWORD_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("手机号登录成功时返回用户ID与可解析的token，并写入登录缓存")
        void loginSuccessByMobile() {
            UserLoginDto dto = new UserLoginDto();
            dto.setCode("damai");
            dto.setMobile("13800138000");
            dto.setPassword("password123");

            UserMobile userMobile = new UserMobile();
            userMobile.setUserId(1001L);
            when(userMobileMapper.selectOne(any())).thenReturn(userMobile);
            User user = new User();
            user.setId(1001L);
            user.setPassword("password123");
            when(userMapper.selectOne(any())).thenReturn(user);
            GetChannelDataVo channelDataVo = new GetChannelDataVo();
            channelDataVo.setTokenSecret("test-secret");
            // 登录前会先读取错误次数缓存（String类型）
            when(redisCache.get(any(RedisKeyBuild.class), eq(String.class))).thenReturn(null);
            when(redisCache.get(any(RedisKeyBuild.class), eq(GetChannelDataVo.class))).thenReturn(channelDataVo);
            when(uidGenerator.getUid()).thenReturn(9999L);

            UserLoginVo loginVo = userService.login(dto);

            assertEquals(1001L, loginVo.getUserId());
            assertNotNull(loginVo.getToken());
            // 使用相同的秘钥可以解析出用户ID，证明token生成正确
            String userStr = TokenUtil.parseToken(loginVo.getToken(), "test-secret");
            assertEquals("1001", com.alibaba.fastjson.JSONObject.parseObject(userStr).getString("userId"));
            verify(redisCache).set(any(RedisKeyBuild.class), eq(user), eq(40L), eq(TimeUnit.MINUTES));
        }

        @Test
        @DisplayName("渠道数据缓存未命中时通过远程客户端获取并登录成功")
        void loginSuccessByEmailWithChannelDataFromClient() {
            UserLoginDto dto = new UserLoginDto();
            dto.setCode("damai");
            dto.setEmail("test@163.com");
            dto.setPassword("password123");

            UserEmail userEmail = new UserEmail();
            userEmail.setUserId(1002L);
            when(userEmailMapper.selectOne(any())).thenReturn(userEmail);
            User user = new User();
            user.setId(1002L);
            user.setPassword("password123");
            when(userMapper.selectOne(any())).thenReturn(user);
            // 缓存未命中
            // 登录前会先读取错误次数缓存（String类型）
            when(redisCache.get(any(RedisKeyBuild.class), eq(String.class))).thenReturn(null);
            when(redisCache.get(any(RedisKeyBuild.class), eq(GetChannelDataVo.class))).thenReturn(null);
            GetChannelDataVo channelDataVo = new GetChannelDataVo();
            channelDataVo.setTokenSecret("email-secret");
            when(baseDataClient.getByCode(any())).thenReturn(ApiResponse.ok(channelDataVo));
            when(uidGenerator.getUid()).thenReturn(8888L);

            UserLoginVo loginVo = userService.login(dto);

            assertEquals(1002L, loginVo.getUserId());
            assertNotNull(loginVo.getToken());
        }

        @Test
        @DisplayName("邮箱不存在时抛出 USER_EMAIL_NOT_EXIST 异常")
        void loginEmailNotExist() {
            UserLoginDto dto = new UserLoginDto();
            dto.setEmail("test@163.com");
            dto.setPassword("pwd");
            when(userEmailMapper.selectOne(any())).thenReturn(null);

            DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                    () -> userService.login(dto));
            assertEquals(BaseCode.USER_EMAIL_NOT_EXIST.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("实名认证与查询逻辑")
    class AuthenticationAndQueryTest {

        @Test
        @DisplayName("实名认证时用户不存在则抛出 USER_EMPTY 异常")
        void authenticationUserNotExist() {
            UserAuthenticationDto dto = new UserAuthenticationDto();
            dto.setId(1L);
            when(userMapper.selectById(1L)).thenReturn(null);

            DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                    () -> userService.authentication(dto));
            assertEquals(BaseCode.USER_EMPTY.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("已实名认证的用户再次认证时抛出 USER_AUTHENTICATION 异常")
        void authenticationAlreadyAuthenticated() {
            UserAuthenticationDto dto = new UserAuthenticationDto();
            dto.setId(1L);
            User user = new User();
            user.setId(1L);
            user.setRelAuthenticationStatus(BusinessStatus.YES.getCode());
            when(userMapper.selectById(1L)).thenReturn(user);

            DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                    () -> userService.authentication(dto));
            assertEquals(BaseCode.USER_AUTHENTICATION.getCode(), exception.getCode());
            verify(userMapper, never()).updateById(any(com.damai.entity.User.class));
        }

        @Test
        @DisplayName("未认证用户实名认证成功后更新用户状态")
        void authenticationSuccess() {
            UserAuthenticationDto dto = new UserAuthenticationDto();
            dto.setId(1L);
            dto.setRelName("张三");
            dto.setIdNumber("110101199001010011");
            User user = new User();
            user.setId(1L);
            user.setRelAuthenticationStatus(BusinessStatus.NO.getCode());
            when(userMapper.selectById(1L)).thenReturn(user);

            userService.authentication(dto);

            verify(userMapper).updateById(org.mockito.ArgumentMatchers.argThat((com.damai.entity.User u) ->
                    BusinessStatus.YES.getCode().equals(((User) u).getRelAuthenticationStatus())
                            && "张三".equals(((User) u).getRelName())));
        }

        @Test
        @DisplayName("根据手机号查询：手机号不存在时抛出 USER_MOBILE_EMPTY 异常")
        void getByMobileNotExist() {
            UserMobileDto dto = new UserMobileDto();
            dto.setMobile("13800138000");
            when(userMobileMapper.selectOne(any())).thenReturn(null);

            DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                    () -> userService.getByMobile(dto));
            assertEquals(BaseCode.USER_MOBILE_EMPTY.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("根据手机号查询成功时返回用户信息与手机号")
        void getByMobileSuccess() {
            UserMobileDto dto = new UserMobileDto();
            dto.setMobile("13800138000");
            UserMobile userMobile = new UserMobile();
            userMobile.setUserId(1001L);
            userMobile.setMobile("13800138000");
            when(userMobileMapper.selectOne(any())).thenReturn(userMobile);
            User user = new User();
            user.setId(1001L);
            user.setName("测试用户");
            when(userMapper.selectById(1001L)).thenReturn(user);

            UserVo userVo = userService.getByMobile(dto);

            assertEquals(1001L, userVo.getId());
            // UserVo.getMobile 会对手机号脱敏（如 138****8000）
            assertEquals("138****8000", userVo.getMobile());
            assertEquals("测试用户", userVo.getName());
        }

        @Test
        @DisplayName("根据ID查询用户不存在时抛出 USER_EMPTY 异常")
        void getByIdNotExist() {
            UserIdDto dto = new UserIdDto();
            dto.setId(1L);
            when(userMapper.selectById(1L)).thenReturn(null);

            DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                    () -> userService.getById(dto));
            assertEquals(BaseCode.USER_EMPTY.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("查询用户及其购票人列表成功")
        void getUserAndTicketUserListSuccess() {
            UserGetAndTicketUserListDto dto = new UserGetAndTicketUserListDto();
            dto.setUserId(1001L);
            User user = new User();
            user.setId(1001L);
            user.setName("测试用户");
            when(userMapper.selectById(1001L)).thenReturn(user);
            TicketUser ticketUser = new TicketUser();
            ticketUser.setId(2001L);
            ticketUser.setUserId(1001L);
            when(ticketUserMapper.selectList(any())).thenReturn(List.of(ticketUser));

            UserGetAndTicketUserListVo result = userService.getUserAndTicketUserList(dto);

            assertEquals(1001L, result.getUserVo().getId());
            assertEquals(1, result.getTicketUserVoList().size());
            assertEquals(2001L, result.getTicketUserVoList().get(0).getId());
        }
    }

    @Nested
    @DisplayName("修改用户信息逻辑")
    class UpdateTest {

        @Test
        @DisplayName("修改密码时用户不存在则抛出 USER_EMPTY 异常")
        void updatePasswordUserNotExist() {
            UserUpdatePasswordDto dto = new UserUpdatePasswordDto();
            dto.setId(1L);
            when(userMapper.selectById(1L)).thenReturn(null);

            DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                    () -> userService.updatePassword(dto));
            assertEquals(BaseCode.USER_EMPTY.getCode(), exception.getCode());
            verify(userMapper, never()).updateById(any(com.damai.entity.User.class));
        }

        @Test
        @DisplayName("修改密码成功时调用 updateById 更新密码")
        void updatePasswordSuccess() {
            UserUpdatePasswordDto dto = new UserUpdatePasswordDto();
            dto.setId(1L);
            dto.setPassword("newPassword");
            User user = new User();
            user.setId(1L);
            when(userMapper.selectById(1L)).thenReturn(user);

            userService.updatePassword(dto);

            verify(userMapper).updateById(org.mockito.ArgumentMatchers.argThat((com.damai.entity.User u) ->
                    "newPassword".equals(((User) u).getPassword())));
        }
    }
}
