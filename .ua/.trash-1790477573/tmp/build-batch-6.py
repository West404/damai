# -*- coding: utf-8 -*-
import json, math, os

ROOT = "E:/WorkSpace/damai"
BATCH_IN = os.path.join(ROOT, ".ua/intermediate/batch-input-6.json")
OUT_DIR = os.path.join(ROOT, ".ua/intermediate")

b = json.load(open(BATCH_IN, encoding="utf-8"))
imports = b["batchImportData"]

# ---------------- paths ----------------
P1M = "damai-server/damai-base-data-service/src/main/java/com/damai/mapper/ChannelDataMapper.java"
P1S = "damai-server/damai-base-data-service/src/main/java/com/damai/service/ChannelDataService.java"
G = "damai-server/damai-gateway-service/src/main/java/com/damai/"
P3 = G + "filter/RequestValidationFilter.java"
P4 = G + "filter/ResponseValidationFilter.java"
P5 = G + "pro/limit/RateLimiter.java"
P6 = G + "pro/limit/RateLimiterProperty.java"
P7 = G + "property/GatewayProperty.java"
P8 = G + "service/ApiRestrictService.java"
P9 = G + "service/ChannelDataService.java"
P10 = G + "service/TokenService.java"
P11 = G + "vo/UserVo.java"
P12 = "damai-server/damai-pay-service/src/main/java/com/damai/pay/PayStrategyContext.java"
P13 = "damai-server/damai-program-service/src/main/java/com/damai/service/composite/ProgramBloomFilterCheckHandler.java"
P14 = "damai-server/damai-program-service/src/main/java/com/damai/service/composite/ProgramRecommendCheckHandler.java"
P15 = "damai-server/damai-program-service/src/main/java/com/damai/service/strategy/ProgramOrderContext.java"
U = "damai-server/damai-user-service/src/main/java/com/damai/service/"
P16 = U + "TokenService.java"
P17 = U + "composite/register/AbstractUserRegisterCheckHandler.java"
P18 = U + "composite/register/impl/UserExistCheckHandler.java"
P19 = U + "composite/register/impl/UserRegisterCountCheckHandler.java"
P20 = U + "composite/register/impl/UserRegisterVerifyCaptcha.java"
P21 = U + "tool/RequestCounter.java"
P22 = "damai-spring-cloud-framework/damai-service-common/src/main/java/com/damai/shardingsphere/DatabaseOrderComplexGeneArithmetic.java"
P23 = "damai-spring-cloud-framework/damai-service-common/src/main/java/com/damai/shardingsphere/TableOrderComplexGeneArithmetic.java"
P24 = "damai-spring-cloud-framework/damai-service-component/src/main/java/com/damai/feign/FeignRequestInterceptor.java"
P25 = "damai-spring-cloud-framework/damai-service-initialize/src/main/java/com/damai/initialize/impl/composite/AbstractComposite.java"
P26 = "damai-thread-pool-framework/src/main/java/com/damai/filter/RequestParamContextFilter.java"

DATEUTILS = "damai-common/src/main/java/com/damai/util/DateUtils.java"
REDISKEYBUILD = "damai-redis-tool-framework/damai-redis-framework/src/main/java/com/damai/redis/RedisKeyBuild.java"
STRINGUTIL = "damai-common/src/main/java/com/damai/util/StringUtil.java"
RSASIGN = "damai-common/src/main/java/com/damai/util/RsaSignTool.java"
RSATOOL = "damai-common/src/main/java/com/damai/util/RsaTool.java"
TOKENUTIL = "damai-common/src/main/java/com/damai/jwt/TokenUtil.java"
SENDMSG = G + "kafka/ApiDataMessageSend.java"
APIRESTRICTOP = G + "service/lua/ApiRestrictCacheOperate.java"
BLOOM = "damai-redisson-framework/damai-redisson-service-framework/damai-bloom-filter-framework/src/main/java/com/damai/handler/BloomFilterHandler.java"
USERSERVICE = U + "UserService.java"
CAPTCHA = "damai-captcha-manage-framework/damai-captcha-framework/src/main/java/com/damai/service/CaptchaHandle.java"

nodes = []
edges = []


def fnode(path, summary, tags, complexity, notes=None):
    n = {"id": "file:" + path, "type": "file", "name": os.path.basename(path),
         "filePath": path, "summary": summary, "tags": tags, "complexity": complexity}
    if notes:
        n["languageNotes"] = notes
    nodes.append(n)


def cnode(path, name, rng, summary, tags, complexity):
    nodes.append({"id": "class:%s:%s" % (path, name), "type": "class", "name": name,
                  "filePath": path, "lineRange": list(rng), "summary": summary,
                  "tags": tags, "complexity": complexity})
    edges.append({"source": "file:" + path, "target": "class:%s:%s" % (path, name),
                  "type": "contains", "direction": "forward", "weight": 1.0})


def mnode(path, name, rng, summary, tags, complexity):
    nodes.append({"id": "function:%s:%s" % (path, name), "type": "function", "name": name,
                  "filePath": path, "lineRange": list(rng), "summary": summary,
                  "tags": tags, "complexity": complexity})
    edges.append({"source": "file:" + path, "target": "function:%s:%s" % (path, name),
                  "type": "contains", "direction": "forward", "weight": 1.0})


def e(src, tgt, kind, weight):
    edges.append({"source": src, "target": tgt, "type": kind, "direction": "forward", "weight": weight})


def cid(path, name):
    return "class:%s:%s" % (path, name)


def fid(path, name):
    return "function:%s:%s" % (path, name)


# ============ PART 1 : files 1-9 ============
fnode(P1M, "渠道数据表 ChannelTableData 的 MyBatis-Plus Mapper 接口，为渠道数据业务提供基础持久化能力。",
      ["data-access", "mybatis-plus", "mapper", "channel-data"], "simple",
      "空接口继承 BaseMapper，SQL 由 MyBatis-Plus 反射生成，未定义自定义方法。")
cnode(P1M, "ChannelDataMapper", (12, 14),
      "渠道数据 Mapper 接口，直接复用 MyBatis-Plus 通用 CRUD 方法操作渠道表。",
      ["mapper", "mybatis-plus", "data-access"], "simple")

fnode(P1S, "base-data 服务的渠道数据业务实现：按 code 查询启用状态的渠道信息，新增渠道并同步写入 Redis 缓存。",
      ["service", "channel-data", "redis-cache", "mybatis-plus"], "moderate")
cnode(P1S, "ChannelDataService", (29, 68),
      "渠道数据服务实现类，组合 Mapper、UidGenerator 与 RedisCache 完成渠道数据的查询、落库与缓存同步。",
      ["service", "channel-data", "cache"], "moderate")
mnode(P1S, "getByCode", (42, 51),
      "根据渠道 code 查询状态为启用的渠道记录并转换为 VO 返回。",
      ["query", "channel-data"], "simple")
mnode(P1S, "add", (53, 61),
      "新增渠道数据：生成分布式 ID、写入数据库并刷新 Redis 中的渠道配置缓存。",
      ["mutation", "channel-data", "id-generator"], "simple")

fnode(P3, "网关统一请求校验过滤器，串行执行限流、请求体缓存、RSA 验签、token 鉴权与 API 规则拦截，并把用户身份透传给下游服务。",
      ["api-handler", "middleware", "validation", "security", "限流"], "complex",
      "基于 Spring Cloud Gateway 的 GlobalFilter + Ordered，用 Reactor 缓存请求体后重写 header 完成 V1/V2 双协议验签。")
cnode(P3, "RequestValidationFilter", (73, 326),
      "网关请求校验过滤器主体，实现 GlobalFilter 与 Ordered，编排限流、验签、鉴权与规则拦截逻辑。",
      ["gateway", "filter", "validation", "security"], "complex")
mnode(P3, "filter", (102, 117),
      "过滤器入口，按限流开关获取信号量许可，异常时释放许可并返回限流业务异常。",
      ["rate-limiting", "entry-point"], "simple")
mnode(P3, "doFilter", (119, 149),
      "注入 traceId、构建透传 header 与线程上下文，并按 Content-Type 决定是否读取请求体。",
      ["middleware", "context-propagation"], "moderate")
mnode(P3, "readBody", (151, 173),
      "缓存请求体并重新包装 ServerHttpRequest，把处理后的 header 与请求体继续向下游传递。",
      ["reactor", "request-body"], "moderate")
mnode(P3, "execute", (175, 182),
      "把客户端请求参数封装进 RequestTemporaryWrapper，供后续验签阶段读取。",
      ["helper", "serialization"], "simple")
mnode(P3, "doExecute", (184, 251),
      "核心校验流程：解析请求体、校验渠道签名、token 鉴权、用户身份校验与 API 规则拦截。",
      ["validation", "security", "orchestration"], "complex")
mnode(P3, "decorateHead", (255, 286),
      "重写转发请求的 header（traceId、userId 与内容长度），保证下游拿到正确上下文。",
      ["header", "middleware"], "moderate")
mnode(P3, "skipCheckToken", (293, 301),
      "基于 AntPathMatcher 判断当前 URL 是否命中免 token 校验白名单。",
      ["whitelist", "utility"], "simple")
mnode(P3, "skipCheckParameter", (303, 311),
      "基于 AntPathMatcher 判断当前 URL 是否跳过参数签名校验。",
      ["whitelist", "utility"], "simple")

fnode(P4, "网关响应校验过滤器，对返回体按渠道用 RSA 公钥加密 data 字段，保障渠道数据回传安全。",
      ["api-handler", "gateway", "security", "serialization"], "moderate")
cnode(P4, "ResponseValidationFilter", (48, 140),
      "响应侧过滤器，装饰响应体并在写出前对渠道报文做加签/加密处理。",
      ["gateway", "filter", "security"], "moderate")
mnode(P4, "filter", (63, 66),
      "过滤器入口，装饰响应对象后继续执行过滤链。",
      ["entry-point", "filter"], "simple")
mnode(P4, "decorate", (69, 120),
      "构造 ServerHttpResponseDecorator，缓存响应体并在写回前完成加密处理。",
      ["reactor", "response-body"], "complex")
mnode(P4, "checkResponseBody", (122, 139),
      "按渠道公钥加密响应 data 字段，写回密文后返回新的响应体。",
      ["encryption", "rsa", "response-body"], "moderate")

fnode(P5, "基于 Semaphore 的本地限流器，为网关请求提供单位时间内的许可获取与释放能力。",
      ["utility", "rate-limiting", "concurrency"], "simple")
cnode(P5, "RateLimiter", (14, 33),
      "信号量实现的单机限流器，限制单位时间内的并发请求数。",
      ["rate-limiting", "concurrency", "限流"], "simple")
mnode(P5, "acquire", (24, 28),
      "尝试获取一个许可，获取失败时抛出限流业务异常。",
      ["rate-limiting", "concurrency"], "simple")

fnode(P6, "网关限流配置属性类，读取限流开关与每秒许可数配置。",
      ["configuration", "properties", "rate-limiting"], "simple")
cnode(P6, "RateLimiterProperty", (11, 19),
      "限流配置属性对象，包含 rateSwitch 开关与 ratePermits 许可数。",
      ["configuration", "properties"], "simple")

fnode(P7, "网关业务配置属性类，维护限流路径、token 校验白名单、参数校验跳过路径与需要 userId 的接口路径。",
      ["configuration", "properties", "gateway"], "simple")
cnode(P7, "GatewayProperty", (12, 35),
      "网关路径规则配置对象，供过滤器与 API 规则服务读取路径匹配规则。",
      ["configuration", "routing"], "simple")

fnode(P8, "网关 API 规则限制服务：从 Redis 读取规则与深度规则参数，通过 Lua 脚本在时间窗口内统计调用量并拦截，同时上报 API 调用数据。",
      ["service", "api-handler", "redis", "lua", "限流"], "complex")
cnode(P8, "ApiRestrictService", (44, 268),
      "API 规则拦截核心服务，负责规则参数组装、客户端 IP 解析与调用数据上报。",
      ["gateway", "rate-limiting", "rule-engine"], "complex")
mnode(P8, "checkApiRestrict", (63, 73),
      "判断请求 URI 是否命中需要 API 规则限制的路径。",
      ["routing", "whitelist"], "simple")
mnode(P8, "apiRestrict", (75, 152),
      "执行 API 规则拦截：读取规则与深度规则、调用 Lua 脚本统计调用次数、命中后保存调用数据并抛出异常。",
      ["rate-limiting", "rule-engine", "redis-lua"], "complex")
mnode(P8, "getRuleParameter", (154, 173),
      "把规则 VO 转换为 Lua 脚本所需参数（统计窗口、阈值、生效时间与 Redis key）。",
      ["parameter", "redis"], "moderate")
mnode(P8, "getDepthRuleParameter", (175, 204),
      "组装深度规则参数列表（多时间窗口），供 Lua 脚本批量计算。",
      ["parameter", "rule-engine"], "moderate")
mnode(P8, "sortStartTimeWindow", (206, 211),
      "计算并排序深度规则的起止时间窗口时间戳。",
      ["sorting", "date-time"], "simple")
mnode(P8, "getTimeWindowTimestamp", (213, 216),
      "把 HH:mm:ss 形式的时间窗口转换为当日时间戳。",
      ["date-time", "utility"], "simple")
mnode(P8, "getIpAddress", (223, 253),
      "依次从多个代理请求头解析客户端真实 IP，兜底使用 RemoteAddress。",
      ["network", "utility"], "moderate")
mnode(P8, "saveApiData", (255, 267),
      "封装 API 调用数据 DTO（ID、IP、URL、多级时间与类型）并发送到 Kafka 上报。",
      ["kafka", "reporting", "dto"], "moderate")

fnode(P9, "网关侧渠道数据服务：优先读 Redis 缓存，未命中时经线程池 + Feign 调用 base-data 服务获取渠道配置并回填缓存。",
      ["service", "feign-client", "cache", "redis", "async"], "moderate")
cnode(P9, "ChannelDataService", (36, 105),
      "渠道配置查询服务，封装缓存读写、远程调用与异常兜底逻辑。",
      ["gateway", "cache", "feign-client"], "moderate")
mnode(P9, "checkCode", (52, 61),
      "校验渠道 code 非空，否则抛出参数异常。",
      ["validation", "utility"], "simple")
mnode(P9, "getChannelDataByCode", (63, 71),
      "按 code 先查缓存再查远程，返回渠道配置 VO。",
      ["cache", "query"], "simple")
mnode(P9, "getChannelDataByClient", (81, 104),
      "通过线程池提交 Feign 调用获取渠道配置，处理超时与业务失败异常。",
      ["feign-client", "async", "error-handling"], "moderate")

# ============ PART 2 : files 10-18 ============
fnode(P10, "网关 token 服务：解析 JWT 取出用户标识，再从 Redis 读取登录用户信息，供网关鉴权与身份透传。",
      ["service", "jwt", "authentication", "redis"], "simple")
cnode(P10, "TokenService", (23, 45),
      "网关 token 解析与用户加载服务。",
      ["gateway", "authentication", "jwt"], "simple")
mnode(P10, "parseToken", (29, 35),
      "解析 token 字符串得到用户 JSON 中的用户标识。",
      ["jwt", "parsing"], "simple")
mnode(P10, "getUser", (37, 44),
      "根据 token 与渠道密钥从 Redis 获取用户 VO，不存在时抛出鉴权异常。",
      ["authentication", "redis"], "simple")

fnode(P11, "网关模块的用户视图对象，承载鉴权后透传的用户基础字段（id、手机号、状态等）。",
      ["data-model", "vo", "user"], "simple")
cnode(P11, "UserVo", (12, 30),
      "网关侧用户 VO，字段与用户服务返回结构保持一致。",
      ["data-model", "vo"], "simple")

fnode(P12, "支付策略上下文，按支付渠道注册并查找支付策略处理器，实现支付方式的策略模式分发。",
      ["service", "strategy", "payment", "context"], "simple")
cnode(P12, "PayStrategyContext", (15, 27),
      "支付策略注册与查找上下文，内部维护渠道到策略处理器的映射。",
      ["strategy", "factory", "payment"], "simple")
mnode(P12, "get", (23, 26),
      "按支付渠道获取对应策略处理器，不存在时抛出业务异常。",
      ["strategy", "lookup"], "simple")

fnode(P13, "节目布隆过滤器校验处理器，在详情查询前判断节目 ID 是否存在于布隆过滤器，拦截不存在的节目以避免缓存穿透。",
      ["api-handler", "composite", "bloom-filter", "validation"], "simple")
cnode(P13, "ProgramBloomFilterCheckHandler", (17, 50),
      "节目详情查询前的布隆过滤器校验处理器，命中失败即抛出业务异常。",
      ["composite", "validation", "bloom-filter"], "simple")
mnode(P13, "execute", (23, 29),
      "校验节目 ID 是否存在于布隆过滤器中，不存在则抛出业务异常。",
      ["validation", "bloom-filter"], "simple")

fnode(P14, "节目推荐查询校验处理器，校验区域、父分类与节目 ID 参数是否齐全，缺参抛出业务异常。",
      ["api-handler", "composite", "validation", "recommendation"], "simple")
cnode(P14, "ProgramRecommendCheckHandler", (17, 48),
      "推荐列表查询的参数完整性校验处理器。",
      ["composite", "validation", "recommendation"], "simple")
mnode(P14, "execute", (20, 27),
      "校验推荐查询参数完整性（areaId、parentProgramCategoryId、programId）。",
      ["validation", "parameter"], "simple")

fnode(P15, "节目下单策略上下文，按版本号注册并获取对应下单策略（V1–V4），支撑下单流程的渐进式演进。",
      ["service", "strategy", "order", "context"], "simple")
cnode(P15, "ProgramOrderContext", (19, 43),
      "下单策略注册与查找上下文，供下单接口按版本选择策略实现。",
      ["strategy", "context", "order"], "simple")
mnode(P15, "init", (32, 37),
      "启动时把各版本下单策略按 version 注册到本地 Map。",
      ["initialization", "strategy"], "simple")
mnode(P15, "get", (39, 42),
      "按版本号获取下单策略实现，不存在时抛出业务异常。",
      ["strategy", "lookup"], "simple")

fnode(P16, "用户服务 token 服务：使用固定密钥解析 JWT 并从 Redis 读取登录用户信息，复用网关模块的 UserVo 保持字段一致。",
      ["service", "jwt", "authentication", "redis"], "simple")
cnode(P16, "TokenService", (23, 47),
      "用户服务侧的 token 解析与用户加载服务。",
      ["authentication", "jwt", "user-service"], "simple")
mnode(P16, "parseToken", (31, 37),
      "解析 token 得到用户 JSON 中的用户标识。",
      ["jwt", "parsing"], "simple")
mnode(P16, "getUser", (39, 46),
      "根据 token 与固定密钥从 Redis 获取用户 VO，不存在时抛出鉴权异常。",
      ["authentication", "redis"], "simple")

fnode(P17, "用户注册校验处理器抽象基类，统一返回 USER_REGISTER_CHECK 校验类型并继承组合处理器基础能力。",
      ["abstract-class", "composite", "validation", "register"], "simple")
cnode(P17, "AbstractUserRegisterCheckHandler", (14, 20),
      "注册校验处理器基类，定义注册校验类型并复用 AbstractComposite 的执行顺序能力。",
      ["abstract-class", "composite", "register"], "simple")

fnode(P18, "注册流程中的用户存在性校验处理器，调用 UserService 判断手机号是否已注册。",
      ["api-handler", "composite", "validation", "register"], "simple")
cnode(P18, "UserExistCheckHandler", (14, 39),
      "注册校验链中的手机号存在性校验节点。",
      ["composite", "validation", "register"], "simple")
mnode(P18, "execute", (20, 23),
      "校验手机号是否已存在，已存在则抛出注册异常。",
      ["validation", "register"], "simple")

# ============ PART 3 : files 19-26 ============
fnode(P19, "注册流程中的频率校验处理器，用本地请求计数器限制短时间内的注册请求量，超限抛异常。",
      ["api-handler", "composite", "rate-limiting", "register"], "simple")
cnode(P19, "UserRegisterCountCheckHandler", (16, 44),
      "注册校验链中的请求频率校验节点，依赖 RequestCounter 做秒级限流。",
      ["composite", "rate-limiting", "register"], "simple")
mnode(P19, "execute", (22, 28),
      "调用本地计数器判断注册请求是否超限，超限则抛出业务异常。",
      ["rate-limiting", "validation"], "simple")

fnode(P20, "注册流程中的验证码校验处理器：校验两次密码一致性、验证码缓存存在性，并调用验证码组件完成行为验证。",
      ["api-handler", "composite", "captcha", "validation"], "moderate")
cnode(P20, "UserRegisterVerifyCaptcha", (24, 73),
      "注册校验链中的验证码与密码一致性校验节点。",
      ["composite", "captcha", "validation"], "moderate")
mnode(P20, "execute", (34, 57),
      "校验密码一致性、验证码缓存与行为验证结果，任一失败抛出注册异常。",
      ["captcha", "validation", "register"], "moderate")

fnode(P21, "基于原子计数器的本地秒级请求限流工具，统计每秒请求量并在超过阈值时返回失败。",
      ["utility", "rate-limiting", "concurrency", "counter"], "simple")
cnode(P21, "RequestCounter", (14, 39),
      "秒级窗口的本地请求计数器，用 AtomicLong 统计并限制请求量。",
      ["rate-limiting", "concurrency", "utility"], "simple")
mnode(P21, "onRequest", (23, 38),
      "按秒窗口累加请求计数，超过最大阈值时重置计数并返回 false。",
      ["rate-limiting", "concurrency"], "moderate")

fnode(P22, "ShardingSphere 分库基因算法：从订单号与用户 ID 拼接的二进制基因中截取库基因位，计算订单应落入的数据库下标。",
      ["infrastructure", "sharding", "shardingsphere", "algorithm"], "moderate",
      "实现 ShardingSphere 的 ComplexKeysShardingAlgorithm，用订单号 + userId 的二进制基因做分库路由。")
cnode(P22, "DatabaseOrderComplexGeneArithmetic", (21, 96),
      "订单库基因分片算法，根据拼接键的基因位决定目标数据库。",
      ["sharding", "shardingsphere", "algorithm"], "moderate")
mnode(P22, "init", (30, 34),
      "从分片配置读取分库数量与分表数量。",
      ["configuration", "initialization"], "simple")
mnode(P22, "doSharding", (40, 70),
      "组合订单号与 userId 基因计算库下标，返回命中的实际库名列表。",
      ["sharding", "routing"], "complex")
mnode(P22, "calculateDatabaseIndex", (80, 91),
      "从二进制基因串截取并哈希得到数据库下标，异常时抛出分片失败错误。",
      ["sharding", "algorithm"], "moderate")

fnode(P23, "ShardingSphere 分表基因算法：依据订单号与用户 ID 基因计算订单表下标，完成订单表的分表路由。",
      ["infrastructure", "sharding", "shardingsphere", "algorithm"], "moderate")
cnode(P23, "TableOrderComplexGeneArithmetic", (20, 60),
      "订单表基因分片算法，根据订单号与 userId 基因选择实际订单表。",
      ["sharding", "shardingsphere", "algorithm"], "moderate")
mnode(P23, "init", (27, 30),
      "从分片配置读取分表数量。",
      ["configuration", "initialization"], "simple")
mnode(P23, "doSharding", (37, 59),
      "根据订单号与 userId 基因计算目标订单表名列表。",
      ["sharding", "routing"], "complex")

fnode(P24, "Feign 请求拦截器，把当前请求上下文中的灰度、用户与链路 header 透传到下游微服务调用。",
      ["middleware", "feign-client", "interceptor", "gray-release"], "moderate")
cnode(P24, "FeignRequestInterceptor", (26, 53),
      "Feign 请求模板拦截器，负责跨服务 header 透传与灰度标记传递。",
      ["feign-client", "interceptor"], "moderate")
mnode(P24, "apply", (32, 52),
      "从当前 HTTP 请求取出灰度与用户 header 并写入 Feign 请求模板。",
      ["feign-client", "header", "context-propagation"], "moderate")

fnode(P25, "组合校验抽象基类：定义执行顺序与校验类型契约，并支持按层级 BFS 顺序批量执行子处理器。",
      ["abstract-class", "composite", "design-pattern", "orchestration"], "moderate")
cnode(P25, "AbstractComposite", (14, 85),
      "组合校验基类，声明 execute/优先级契约并实现层级化批量执行。",
      ["abstract-class", "composite", "orchestration"], "moderate")
mnode(P25, "allExecute", (64, 84),
      "以队列按层级顺序执行所有子组合处理器，保证父级先于子级完成校验。",
      ["orchestration", "bfs", "composite"], "complex")

fnode(P26, "Servlet 请求过滤器，把请求头中的链路/用户参数写入 MDC 并在请求结束后清理，便于线程池与日志追踪。",
      ["middleware", "filter", "logging", "context-propagation"], "simple")
cnode(P26, "RequestParamContextFilter", (22, 35),
      "Web 过滤器，负责把请求头上下文注入 MDC 并保证清理。",
      ["filter", "logging", "mdc"], "simple")
mnode(P26, "doFilterInternal", (23, 34),
      "从请求头提取链路参数写入 MDC，执行过滤链后清理上下文。",
      ["filter", "logging", "context-propagation"], "simple")

# ============ explicit extra edges ============
# calls (part 1)
e(fid(P1S, "add"), fid(DATEUTILS, "now"), "calls", 0.8)
e(fid(P1S, "add"), fid(REDISKEYBUILD, "createRedisKey"), "calls", 0.8)
e(fid(P3, "doExecute"), fid(STRINGUTIL, "isEmpty"), "calls", 0.8)
e(fid(P3, "doExecute"), fid(P9, "getChannelDataByCode"), "calls", 0.8)
e(fid(P3, "doExecute"), fid(P10, "getUser"), "calls", 0.8)
e(fid(P3, "doExecute"), fid(P8, "apiRestrict"), "calls", 0.8)
e(fid(P3, "doExecute"), fid(RSASIGN, "verifyRsaSign256"), "calls", 0.8)
e(fid(P3, "doExecute"), fid(RSATOOL, "decrypt"), "calls", 0.8)
e(fid(P4, "checkResponseBody"), fid(P9, "getChannelDataByCode"), "calls", 0.8)
e(fid(P4, "checkResponseBody"), fid(RSATOOL, "encrypt"), "calls", 0.8)
e(fid(P8, "apiRestrict"), fid(APIRESTRICTOP, "apiRuleOperate"), "calls", 0.8)
e(fid(P8, "saveApiData"), fid(SENDMSG, "sendMessage"), "calls", 0.8)
e(fid(P9, "getChannelDataByClient"), fid(STRINGUTIL, "isEmpty"), "calls", 0.8)

# calls (part 2)
e(fid(P10, "parseToken"), fid(TOKENUTIL, "parseToken"), "calls", 0.8)
e(fid(P10, "getUser"), fid(REDISKEYBUILD, "createRedisKey"), "calls", 0.8)
e(fid(P13, "execute"), fid(BLOOM, "contains"), "calls", 0.8)
e(fid(P16, "parseToken"), fid(TOKENUTIL, "parseToken"), "calls", 0.8)
e(fid(P16, "getUser"), fid(REDISKEYBUILD, "createRedisKey"), "calls", 0.8)
e(fid(P18, "execute"), fid(USERSERVICE, "doExist"), "calls", 0.8)

# calls (part 3)
e(fid(P19, "execute"), fid(P21, "onRequest"), "calls", 0.8)
e(fid(P20, "execute"), fid(CAPTCHA, "verification"), "calls", 0.8)
e(fid(P20, "execute"), fid(REDISKEYBUILD, "createRedisKey"), "calls", 0.8)
e(fid(P22, "calculateDatabaseIndex"), fid(STRINGUTIL, "isNotEmpty"), "calls", 0.8)
e(fid(P24, "apply"), fid(STRINGUTIL, "isEmpty"), "calls", 0.8)
e(fid(P26, "doFilterInternal"), fid(STRINGUTIL, "isNotEmpty"), "calls", 0.8)

# inherits
e(cid(P13, "ProgramBloomFilterCheckHandler"), cid(P25, "AbstractComposite"), "inherits", 0.9)
e(cid(P14, "ProgramRecommendCheckHandler"), cid(P25, "AbstractComposite"), "inherits", 0.9)
e(cid(P17, "AbstractUserRegisterCheckHandler"), cid(P25, "AbstractComposite"), "inherits", 0.9)
e(cid(P18, "UserExistCheckHandler"), cid(P17, "AbstractUserRegisterCheckHandler"), "inherits", 0.9)
e(cid(P19, "UserRegisterCountCheckHandler"), cid(P17, "AbstractUserRegisterCheckHandler"), "inherits", 0.9)
e(cid(P20, "UserRegisterVerifyCaptcha"), cid(P17, "AbstractUserRegisterCheckHandler"), "inherits", 0.9)

# ============ import edges (1:1 with batchImportData) ============
import_edges = 0
for path in sorted(imports):
    for dep in imports[path]:
        if dep == path:
            print("SELF-IMPORT SKIPPED:", path)
            continue
        e("file:" + path, "file:" + dep, "imports", 0.7)
        import_edges += 1

# ============ partition ============
files = sorted(f["path"] for f in b["batchFiles"])
file_set = set(files)
total_nodes = len(nodes)
total_edges = len(edges)
parts = max(math.ceil(total_nodes / 60.0), math.ceil(total_edges / 120.0))
# ensure per-part budget (60 nodes / 120 edges) is respected
while True:
    chunk = math.ceil(len(files) / parts)
    groups = [set(files[i:i + chunk]) for i in range(0, len(files), chunk)]
    ok = True
    for g in groups:
        gn = [n for n in nodes if (n.get("filePath") in g)]
        gids = set(n["id"] for n in gn)
        ge = [x for x in edges if x["source"] in gids]
        if len(gn) > 60 or len(ge) > 120:
            ok = False
    if ok or parts > 12:
        break
    parts += 1

print("total nodes", total_nodes, "total edges", total_edges, "imports", import_edges, "parts", parts)

for k, g in enumerate(groups, 1):
    gn = [n for n in nodes if n.get("filePath") in g]
    gids = set(n["id"] for n in gn)
    ge = [x for x in edges if x["source"] in gids]
    out = os.path.join(OUT_DIR, "batch-6-part-%d.json" % k)
    json.dump({"nodes": gn, "edges": ge}, open(out, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print("part", k, "files", len(g), "nodes", len(gn), "edges", len(ge), "->", out)
