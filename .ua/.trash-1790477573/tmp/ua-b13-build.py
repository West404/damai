import json, math, os

UA = 'E:/WorkSpace/damai/.ua'
inp = json.load(open(UA + '/intermediate/batch-input-13.json', encoding='utf-8'))
imp = inp['batchImportData']
files = [f['path'] for f in inp['batchFiles']]

O = 'damai-server/damai-order-service/src/main/java/com/damai'
C = 'damai-server/damai-customize-service/src/main/java/com/damai'
G = 'damai-server/damai-gateway-service/src/main/java/com/damai'
P = 'damai-server/damai-program-service/src/main/java/com/damai'
S = 'damai-spring-cloud-framework'
V = 'damai-server-client/damai-order-client/src/main/java/com/damai/vo/UserInfoVo.java'

fUserInfoVo = V
fBroadcastController = C + '/controller/BroadcastController.java'
fBroadcastService = C + '/service/BroadcastService.java'
fConfig = G + '/conf/Config.java'
fReqTmp = G + '/conf/RequestTemporaryWrapper.java'
fGatewayEH = G + '/exception/GatewayDefaultExceptionHandler.java'
fOrderController = O + '/controller/OrderController.java'
fOrder = O + '/entity/Order.java'
fOrderTicketUser = O + '/entity/OrderTicketUser.java'
fOrderAgg = O + '/entity/OrderTicketUserAggregate.java'
fOrderMapper = O + '/mapper/OrderMapper.java'
fOrderTicketUserMapper = O + '/mapper/OrderTicketUserMapper.java'
fOrderService = O + '/service/OrderService.java'
fOrderTicketUserService = O + '/service/OrderTicketUserService.java'
fDelayConsumer = O + '/service/delayconsumer/DelayOrderCancelConsumer.java'
fCreateOrderConsumer = O + '/service/kafka/CreateOrderConsumer.java'
fOrderProperties = O + '/service/properties/OrderProperties.java'
fProgramCheck = P + '/service/composite/impl/ProgramUserExistCheckHandler.java'
fTokenExpire = P + '/service/tool/TokenExpireManager.java'
fDefaultEH = S + '/damai-service-common/src/main/java/com/damai/exception/DefaultExceptionHandler.java'
fReqFilter = S + '/damai-service-component/src/main/java/com/damai/filter/RequestWrapperFilter.java'
fCustomizeWrapper = S + '/damai-service-component/src/main/java/com/damai/request/CustomizeRequestWrapper.java'

BaseTableData = S + '/damai-service-common/src/main/java/com/damai/data/BaseTableData.java'
AbstractCheck = P + '/service/composite/AbstractProgramCheckHandler.java'
ConsumerTask = 'damai-redisson-framework/damai-service-delay-queue-framework/src/main/java/com/damai/core/ConsumerTask.java'
SendDelay = O + '/service/delaysend/DelayOperateProgramDataSend.java'
ProgramService = P + '/service/ProgramService.java'

nodes = []
edges = []


def fnode(path, summary, tags, complexity, notes=None):
    n = {"id": "file:" + path, "type": "file", "name": os.path.basename(path), "filePath": path,
         "summary": summary, "tags": tags, "complexity": complexity}
    if notes:
        n["languageNotes"] = notes
    nodes.append(n)


def cnode(path, name, summary, tags, complexity, lr):
    nodes.append({"id": "class:%s:%s" % (path, name), "type": "class", "name": name, "filePath": path,
                  "lineRange": lr, "summary": summary, "tags": tags, "complexity": complexity})


def fnodep(path, name, summary, tags, complexity, lr):
    nodes.append({"id": "function:%s:%s" % (path, name), "type": "function", "name": name, "filePath": path,
                  "lineRange": lr, "summary": summary, "tags": tags, "complexity": complexity})


def contains(path, kind, name):
    edges.append({"source": "file:" + path, "target": "%s:%s:%s" % (kind, path, name), "type": "contains",
                  "direction": "forward", "weight": 1.0})


def exports(path, kind, name):
    edges.append({"source": "file:" + path, "target": "%s:%s:%s" % (kind, path, name), "type": "exports",
                  "direction": "forward", "weight": 0.8})


# ---------- file nodes ----------
fnode(fUserInfoVo, "订单侧用户信息 VO，承载用户 id、姓名、证件姓名、性别、手机号与地址等字段，用于订单详情中关联用户资料的传输与展示。",
      ["data-model", "vo", "订单", "用户信息"], "simple")
fnode(fBroadcastController, "广播调用控制层，对外暴露 POST /broadcast/add 接口，将 BroadcastCallDto 交给 BroadcastService 处理后返回统一响应 ApiResponse。",
      ["api-handler", "controller", "广播调用", "入口点"], "simple")
fnode(fBroadcastService, "广播调用业务服务，接收 BroadcastCallDto 完成广播消息的组装与下发。",
      ["service", "业务逻辑", "广播调用"], "simple")
fnode(fConfig, "网关服务通用配置类，注册 RestTemplate、HttpMessageConverters、全局异常处理器、CORS 跨域规则与监听线程池等 Bean。",
      ["configuration", "gateway", "bean注册", "跨域"], "moderate",
      "实现 WebFluxConfigurer 覆写 addCorsMappings；@Bean 配合 @Order(-2) 让自定义 ErrorWebExceptionHandler 优先于 Spring 默认实现。")
fnode(fReqTmp, "网关异常处理过程中传递临时数据的包装对象，暂存请求参数 Map 与待写出的 ApiResponse。",
      ["data-model", "gateway", "异常处理"], "simple")
fnode(fGatewayEH, "网关全局异常处理器，实现 ErrorWebExceptionHandler，把 NOT_FOUND、业务异常、参数异常与未知异常统一转换为 JSON 响应体。",
      ["api-handler", "异常处理", "gateway", "webflux"], "moderate")
fnode(fOrderController, "订单服务 REST 控制层，暴露订单创建、支付、支付状态检查、支付宝回调、列表、详情、缓存查询、账户订单数统计与取消等接口。",
      ["api-handler", "controller", "订单", "入口点"], "moderate")
fnode(fOrder, "订单实体，映射订单主表，包含订单号、节目信息、价格、支付方式、订单状态及下单/取消/支付时间等字段，继承 BaseTableData 复用公共字段。",
      ["data-model", "entity", "订单", "mybatis-plus"], "moderate")
fnode(fOrderTicketUser, "购票人订单实体，一行对应一个座位/票档的购票记录，包含座位信息、票档、支付价格与订单状态等字段。",
      ["data-model", "entity", "购票人", "mybatis-plus"], "moderate")
fnode(fOrderAgg, "购票人订单聚合结果对象，用于按订单号统计每个订单下的购票人数量。",
      ["data-model", "聚合对象", "订单统计"], "simple")
fnode(fOrderMapper, "订单表 MyBatis-Plus Mapper 接口，除基础 CRUD 外提供账户下购票人数量统计与订单数据真实删除。",
      ["mapper", "data-access", "订单", "mybatis-plus"], "simple")
fnode(fOrderTicketUserMapper, "购票人订单 Mapper 接口，提供按订单号列表聚合购票人数量与购票人订单数据真实删除。",
      ["mapper", "data-access", "购票人", "mybatis-plus"], "simple")
fnode(fOrderService, "订单核心业务服务，覆盖创建订单、发起支付、支付回调与核对、订单状态变更、节目座位/票档缓存反向操作、列表详情聚合与订单取消，并通过 RPC 与支付、用户、节目服务协作。",
      ["service", "订单", "业务核心", "分布式锁", "缓存"], "complex",
      "基于 MyBatis-Plus ServiceImpl 与 Wrappers 构造条件，事务方法统一 @Transactional(rollbackFor = Exception.class)，并用 @ServiceLock/@RepeatExecuteLimit 注解实现 Redisson 分布式锁与防重复执行。")
fnode(fOrderTicketUserService, "购票人订单服务，继承 MyBatis-Plus ServiceImpl，为订单创建提供购票人记录批量保存等基础能力。",
      ["service", "购票人", "mybatis-plus"], "simple")
fnode(fDelayConsumer, "延迟队列消费者，订阅订单取消延迟 topic，解析 DelayOrderCancelDto 后驱动 OrderService 取消订单。",
      ["consumer", "延迟队列", "订单取消", "redisson"], "simple")
fnode(fCreateOrderConsumer, "Kafka 消费者，监听创建订单主题，依据消息延迟时间决定落库创建订单还是回滚节目座位数据。",
      ["consumer", "kafka", "订单创建", "异步处理"], "moderate")
fnode(fOrderProperties, "订单支付回调相关配置，通过 @Value 注入支付成功通知接口地址与支付成功跳转页面地址。",
      ["configuration", "properties", "支付回调"], "simple")
fnode(fProgramCheck, "节目下单流程中的用户与购票人校验处理器，作为责任链一环校验购票人有效性与账号限购数量。",
      ["handler", "责任链", "校验", "下单流程"], "moderate")
fnode(fTokenExpire, "token 失效时间配置组件，通过 @Value 注入 token.expire.time（默认 40），供缓存过期时间计算使用。",
      ["configuration", "token", "缓存过期"], "simple")
fnode(fDefaultEH, "服务通用全局异常处理器（@RestControllerAdvice），统一处理业务异常、参数校验异常与未捕获异常并返回 ApiResponse。",
      ["api-handler", "异常处理", "spring-boot", "全局advice"], "moderate")
fnode(fReqFilter, "Servlet 过滤器，用 CustomizeRequestWrapper 包装请求后继续执行过滤链，使后续逻辑可重复读取请求体。",
      ["filter", "middleware", "请求包装"], "simple")
fnode(fCustomizeWrapper, "HttpServletRequestWrapper 实现，构造时缓存请求体并重写 getInputStream/getReader，解决 Servlet 请求体只能读取一次的问题。",
      ["middleware", "请求包装", "servlet", "工具类"], "moderate",
      "继承 HttpServletRequestWrapper，用匿名内部类实现 ServletInputStream 以提供可重复读取的输入流。")

# ---------- class nodes ----------
cnode(fUserInfoVo, "UserInfoVo", "订单侧用户信息视图对象，字段与用户基本资料一一对应，用于订单详情返回。",
      ["data-model", "vo", "订单", "用户信息"], "simple", [14, 33])
cnode(fBroadcastController, "BroadcastController", "广播调用控制器，声明 /broadcast/add 接口并注入 BroadcastService。",
      ["api-handler", "controller", "广播调用"], "simple", [23, 34])
cnode(fBroadcastService, "BroadcastService", "广播调用业务服务类，提供 call 方法处理广播请求。",
      ["service", "业务逻辑", "广播调用"], "simple", [12, 16])
cnode(fConfig, "Config", "网关通用配置类，声明 RestTemplate、消息转换器、异常处理器、跨域规则与监听线程池等 Bean。",
      ["configuration", "gateway", "bean注册"], "moderate", [26, 69])
cnode(fReqTmp, "RequestTemporaryWrapper", "异常处理临时数据包装类，持有请求参数 Map 与 ApiResponse 两个字段。",
      ["data-model", "gateway", "异常处理"], "simple", [14, 19])
cnode(fGatewayEH, "GatewayDefaultExceptionHandler", "网关自定义异常处理器，实现 ErrorWebExceptionHandler 完成异常到 JSON 响应的转换。",
      ["api-handler", "异常处理", "gateway"], "moderate", [24, 81])
cnode(fOrderController, "OrderController", "订单控制器，声明订单全生命周期相关 HTTP 接口并统一委托 OrderService。",
      ["api-handler", "controller", "订单"], "moderate", [36, 94])
cnode(fOrder, "Order", "订单表实体类，继承 BaseTableData 并实现 Serializable，字段覆盖节目信息、订单金额、状态与关键时间点。",
      ["data-model", "entity", "订单", "mybatis-plus"], "moderate", [19, 108])
cnode(fOrderTicketUser, "OrderTicketUser", "购票人订单实体类，继承 BaseTableData，按座位/票档记录订单明细与支付信息。",
      ["data-model", "entity", "购票人", "mybatis-plus"], "moderate", [19, 98])
cnode(fOrderAgg, "OrderTicketUserAggregate", "购票人订单聚合对象，包含订单号与购票人数量两个字段。",
      ["data-model", "聚合对象", "订单统计"], "simple", [11, 16])
cnode(fOrderMapper, "OrderMapper", "订单 Mapper 接口，继承 MyBatis-Plus BaseMapper<Order> 并声明账户购票人数量统计与订单真实删除方法。",
      ["mapper", "data-access", "订单", "mybatis-plus"], "simple", [13, 28])
cnode(fOrderTicketUserMapper, "OrderTicketUserMapper", "购票人订单 Mapper 接口，继承 BaseMapper<OrderTicketUser> 并声明购票人聚合查询与真实删除方法。",
      ["mapper", "data-access", "购票人", "mybatis-plus"], "simple", [15, 30])
cnode(fOrderService, "OrderService", "订单领域核心服务类，继承 ServiceImpl<OrderMapper, Order>，编排订单创建、支付、回调、状态更新、缓存反向操作与取消等流程。",
      ["service", "订单", "业务核心", "分布式锁"], "complex", [99, 631])
cnode(fOrderTicketUserService, "OrderTicketUserService", "购票人订单服务类，继承 ServiceImpl<OrderTicketUserMapper, OrderTicketUser> 提供批量保存等基础操作。",
      ["service", "购票人", "mybatis-plus"], "simple", [14, 16])
cnode(fDelayConsumer, "DelayOrderCancelConsumer", "延迟订单取消消费者，实现 ConsumerTask 接口，按前缀区分名拼接订阅 topic。",
      ["consumer", "延迟队列", "订单取消"], "simple", [23, 57])
cnode(fCreateOrderConsumer, "CreateOrderConsumer", "Kafka 创建订单消费者，按消息延迟阈值决定创建订单或回滚座位数据。",
      ["consumer", "kafka", "订单创建"], "moderate", [31, 77])
cnode(fOrderProperties, "OrderProperties", "订单支付回调配置类，以 @Value 注入通知地址与跳转页面地址。",
      ["configuration", "properties", "支付回调"], "simple", [14, 27])
cnode(fProgramCheck, "ProgramUserExistCheckHandler", "下单校验责任链中的用户与购票人校验处理器，继承 AbstractProgramCheckHandler 并声明执行顺序。",
      ["handler", "责任链", "校验", "下单流程"], "moderate", [41, 135])
cnode(fTokenExpire, "TokenExpireManager", "token 失效时间管理组件，持有 tokenExpireTime 配置属性。",
      ["configuration", "token", "缓存过期"], "simple", [14, 18])
cnode(fDefaultEH, "DefaultExceptionHandler", "全局异常处理器类，声明三类 @ExceptionHandler 方法与请求信息提取辅助方法。",
      ["api-handler", "异常处理", "全局advice"], "moderate", [23, 69])
cnode(fReqFilter, "RequestWrapperFilter", "请求包装过滤器，继承 OncePerRequestFilter 并覆写 doFilterInternal。",
      ["filter", "middleware", "请求包装"], "simple", [17, 25])
cnode(fCustomizeWrapper, "CustomizeRequestWrapper", "请求包装器类，继承 HttpServletRequestWrapper 并缓存请求体以支持重复读取。",
      ["middleware", "请求包装", "servlet"], "moderate", [20, 65])

# ---------- function nodes ----------
fnodep(fBroadcastController, "call", "广播调用接口方法：校验入参后调用 BroadcastService.call 并返回 ApiResponse.ok()。",
       ["api-handler", "controller", "广播调用"], "simple", [28, 33])
fnodep(fConfig, "threadPoolExecutor", "创建监听线程池 Bean：核心线程数为 CPU 核数、最大为核心核数 +10，使用无界队列与 listen-start-thread- 前缀线程工厂。",
       ["configuration", "线程池", "bean定义"], "simple", [58, 68])
fnodep(fGatewayEH, "handle", "网关统一异常处理：判断响应是否已提交，按异常类型构造 ApiResponse 并写入响应体，业务类异常返回 200、其余返回 500。",
       ["api-handler", "异常处理", "webflux", "reactive"], "complex", [26, 80])

fnodep(fOrderController, "create", "订单创建接口 POST /order/create，仅供内部 program 服务调用，返回 OrderService.create 生成的订单号。",
       ["api-handler", "订单创建", "内部接口"], "simple", [41, 45])
fnodep(fOrderController, "pay", "订单支付接口 POST /order/pay，返回支付参数。",
       ["api-handler", "支付", "订单"], "simple", [47, 51])
fnodep(fOrderController, "payCheck", "支付后订单状态检查接口 POST /order/pay/check，返回订单支付核对结果。",
       ["api-handler", "支付核对", "订单"], "simple", [53, 57])
fnodep(fOrderController, "alipayNotify", "支付宝支付异步回调接口 POST /order/alipay/notify，直接返回业务处理结果字符串。",
       ["api-handler", "支付回调", "支付宝"], "simple", [59, 63])
fnodep(fOrderController, "selectList", "订单列表查询接口 POST /order/select/list。",
       ["api-handler", "订单查询", "列表"], "simple", [65, 69])
fnodep(fOrderController, "get", "订单详情查询接口 POST /order/get，返回订单与购票人/用户信息聚合结果。",
       ["api-handler", "订单查询", "详情"], "simple", [71, 75])
fnodep(fOrderController, "accountOrderCount", "账户下某节目订单数量接口 POST /order/account/order/count，仅供内部 program 服务调用。",
       ["api-handler", "订单统计", "内部接口"], "simple", [77, 81])
fnodep(fOrderController, "getCache", "读取缓存订单接口 POST /order/get/cache，返回 Redis 中暂存的订单号。",
       ["api-handler", "缓存查询", "订单"], "simple", [83, 87])
fnodep(fOrderController, "cancel", "订单取消接口 POST /order/cancel，委托 OrderService.initiateCancel 完成校验与取消。",
       ["api-handler", "订单取消", "订单"], "simple", [89, 93])

fnodep(fOrderService, "create", "创建订单：校验订单号唯一后复制 DTO 生成订单与购票人记录并批量落库，最后递增缓存中的订单计数。",
       ["service", "订单创建", "事务", "mybatis-plus"], "moderate", [138, 165])
fnodep(fOrderService, "cancel", "取消订单入口：在防重复执行与分布式锁保护下将订单及购票人记录状态置为已取消。",
       ["service", "订单取消", "分布式锁", "事务"], "simple", [170, 176])
fnodep(fOrderService, "pay", "发起支付：校验订单存在性与状态、比对支付金额，组装 PayDto 后调用支付服务 commonPay 返回支付参数。",
       ["service", "支付", "rpc调用", "参数校验"], "moderate", [178, 204])
fnodep(fOrderService, "getPayDto", "根据支付入参与订单号组装 PayDto，填充账单类型、渠道、平台、金额及配置的回调与跳转地址。",
       ["service", "支付", "数据组装"], "simple", [206, 217])
fnodep(fOrderService, "payCheck", "支付后核对：对已取消订单发起退款，否则向支付服务发起交易查询，并按账单状态更新订单状态与节目座位缓存。",
       ["service", "支付核对", "分布式锁", "缓存反向操作"], "complex", [222, 283])
fnodep(fOrderService, "alipayNotify", "处理支付宝异步回调：解析回调参数、以订单号加分布式锁，按订单当前状态退款或确认支付通知，成功后更新订单与节目数据。",
       ["service", "支付回调", "分布式锁", "幂等"], "complex", [286, 346])
fnodep(fOrderService, "updateOrderRelatedData", "订单状态变更主流程：更新订单与购票人记录的状态和时间，取消时回补缓存计数，支付时按座位聚合并反向操作节目座位与票档缓存。",
       ["service", "订单状态", "缓存反向操作", "事务"], "complex", [351, 404])
fnodep(fOrderService, "checkOrderStatus", "校验订单状态，若已取消、已支付或已退款则抛出业务异常，避免重复或非法状态变更。",
       ["service", "状态校验", "异常处理"], "simple", [406, 419])
fnodep(fOrderService, "updateProgramRelatedDataResolution", "节目数据反向操作：基于座位缓存分组回滚/确认座位锁与售出状态、票档余票，并组装延迟消息数据同步节目服务。",
       ["service", "缓存反向操作", "座位库存", "延迟消息"], "complex", [421, 491])
fnodep(fOrderService, "selectList", "查询用户订单列表：按用户 id 查询订单、转换为 VO 并聚合每个订单的购票人数量。",
       ["service", "订单查询", "数据聚合"], "moderate", [493, 514])
fnodep(fOrderService, "get", "查询订单详情：组装订单 VO 与按票档分组的购票人信息，并通过用户服务 RPC 补充用户与购票人资料。",
       ["service", "订单查询", "rpc调用", "数据聚合"], "complex", [516, 584])
fnodep(fOrderService, "initiateCancel", "取消订单前置校验：确认订单存在且处于未支付状态后调用 cancel 执行取消。",
       ["service", "订单取消", "参数校验"], "moderate", [611, 624])

fnodep(fDelayConsumer, "execute", "消费延迟订单取消消息：判空后反序列化 DelayOrderCancelDto，转换为 OrderCancelDto 调用 OrderService.cancel 并记录结果日志。",
       ["consumer", "延迟队列", "订单取消"], "moderate", [33, 51])
fnodep(fCreateOrderConsumer, "consumerOrderMessage", "消费 Kafka 创建订单消息：计算消息延迟，超时则回滚节目座位数据，未超时则调用 OrderService.createMq 落库创建订单。",
       ["consumer", "kafka", "订单创建", "幂等"], "moderate", [43, 76])
fnodep(fProgramCheck, "execute", "校验下单用户与购票人：读取购票人缓存或调用用户服务补全、校验购票人有效性、查询节目与账号限购数量并写入缓存。",
       ["handler", "校验", "rpc调用", "缓存"], "complex", [58, 119])
fnodep(fDefaultEH, "toolkitExceptionHandler", "处理 DaMaiFrameException 业务异常，记录请求信息并返回业务错误码与错误信息。",
       ["api-handler", "异常处理", "业务异常"], "simple", [28, 32])
fnodep(fDefaultEH, "validExceptionHandler", "处理 MethodArgumentNotValidException：把字段校验错误映射为 ArgumentError 列表并以参数错误码返回。",
       ["api-handler", "异常处理", "参数校验"], "moderate", [36, 51])
fnodep(fDefaultEH, "defaultErrorHandler", "兜底处理未捕获异常，记录日志后返回默认错误响应。",
       ["api-handler", "异常处理", "兜底"], "simple", [56, 60])
fnodep(fReqFilter, "doFilterInternal", "用 CustomizeRequestWrapper 包装原始请求后继续执行过滤链，使后续逻辑可重复读取请求体。",
       ["filter", "middleware", "请求包装"], "simple", [19, 24])
fnodep(fCustomizeWrapper, "getInputStream", "重写 getInputStream：基于缓存的请求体字节构造可反复读取的 ServletInputStream 匿名实现。",
       ["middleware", "请求包装", "servlet"], "moderate", [30, 55])

# ---------- contains ----------
for path, kind, name in [
    (fUserInfoVo, 'class', 'UserInfoVo'),
    (fBroadcastController, 'class', 'BroadcastController'), (fBroadcastController, 'function', 'call'),
    (fBroadcastService, 'class', 'BroadcastService'),
    (fConfig, 'class', 'Config'), (fConfig, 'function', 'threadPoolExecutor'),
    (fReqTmp, 'class', 'RequestTemporaryWrapper'),
    (fGatewayEH, 'class', 'GatewayDefaultExceptionHandler'), (fGatewayEH, 'function', 'handle'),
    (fOrderController, 'class', 'OrderController'),
    (fOrder, 'class', 'Order'), (fOrderTicketUser, 'class', 'OrderTicketUser'), (fOrderAgg, 'class', 'OrderTicketUserAggregate'),
    (fOrderMapper, 'class', 'OrderMapper'), (fOrderTicketUserMapper, 'class', 'OrderTicketUserMapper'),
    (fOrderService, 'class', 'OrderService'), (fOrderTicketUserService, 'class', 'OrderTicketUserService'),
    (fDelayConsumer, 'class', 'DelayOrderCancelConsumer'), (fDelayConsumer, 'function', 'execute'),
    (fCreateOrderConsumer, 'class', 'CreateOrderConsumer'), (fCreateOrderConsumer, 'function', 'consumerOrderMessage'),
    (fOrderProperties, 'class', 'OrderProperties'),
    (fProgramCheck, 'class', 'ProgramUserExistCheckHandler'), (fProgramCheck, 'function', 'execute'),
    (fTokenExpire, 'class', 'TokenExpireManager'),
    (fDefaultEH, 'class', 'DefaultExceptionHandler'),
    (fReqFilter, 'class', 'RequestWrapperFilter'), (fReqFilter, 'function', 'doFilterInternal'),
    (fCustomizeWrapper, 'class', 'CustomizeRequestWrapper'), (fCustomizeWrapper, 'function', 'getInputStream'),
]:
    contains(path, kind, name)

for name in ["create", "pay", "payCheck", "alipayNotify", "selectList", "get", "accountOrderCount", "getCache", "cancel"]:
    contains(fOrderController, 'function', name)
for name in ["create", "cancel", "pay", "getPayDto", "payCheck", "alipayNotify", "updateOrderRelatedData",
             "checkOrderStatus", "updateProgramRelatedDataResolution", "selectList", "get", "initiateCancel"]:
    contains(fOrderService, 'function', name)
for name in ["toolkitExceptionHandler", "validExceptionHandler", "defaultErrorHandler"]:
    contains(fDefaultEH, 'function', name)

# ---------- exports (public classes + public methods) ----------
for path, kind, name in [
    (fUserInfoVo, 'class', 'UserInfoVo'),
    (fBroadcastController, 'class', 'BroadcastController'), (fBroadcastController, 'function', 'call'),
    (fBroadcastService, 'class', 'BroadcastService'),
    (fConfig, 'class', 'Config'), (fConfig, 'function', 'threadPoolExecutor'),
    (fReqTmp, 'class', 'RequestTemporaryWrapper'),
    (fGatewayEH, 'class', 'GatewayDefaultExceptionHandler'), (fGatewayEH, 'function', 'handle'),
    (fOrderController, 'class', 'OrderController'),
    (fOrder, 'class', 'Order'), (fOrderTicketUser, 'class', 'OrderTicketUser'), (fOrderAgg, 'class', 'OrderTicketUserAggregate'),
    (fOrderMapper, 'class', 'OrderMapper'), (fOrderTicketUserMapper, 'class', 'OrderTicketUserMapper'),
    (fOrderService, 'class', 'OrderService'), (fOrderTicketUserService, 'class', 'OrderTicketUserService'),
    (fDelayConsumer, 'class', 'DelayOrderCancelConsumer'), (fDelayConsumer, 'function', 'execute'),
    (fCreateOrderConsumer, 'class', 'CreateOrderConsumer'), (fCreateOrderConsumer, 'function', 'consumerOrderMessage'),
    (fOrderProperties, 'class', 'OrderProperties'),
    (fProgramCheck, 'class', 'ProgramUserExistCheckHandler'),
    (fTokenExpire, 'class', 'TokenExpireManager'),
    (fDefaultEH, 'class', 'DefaultExceptionHandler'),
    (fReqFilter, 'class', 'RequestWrapperFilter'),
    (fCustomizeWrapper, 'class', 'CustomizeRequestWrapper'), (fCustomizeWrapper, 'function', 'getInputStream'),
]:
    exports(path, kind, name)

for name in ["create", "pay", "payCheck", "alipayNotify", "selectList", "get", "accountOrderCount", "getCache", "cancel"]:
    exports(fOrderController, 'function', name)
for name in ["create", "cancel", "pay", "payCheck", "alipayNotify", "updateOrderRelatedData", "checkOrderStatus",
             "updateProgramRelatedDataResolution", "selectList", "get", "initiateCancel"]:
    exports(fOrderService, 'function', name)
for name in ["toolkitExceptionHandler", "validExceptionHandler", "defaultErrorHandler"]:
    exports(fDefaultEH, 'function', name)

# ---------- imports (1:1 with batchImportData) ----------
import_count = 0
for f in files:
    for t in imp[f]:
        if t == f:
            continue
        edges.append({"source": "file:" + f, "target": "file:" + t, "type": "imports",
                      "direction": "forward", "weight": 0.7})
        import_count += 1

# ---------- inherits / implements ----------
edges.append({"source": "class:%s:Order" % fOrder, "target": "class:%s:BaseTableData" % BaseTableData,
              "type": "inherits", "direction": "forward", "weight": 0.9})
edges.append({"source": "class:%s:OrderTicketUser" % fOrderTicketUser,
              "target": "class:%s:BaseTableData" % BaseTableData,
              "type": "inherits", "direction": "forward", "weight": 0.9})
edges.append({"source": "class:%s:ProgramUserExistCheckHandler" % fProgramCheck,
              "target": "class:%s:AbstractProgramCheckHandler" % AbstractCheck,
              "type": "inherits", "direction": "forward", "weight": 0.9})
edges.append({"source": "class:%s:DelayOrderCancelConsumer" % fDelayConsumer,
              "target": "class:%s:ConsumerTask" % ConsumerTask,
              "type": "implements", "direction": "forward", "weight": 0.9})

# ---------- calls ----------
for a, b in [("create", "create"), ("pay", "pay"), ("payCheck", "payCheck"), ("alipayNotify", "alipayNotify"),
             ("selectList", "selectList"), ("get", "get"), ("cancel", "initiateCancel")]:
    edges.append({"source": "function:%s:%s" % (fOrderController, a),
                  "target": "function:%s:%s" % (fOrderService, b),
                  "type": "calls", "direction": "forward", "weight": 0.8})
edges.append({"source": "function:%s:execute" % fDelayConsumer, "target": "function:%s:cancel" % fOrderService,
              "type": "calls", "direction": "forward", "weight": 0.8})
edges.append({"source": "function:%s:updateProgramRelatedDataResolution" % fOrderService,
              "target": "function:%s:sendMessage" % SendDelay, "type": "calls", "direction": "forward", "weight": 0.8})
edges.append({"source": "function:%s:execute" % fProgramCheck,
              "target": "function:%s:detailV2" % ProgramService, "type": "calls", "direction": "forward", "weight": 0.8})
edges.append({"source": "function:%s:handle" % fGatewayEH,
              "target": "class:%s:RequestTemporaryWrapper" % fReqTmp,
              "type": "calls", "direction": "forward", "weight": 0.8})
edges.append({"source": "function:%s:doFilterInternal" % fReqFilter,
              "target": "class:%s:CustomizeRequestWrapper" % fCustomizeWrapper,
              "type": "calls", "direction": "forward", "weight": 0.8})

# ---------- dedupe + self-edge guard ----------
seen = set()
ded = []
for e in edges:
    if e['source'] == e['target']:
        print('SELF EDGE DROPPED', e)
        continue
    k = (e['source'], e['target'], e['type'])
    if k in seen:
        print('DUP DROPPED', k)
        continue
    seen.add(k)
    ded.append(e)
edges = ded
ids = set(n['id'] for n in nodes)
assert len(ids) == len(nodes), 'duplicate node ids'
print('imports', import_count, 'expected', sum(len(imp[f]) for f in files))
print('nodes', len(nodes), 'edges', len(edges))

# ---------- partition ----------
N, E = len(nodes), len(edges)
if N <= 60 and E <= 120:
    parts = 1
else:
    parts = math.ceil(max(N / 60.0, E / 120.0))
parts = max(parts, 1)
srt = sorted(files)


def build(parts):
    chunk = math.ceil(len(srt) / float(parts))
    groups = [set(srt[i:i + chunk]) for i in range(0, len(srt), chunk)]
    res = []
    for g in groups:
        pn = [n for n in nodes if n.get('filePath') in g]
        pids = set(n['id'] for n in pn)
        pe = [e for e in edges if e['source'] in pids]
        res.append({'nodes': pn, 'edges': pe, 'files': sorted(g)})
    return res


out = build(parts)
while any(len(o['nodes']) > 60 or len(o['edges']) > 120 for o in out) and parts < len(srt):
    parts += 1
    out = build(parts)
print('parts', parts, 'sizes', [(len(o['files']), len(o['nodes']), len(o['edges'])) for o in out])

if len(out) == 1:
    fn = UA + '/intermediate/batch-13.json'
    json.dump({'nodes': out[0]['nodes'], 'edges': out[0]['edges']}, open(fn, 'w', encoding='utf-8'),
              ensure_ascii=False, indent=2)
    print('wrote', fn, len(out[0]['nodes']), len(out[0]['edges']))
else:
    for k, o in enumerate(out, 1):
        fn = UA + '/intermediate/batch-13-part-%d.json' % k
        json.dump({'nodes': o['nodes'], 'edges': o['edges']}, open(fn, 'w', encoding='utf-8'),
                  ensure_ascii=False, indent=2)
        print('wrote', fn, 'files', len(o['files']), 'nodes', len(o['nodes']), 'edges', len(o['edges']))
