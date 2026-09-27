import json, math, os

UA = r"E:/WorkSpace/damai/.ua"
inp = json.load(open(UA + "/tmp/ua-file-analyzer-input-2.json", encoding="utf-8"))
imp = inp["batchImportData"]
BATCH = 2

def f(p):
    return "file:" + p

# ---------------- file nodes ----------------
FILE_META = {
"damai-server-client/damai-user-client/src/main/java/com/damai/client/UserClient.java": {
 "name": "UserClient.java",
 "summary": "用户服务的 Feign 客户端接口，声明按 id 查用户、按 userId 查购票人列表、查询用户与购票人集合三个远程调用方法，由网关/订单等服务跨服务调用。",
 "tags": ["feign-client", "service-client", "接口", "远程调用"], "complexity": "simple",
 "languageNotes": "@FeignClient 通过 SPRING_INJECT_PREFIX_DISTINCTION_NAME 拼接服务名，并指定 UserClientFallback 作为熔断降级实现。"},
"damai-server-client/damai-user-client/src/main/java/com/damai/client/UserClientFallback.java": {
 "name": "UserClientFallback.java",
 "summary": "用户服务 Feign 调用的降级实现，三个远程方法失败时统一返回 BaseCode.SYSTEM_ERROR 包装的 ApiResponse。",
 "tags": ["fallback", "feign-client", "容错", "service-client"], "complexity": "simple"},
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/TicketUserDto.java": {
 "name": "TicketUserDto.java",
 "summary": "新增购票人的请求参数 DTO，承载用户 id、真实姓名、证件类型与证件号码。",
 "tags": ["dto", "data-model", "请求参数", "购票人"], "complexity": "simple"},
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/TicketUserIdDto.java": {
 "name": "TicketUserIdDto.java",
 "summary": "购票人单条操作的请求参数 DTO，仅包含购票人 id，用于删除等按主键操作的接口。",
 "tags": ["dto", "data-model", "请求参数", "购票人"], "complexity": "simple"},
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/TicketUserListDto.java": {
 "name": "TicketUserListDto.java",
 "summary": "按用户 id 查询购票人列表的请求参数 DTO。",
 "tags": ["dto", "data-model", "请求参数", "购票人"], "complexity": "simple"},
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserAuthenticationDto.java": {
 "name": "UserAuthenticationDto.java",
 "summary": "用户实名认证的请求参数 DTO，包含用户 id、真实姓名与身份证号。",
 "tags": ["dto", "data-model", "请求参数", "实名认证"], "complexity": "simple"},
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserExistDto.java": {
 "name": "UserExistDto.java",
 "summary": "判断用户是否存在的请求参数 DTO，仅携带手机号。",
 "tags": ["dto", "data-model", "请求参数", "校验"], "complexity": "simple"},
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserGetAndTicketUserListDto.java": {
 "name": "UserGetAndTicketUserListDto.java",
 "summary": "查询用户与购票人集合的内部调用参数 DTO，仅包含用户 id，不对外暴露给前端。",
 "tags": ["dto", "data-model", "请求参数", "内部调用"], "complexity": "simple"},
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserIdDto.java": {
 "name": "UserIdDto.java",
 "summary": "通用用户 id 请求参数 DTO，被用户查询、支付、订单等多个服务的接口复用。",
 "tags": ["dto", "data-model", "请求参数", "通用参数"], "complexity": "simple"},
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserLoginDto.java": {
 "name": "UserLoginDto.java",
 "summary": "用户登录请求参数 DTO，包含渠道 code、手机号、邮箱与密码，支持手机号或邮箱两种登录方式。",
 "tags": ["dto", "data-model", "请求参数", "登录"], "complexity": "simple"},
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserLogoutDto.java": {
 "name": "UserLogoutDto.java",
 "summary": "用户退出登录的请求参数 DTO，包含渠道 code 与待失效的 token。",
 "tags": ["dto", "data-model", "请求参数", "登出"], "complexity": "simple"},
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserMobileDto.java": {
 "name": "UserMobileDto.java",
 "summary": "按手机号查询用户的请求参数 DTO。",
 "tags": ["dto", "data-model", "请求参数", "手机号"], "complexity": "simple"},
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserUpdateDto.java": {
 "name": "UserUpdateDto.java",
 "summary": "修改用户个人信息的请求参数 DTO，包含姓名、真实姓名、性别、手机号与身份证号。",
 "tags": ["dto", "data-model", "请求参数", "用户资料"], "complexity": "simple"},
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserUpdateEmailDto.java": {
 "name": "UserUpdateEmailDto.java",
 "summary": "修改用户邮箱的请求参数 DTO，包含用户 id 与新邮箱地址。",
 "tags": ["dto", "data-model", "请求参数", "邮箱"], "complexity": "simple"},
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserUpdateMobileDto.java": {
 "name": "UserUpdateMobileDto.java",
 "summary": "修改用户手机号的请求参数 DTO，包含用户 id 与新手机号。",
 "tags": ["dto", "data-model", "请求参数", "手机号"], "complexity": "simple"},
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserUpdatePasswordDto.java": {
 "name": "UserUpdatePasswordDto.java",
 "summary": "修改用户密码的请求参数 DTO，包含用户 id 与新密码。",
 "tags": ["dto", "data-model", "请求参数", "密码"], "complexity": "simple"},
"damai-server-client/damai-user-client/src/main/java/com/damai/vo/TicketUserVo.java": {
 "name": "TicketUserVo.java",
 "summary": "购票人返回 VO，在 getRelName/getIdNumber 中借助 Hutool 对真实姓名与身份证号做脱敏后再对外输出。",
 "tags": ["vo", "data-model", "脱敏", "购票人"], "complexity": "simple",
 "languageNotes": "通过覆写 getter 实现序列化时自动脱敏，避免调用方遗漏数据处理。"},
"damai-server-client/damai-user-client/src/main/java/com/damai/vo/UserGetAndTicketUserListVo.java": {
 "name": "UserGetAndTicketUserListVo.java",
 "summary": "用户与购票人集合的组合返回 VO，聚合用户信息与购票人列表供下单等场景一次查询。",
 "tags": ["vo", "data-model", "聚合结果", "购票人"], "complexity": "simple"},
"damai-server-client/damai-user-client/src/main/java/com/damai/vo/UserLoginVo.java": {
 "name": "UserLoginVo.java",
 "summary": "登录结果返回 VO，包含用户 id 与签发的 token。",
 "tags": ["vo", "data-model", "登录", "token"], "complexity": "simple"},
"damai-server/damai-user-service/src/main/java/com/damai/controller/TicketUserController.java": {
 "name": "TicketUserController.java",
 "summary": "购票人 REST 控制层，挂载 /ticket/user 路径，暴露查询列表、新增、删除三个 POST 接口并统一封装 ApiResponse。",
 "tags": ["api-handler", "controller", "入口点", "购票人"], "complexity": "simple"},
"damai-server/damai-user-service/src/main/java/com/damai/controller/UserController.java": {
 "name": "UserController.java",
 "summary": "用户服务 REST 控制层，挂载 /user 路径，提供按手机号/id 查询、注册、存在校验、登录、登出、资料/密码/邮箱/手机号修改、实名认证以及内部使用的用户购票人聚合查询共 12 个 POST 接口。",
 "tags": ["api-handler", "controller", "入口点", "用户"], "complexity": "moderate",
 "languageNotes": "全部接口使用 POST + @Valid 校验请求体，并用 Swagger3 注解 @Operation/@Tag 生成接口文档。"},
"damai-server/damai-user-service/src/main/java/com/damai/entity/TicketUser.java": {
 "name": "TicketUser.java",
 "summary": "购票人实体，映射 d_ticket_user 表，保存用户 id、真实姓名、证件类型与证件号码，公共审计字段继承自 BaseTableData。",
 "tags": ["entity", "data-model", "mybatis-plus", "购票人"], "complexity": "simple"},
"damai-server/damai-user-service/src/main/java/com/damai/entity/User.java": {
 "name": "User.java",
 "summary": "用户实体，映射 d_user 表，包含姓名、手机号、密码、邮箱、性别、实名认证与邮箱认证状态、身份证号、收货地址等字段。",
 "tags": ["entity", "data-model", "mybatis-plus", "用户"], "complexity": "simple"},
"damai-server/damai-user-service/src/main/java/com/damai/entity/UserEmail.java": {
 "name": "UserEmail.java",
 "summary": "用户邮箱实体，映射用户邮箱表，以 userId 关联用户并提供邮箱唯一性落库能力。",
 "tags": ["entity", "data-model", "mybatis-plus", "邮箱"], "complexity": "simple"},
"damai-server/damai-user-service/src/main/java/com/damai/entity/UserMobile.java": {
 "name": "UserMobile.java",
 "summary": "用户手机号实体，映射 d_user_mobile 表，以 userId 关联用户并支撑手机号登录与查重。",
 "tags": ["entity", "data-model", "mybatis-plus", "手机号"], "complexity": "simple"},
"damai-server/damai-user-service/src/main/java/com/damai/mapper/TicketUserMapper.java": {
 "name": "TicketUserMapper.java",
 "summary": "购票人 Mapper 接口，继承 MyBatis-Plus BaseMapper 获得 TicketUser 的基础 CRUD 能力。",
 "tags": ["mapper", "mybatis-plus", "数据访问", "购票人"], "complexity": "simple"},
"damai-server/damai-user-service/src/main/java/com/damai/mapper/UserEmailMapper.java": {
 "name": "UserEmailMapper.java",
 "summary": "用户邮箱 Mapper 接口，继承 MyBatis-Plus BaseMapper 获得 UserEmail 的基础 CRUD 能力。",
 "tags": ["mapper", "mybatis-plus", "数据访问", "邮箱"], "complexity": "simple"},
"damai-server/damai-user-service/src/main/java/com/damai/mapper/UserMapper.java": {
 "name": "UserMapper.java",
 "summary": "用户 Mapper 接口，继承 MyBatis-Plus BaseMapper 获得 User 的基础 CRUD 能力。",
 "tags": ["mapper", "mybatis-plus", "数据访问", "用户"], "complexity": "simple"},
"damai-server/damai-user-service/src/main/java/com/damai/mapper/UserMobileMapper.java": {
 "name": "UserMobileMapper.java",
 "summary": "用户手机号 Mapper 接口，继承 MyBatis-Plus BaseMapper 获得 UserMobile 的基础 CRUD 能力。",
 "tags": ["mapper", "mybatis-plus", "数据访问", "手机号"], "complexity": "simple"},
"damai-server/damai-user-service/src/main/java/com/damai/service/TicketUserService.java": {
 "name": "TicketUserService.java",
 "summary": "购票人业务服务，提供列表查询（Redis 缓存读写）、新增（用户与证件唯一性校验 + UidGenerator 主键）与删除，并在写操作后清理购票人列表缓存。",
 "tags": ["service", "业务逻辑", "购票人", "缓存"], "complexity": "moderate"},
"damai-server/damai-user-service/src/main/java/com/damai/service/UserMobileService.java": {
 "name": "UserMobileService.java",
 "summary": "用户手机号服务，仅继承 MyBatis-Plus ServiceImpl<UserMobileMapper, UserMobile> 以复用通用 CRUD 能力，供其他服务注入使用。",
 "tags": ["service", "mybatis-plus", "手机号", "空实现"], "complexity": "simple"},
"damai-server/damai-user-service/src/main/java/com/damai/service/UserService.java": {
 "name": "UserService.java",
 "summary": "用户核心业务服务，统一承载注册（分布式锁 + 组合校验 + 布隆过滤器）、登录（手机号/邮箱双通道、Redis 错误次数限流、JWT 签发）、登出、资料/密码/邮箱/手机号修改、实名认证、按手机号或 id 查询以及用户与购票人聚合查询。",
 "tags": ["service", "业务逻辑", "用户", "分布式锁", "认证"], "complexity": "complex",
 "languageNotes": "登录态与错误计数均落在 Redis；注册路径使用 @ServiceLock 分布式锁防止同手机号并发注册；渠道密钥通过 BaseDataClient 远程获取并带 Redis 缓存。"},
"damai-spring-cloud-framework/damai-service-common/src/main/java/com/damai/data/BaseTableData.java": {
 "name": "BaseTableData.java",
 "summary": "所有数据库实体的公共父类，统一提供 createTime（插入填充）、editTime（插入与更新填充）与逻辑删除标记 status 字段。",
 "tags": ["entity", "data-model", "公共父类", "mybatis-plus"], "complexity": "simple",
 "languageNotes": "@TableField(fill=...) 交由 MyBatis-Plus 自动填充时间字段，避免各业务表重复处理审计字段。"},
}

# ---------------- class nodes: (lineRange, summary, tags, complexity, languageNotes|None) ----------------
CLASS_META = {
"damai-server-client/damai-user-client/src/main/java/com/damai/client/UserClient.java": ("UserClient", [23, 52],
 "用户服务远程调用接口，方法上使用 @PostMapping 声明目标地址，是订单、网关等服务访问用户能力（查用户、查购票人、聚合查询）的统一入口。",
 ["feign-client", "interface", "service-client", "远程调用"], "simple", None),
"damai-server-client/damai-user-client/src/main/java/com/damai/client/UserClientFallback.java": ("UserClientFallback", [20, 37],
 "UserClient 的降级实现类，对三个远程方法一律返回 BaseCode.SYSTEM_ERROR，保证下游不可用时调用方不抛异常。",
 ["fallback", "容错", "service-client", "降级实现"], "simple", None),
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/TicketUserDto.java": ("TicketUserDto", [17, 42],
 "购票人新增参数对象，字段包括 userId、relName、idType、idNumber，实现 Serializable 以便跨服务传输。",
 ["dto", "data-model", "购票人", "序列化"], "simple", None),
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/TicketUserIdDto.java": ("TicketUserIdDto", [12, 19],
 "仅含购票人主键 id 的参数对象，用于删除等单记录操作。", ["dto", "data-model", "购票人", "主键参数"], "simple", None),
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/TicketUserListDto.java": ("TicketUserListDto", [13, 20],
 "购票人列表查询参数对象，只包含 userId，用于按用户维度查询购票人。", ["dto", "data-model", "购票人", "查询参数"], "simple", None),
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserAuthenticationDto.java": ("UserAuthenticationDto", [17, 36],
 "实名认证参数对象，携带用户 id、真实姓名与身份证号。", ["dto", "data-model", "实名认证", "参数对象"], "simple", None),
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserExistDto.java": ("UserExistDto", [16, 27],
 "用户存在性校验参数对象，仅含手机号。", ["dto", "data-model", "校验", "参数对象"], "simple", None),
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserGetAndTicketUserListDto.java": ("UserGetAndTicketUserListDto", [12, 19],
 "用户与购票人聚合查询参数对象，仅含 userId，仅供服务间内部调用。", ["dto", "data-model", "内部调用", "参数对象"], "simple", None),
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserIdDto.java": ("UserIdDto", [12, 19],
 "通用用户主键参数对象，被多个用户相关接口复用。", ["dto", "data-model", "通用参数", "主键参数"], "simple", None),
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserLoginDto.java": ("UserLoginDto", [13, 30],
 "登录参数对象，包含渠道 code、手机号、邮箱、密码，支撑手机号登录与邮箱登录两种路径。",
 ["dto", "data-model", "登录", "参数对象"], "simple", None),
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserLogoutDto.java": ("UserLogoutDto", [13, 24],
 "登出参数对象，包含渠道 code 与待解析的 token。", ["dto", "data-model", "登出", "参数对象"], "simple", None),
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserMobileDto.java": ("UserMobileDto", [12, 19],
 "手机号查询参数对象，仅含 mobile。", ["dto", "data-model", "手机号", "查询参数"], "simple", None),
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserUpdateDto.java": ("UserUpdateDto", [16, 42],
 "用户资料修改参数对象，包含 id、姓名、真实姓名、性别、手机号与身份证号。",
 ["dto", "data-model", "用户资料", "参数对象"], "simple", None),
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserUpdateEmailDto.java": ("UserUpdateEmailDto", [17, 32],
 "邮箱修改参数对象，包含用户 id 与新邮箱。", ["dto", "data-model", "邮箱", "参数对象"], "simple", None),
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserUpdateMobileDto.java": ("UserUpdateMobileDto", [17, 32],
 "手机号修改参数对象，包含用户 id 与新手机号。", ["dto", "data-model", "手机号", "参数对象"], "simple", None),
"damai-server-client/damai-user-client/src/main/java/com/damai/dto/UserUpdatePasswordDto.java": ("UserUpdatePasswordDto", [17, 32],
 "密码修改参数对象，包含用户 id 与新密码。", ["dto", "data-model", "密码", "参数对象"], "simple", None),
"damai-server-client/damai-user-client/src/main/java/com/damai/vo/TicketUserVo.java": ("TicketUserVo", [17, 53],
 "购票人返回对象，除基础字段外覆写 getRelName 与 getIdNumber，在输出时对姓名与身份证号做脱敏。",
 ["vo", "data-model", "脱敏", "购票人"], "simple", None),
"damai-server-client/damai-user-client/src/main/java/com/damai/vo/UserGetAndTicketUserListVo.java": ("UserGetAndTicketUserListVo", [13, 22],
 "聚合返回对象，组合 UserVo 与 TicketUserVo 列表，减少下单流程的远程调用次数。",
 ["vo", "data-model", "聚合结果", "内部调用"], "simple", None),
"damai-server-client/damai-user-client/src/main/java/com/damai/vo/UserLoginVo.java": ("UserLoginVo", [11, 20],
 "登录返回对象，承载用户 id 与签发的 token。", ["vo", "data-model", "登录", "token"], "simple", None),
"damai-server/damai-user-service/src/main/java/com/damai/controller/TicketUserController.java": ("TicketUserController", [25, 52],
 "购票人控制层，注入 TicketUserService 并暴露 /ticket/user/list、/add、/delete 三个 POST 接口，统一用 ApiResponse.ok 包装结果。",
 ["api-handler", "controller", "购票人", "rest-api"], "simple", None),
"damai-server/damai-user-service/src/main/java/com/damai/controller/UserController.java": ("UserController", [34, 122],
 "用户控制层，注入 UserService，提供 12 个用户相关 POST 接口（查询、注册、登录、登出、资料与凭证修改、实名认证及内部聚合查询）。",
 ["api-handler", "controller", "用户", "rest-api"], "moderate", None),
"damai-server/damai-user-service/src/main/java/com/damai/entity/TicketUser.java": ("TicketUser", [15, 46],
 "购票人持久化实体，@TableName 指定 d_ticket_user，字段含 userId、relName、idType、idNumber。",
 ["entity", "data-model", "mybatis-plus", "购票人"], "simple", None),
"damai-server/damai-user-service/src/main/java/com/damai/entity/User.java": ("User", [15, 78],
 "用户持久化实体，@TableName 指定 d_user，集中承载账号、联系方式与认证状态字段。",
 ["entity", "data-model", "mybatis-plus", "用户"], "simple", None),
"damai-server/damai-user-service/src/main/java/com/damai/entity/UserEmail.java": ("UserEmail", [15, 36],
 "用户邮箱持久化实体，以 userId 外联用户，用于保证一个邮箱只绑定一个账号。",
 ["entity", "data-model", "mybatis-plus", "邮箱"], "simple", None),
"damai-server/damai-user-service/src/main/java/com/damai/entity/UserMobile.java": ("UserMobile", [15, 36],
 "用户手机号持久化实体，@TableName 指定 d_user_mobile，支撑手机号登录与查重。",
 ["entity", "data-model", "mybatis-plus", "手机号"], "simple", None),
"damai-server/damai-user-service/src/main/java/com/damai/mapper/TicketUserMapper.java": ("TicketUserMapper", [12, 14],
 "MyBatis-Plus Mapper 接口，泛型绑定 TicketUser 实体。", ["mapper", "mybatis-plus", "数据访问", "购票人"], "simple", None),
"damai-server/damai-user-service/src/main/java/com/damai/mapper/UserEmailMapper.java": ("UserEmailMapper", [11, 13],
 "MyBatis-Plus Mapper 接口，泛型绑定 UserEmail 实体。", ["mapper", "mybatis-plus", "数据访问", "邮箱"], "simple", None),
"damai-server/damai-user-service/src/main/java/com/damai/mapper/UserMapper.java": ("UserMapper", [12, 14],
 "MyBatis-Plus Mapper 接口，泛型绑定 User 实体。", ["mapper", "mybatis-plus", "数据访问", "用户"], "simple", None),
"damai-server/damai-user-service/src/main/java/com/damai/mapper/UserMobileMapper.java": ("UserMobileMapper", [11, 13],
 "MyBatis-Plus Mapper 接口，泛型绑定 UserMobile 实体。", ["mapper", "mybatis-plus", "数据访问", "手机号"], "simple", None),
"damai-server/damai-user-service/src/main/java/com/damai/service/TicketUserService.java": ("TicketUserService", [35, 96],
 "购票人业务服务，注入 TicketUserMapper/UserMapper/UidGenerator/RedisCache，实现带缓存的列表查询、唯一性校验的新增与删除。",
 ["service", "业务逻辑", "购票人", "缓存"], "moderate", None),
"damai-server/damai-user-service/src/main/java/com/damai/service/UserMobileService.java": ("UserMobileService", [14, 17],
 "空壳服务类，仅通过继承 ServiceImpl 暴露手机号实体的通用 CRUD 方法。",
 ["service", "mybatis-plus", "手机号", "空实现"], "simple", None),
"damai-server/damai-user-service/src/main/java/com/damai/service/UserService.java": ("UserService", [74, 388],
 "用户领域核心服务，聚合 4 个 Mapper、UidGenerator、RedisCache、BloomFilterHandler、CompositeContainer 与 BaseDataClient，实现注册到认证的完整用户生命周期逻辑。",
 ["service", "业务逻辑", "用户", "分布式锁", "认证"], "complex",
 "继承 MyBatis-Plus ServiceImpl<UserMapper, User> 复用通用持久化方法，同时通过 @Transactional(rollbackFor=Exception.class) 保证注册等多表写入的一致性。"),
"damai-spring-cloud-framework/damai-service-common/src/main/java/com/damai/data/BaseTableData.java": ("BaseTableData", [13, 33],
 "实体公共父类，定义 createTime、editTime、status 三个字段并交由 MyBatis-Plus 自动填充，被用户、订单、节目等各服务实体统一继承。",
 ["entity", "data-model", "公共父类", "基础类"], "simple", None),
}

# ---------------- function nodes ----------------
FUNC_META = {
"damai-server/damai-user-service/src/main/java/com/damai/service/TicketUserService.java": {
 "list": ([50, 61], "查询购票人列表：先按 userId 读 Redis 缓存，未命中则用 LambdaQueryWrapper 查库，随后把结果转换为 TicketUserVo 列表。", ["业务逻辑", "缓存查询", "购票人"], "simple"),
 "add": ([63, 82], "新增购票人：校验目标用户存在、同证件不重复，用 UidGenerator 生成主键入库并清理该用户的购票人列表缓存。", ["业务逻辑", "唯一性校验", "购票人", "缓存失效"], "moderate"),
},
"damai-server/damai-user-service/src/main/java/com/damai/service/UserService.java": {
 "register": ([110, 128], "用户注册：先经 CompositeContainer 执行注册组合校验，再用 UidGenerator 生成 id 分别写入用户表与手机号表，并把手机号加入布隆过滤器。", ["业务逻辑", "注册", "组合校验", "布隆过滤器"], "moderate"),
 "doExist": ([135, 145], "手机号存在性判断：先查布隆过滤器快速拦截，疑似存在时再查手机号表，命中则抛 DaMaiFrameException。", ["业务逻辑", "布隆过滤器", "校验"], "simple"),
 "login": ([152, 204], "用户登录主流程：支持手机号或邮箱两种方式，用 Redis 记录并限制连续密码错误次数，校验通过后写入登录缓存并签发携带渠道密钥的 JWT token。", ["业务逻辑", "登录", "JWT", "限流"], "complex"),
 "logout": ([226, 235], "退出登录：用渠道密钥解析 token 取出用户信息，删除 Redis 中对应的登录态缓存。", ["业务逻辑", "登出", "token", "缓存"], "simple"),
 "update": ([245, 254], "修改用户基本资料：校验用户存在后拷贝字段更新 User 记录。", ["业务逻辑", "用户资料", "更新"], "simple"),
 "updatePassword": ([255, 264], "修改密码：校验用户存在后更新密码字段。", ["业务逻辑", "密码", "更新"], "simple"),
 "updateEmail": ([265, 293], "修改邮箱：更新用户邮箱与邮箱认证状态，并按邮箱是否已存在决定插入或更新用户邮箱表。", ["业务逻辑", "邮箱", "数据一致性"], "moderate"),
 "updateMobile": ([295, 321], "修改手机号：更新用户手机号，同时保证手机号表记录唯一（存在则更新、不存在则插入）。", ["业务逻辑", "手机号", "数据一致性"], "moderate"),
 "authentication": ([323, 338], "实名认证：校验用户存在且尚未认证，写入真实姓名、身份证号并将实名认证状态置为已认证。", ["业务逻辑", "实名认证", "校验"], "simple"),
 "getByMobile": ([340, 355], "按手机号查询用户：经手机号表取得 userId 后查询用户表，回填手机号并转换为 UserVo 返回。", ["业务逻辑", "查询", "手机号"], "simple"),
 "getUserAndTicketUserList": ([367, 381], "聚合查询用户信息与购票人列表并组装 UserGetAndTicketUserListVo，供订单等内部服务一次性获取下单所需数据。", ["业务逻辑", "聚合查询", "内部调用"], "simple"),
},
}

nodes = []
edges = []
node_ids = set()

def add_node(n):
    assert n["id"] not in node_ids, n["id"]
    node_ids.add(n["id"])
    nodes.append(n)

# file + class + function nodes
for bf in inp["batchFiles"]:
    p = bf["path"]
    meta = FILE_META[p]
    ntype = {"code": "file"}.get(bf["fileCategory"], "file")
    add_node({"id": f(p).replace("file:", ntype + ":"), "type": ntype, "name": meta["name"],
              "filePath": p, "summary": meta["summary"], "tags": meta["tags"], "complexity": meta["complexity"],
              **({"languageNotes": meta["languageNotes"]} if meta.get("languageNotes") else {})})
    if p in CLASS_META:
        cname, rng, summ, tags, cx, ln = CLASS_META[p]
        add_node({"id": f"class:{p}:{cname}", "type": "class", "name": cname, "filePath": p,
                  "lineRange": rng, "summary": summ, "tags": tags, "complexity": cx,
                  **({"languageNotes": ln} if ln else {})})
    for fname, (rng, summ, tags, cx) in FUNC_META.get(p, {}).items():
        add_node({"id": f"function:{p}:{fname}", "type": "function", "name": fname, "filePath": p,
                  "lineRange": rng, "summary": summ, "tags": tags, "complexity": cx})

# contains / exports
for bf in inp["batchFiles"]:
    p = bf["path"]
    if p in CLASS_META:
        cname = CLASS_META[p][0]
        edges.append({"source": f(p), "target": f"class:{p}:{cname}", "type": "contains", "direction": "forward", "weight": 1.0})
        edges.append({"source": f(p), "target": f"class:{p}:{cname}", "type": "exports", "direction": "forward", "weight": 0.8})
    for fname in FUNC_META.get(p, {}):
        edges.append({"source": f(p), "target": f"function:{p}:{fname}", "type": "contains", "direction": "forward", "weight": 1.0})

# imports (1:1 with batchImportData)
import_total = 0
for p, targets in imp.items():
    for t in targets:
        import_total += 1
        assert t != p, p
        edges.append({"source": f(p), "target": f(t), "type": "imports", "direction": "forward", "weight": 0.7})

# inherits: entities -> BaseTableData
BASE = "damai-spring-cloud-framework/damai-service-common/src/main/java/com/damai/data/BaseTableData.java"
for e in ["TicketUser", "User", "UserEmail", "UserMobile"]:
    p = f"damai-server/damai-user-service/src/main/java/com/damai/entity/{e}.java"
    edges.append({"source": f"class:{p}:{e}", "target": f"class:{BASE}:BaseTableData", "type": "inherits", "direction": "forward", "weight": 0.9})

# implements: fallback -> feign interface
UC = "damai-server-client/damai-user-client/src/main/java/com/damai/client/UserClient.java"
UCF = "damai-server-client/damai-user-client/src/main/java/com/damai/client/UserClientFallback.java"
edges.append({"source": f"class:{UCF}:UserClientFallback", "target": f"class:{UC}:UserClient", "type": "implements", "direction": "forward", "weight": 0.9})

# calls
def call(s, t):
    assert s != t
    edges.append({"source": s, "target": t, "type": "calls", "direction": "forward", "weight": 0.8})

API = "damai-common/src/main/java/com/damai/common/ApiResponse.java"
STRU = "damai-common/src/main/java/com/damai/util/StringUtil.java"
TOKEN = "damai-common/src/main/java/com/damai/jwt/TokenUtil.java"
BLOOM = "damai-redisson-framework/damai-redisson-service-framework/damai-bloom-filter-framework/src/main/java/com/damai/handler/BloomFilterHandler.java"
CONT = "damai-spring-cloud-framework/damai-service-initialize/src/main/java/com/damai/initialize/impl/composite/CompositeContainer.java"
RKB = "damai-redis-tool-framework/damai-redis-framework/src/main/java/com/damai/redis/RedisKeyBuild.java"
TUC = "damai-server/damai-user-service/src/main/java/com/damai/controller/TicketUserController.java"
UCT = "damai-server/damai-user-service/src/main/java/com/damai/controller/UserController.java"
TUS = "damai-server/damai-user-service/src/main/java/com/damai/service/TicketUserService.java"
US = "damai-server/damai-user-service/src/main/java/com/damai/service/UserService.java"
TUVO = "damai-server-client/damai-user-client/src/main/java/com/damai/vo/TicketUserVo.java"

call(f(UCF), f"function:{API}:error")
call(f(TUC), f"function:{API}:ok")
call(f(TUC), f"function:{TUS}:list")
call(f(TUC), f"function:{TUS}:add")
for m in ["register", "login", "logout", "update", "updatePassword", "updateEmail", "updateMobile", "authentication", "getByMobile", "getUserAndTicketUserList"]:
    call(f(UCT), f"function:{US}:{m}")
call(f(UCT), f"function:{API}:ok")
call(f(TUVO), f"function:{STRU}:isNotEmpty")
call(f(US), f"function:{TOKEN}:createToken")
call(f(US), f"function:{TOKEN}:parseToken")
call(f(US), f"function:{BLOOM}:add")
call(f(US), f"function:{BLOOM}:contains")
call(f(US), f"function:{CONT}:execute")
call(f(US), f"function:{RKB}:createRedisKey")

# ---------------- split ----------------
paths = sorted(bf["path"] for bf in inp["batchFiles"])
N = len(paths)
parts = math.ceil(max(len(nodes) / 60, len(edges) / 120))
chunk = math.ceil(N / parts)
groups = [set(paths[i:i + chunk]) for i in range(0, N, chunk)]
while len(groups) > parts:
    groups[-2] |= groups[-1]
    groups.pop()

def owner(nid, node_by_id):
    if nid in node_by_id:
        return node_by_id[nid].get("filePath")
    for pref in ("file:", "class:", "function:", "config:", "document:", "service:", "table:", "endpoint:", "pipeline:", "schema:", "resource:"):
        if nid.startswith(pref):
            rest = nid[len(pref):]
            for p in paths:
                if rest.startswith(p):
                    return p
    return None

node_by_id = {n["id"]: n for n in nodes}
edges_by_owner = {p: [] for p in paths}
unassigned = []
for e in edges:
    o = owner(e["source"], node_by_id)
    if o is None:
        unassigned.append(e)
    else:
        edges_by_owner[o].append(e)

written = []
sizes = []
for g in groups:
    sizes.append((len([n for n in nodes if n.get("filePath") in g]),
                  len([e for p in g for e in edges_by_owner[p]])))
while not all(a <= 60 and b <= 120 for a, b in sizes):
    parts += 1
    chunk = math.ceil(N / parts)
    groups = [set(paths[i:i + chunk]) for i in range(0, N, chunk)]
    while len(groups) > parts:
        groups[-2] |= groups[-1]
        groups.pop()
    sizes = []
    for g in groups:
        sizes.append((len([n for n in nodes if n.get("filePath") in g]),
                      len([e for p in g for e in edges_by_owner[p]])))

for k, g in enumerate(groups, 1):
    pn = [n for n in nodes if n.get("filePath") in g]
    pe = []
    for p in g:
        pe.extend(edges_by_owner[p])
    out = {"nodes": pn, "edges": pe}
    assert len(pn) <= 60 and len(pe) <= 120, (k, len(pn), len(pe))
    fn = f"{UA}/intermediate/batch-{BATCH}-part-{k}.json" if len(groups) > 1 else f"{UA}/intermediate/batch-{BATCH}.json"
    json.dump(out, open(fn, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    written.append((fn, len(pn), len(pe)))

print("parts:", len(groups), "chunkSize:", chunk)
for w in written:
    print(w[0], "nodes=", w[1], "edges=", w[2])
print("totalNodes:", len(nodes), "totalEdges:", len(edges), "importEdges:", import_total,
      "expectedImports:", sum(len(v) for v in imp.values()))
print("unassignedEdges:", unassigned)
