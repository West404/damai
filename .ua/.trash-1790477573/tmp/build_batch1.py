# -*- coding: utf-8 -*-
import json, io, math

ROOT = 'E:/WorkSpace/damai'
ID = 'damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/'
D  = 'damai-id-generator-framework/src/main/java/com/damai/'
DQ = 'damai-redisson-framework/damai-service-delay-queue-framework/src/main/java/com/damai/'
PC = 'damai-server-client/damai-program-client/src/main/java/com/damai/'
BD = 'damai-server/damai-base-data-service/src/main/java/com/damai/'
OD = 'damai-server/damai-order-service/src/main/java/com/damai/'
PG = 'damai-server/damai-program-service/src/main/java/com/damai/'

COMMON_API = 'damai-common/src/main/java/com/damai/common/ApiResponse.java'
COMMON_STR = 'damai-common/src/main/java/com/damai/util/StringUtil.java'
REDIS_WIA  = 'damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/config/RedisDisposableWorkerIdAssigner.java'
DQC        = DQ + 'context/DelayQueueContext.java'
BASEQ      = DQ + 'core/DelayBaseQueue.java'

# ---------------------------------------------------------------- file nodes
F = []
def file(p, summary, tags, cx, notes=None):
    F.append((p, summary, tags, cx, notes))

file(ID+'BitsAllocator.java',
     '雪花算法位分配器，按 timestampBits/workerIdBits/sequenceBits 计算各段最大值与位移量，并把时间戳、机器号、序列号按位拼装或拆解为最终 UID。',
     ['工具类','位运算','雪花算法','id-generator'], 'moderate')
file(ID+'UidGenerator.java',
     'UID 生成器统一接口，约定 getUid/getId/getOrderNumber/parseUid 四项能力，是百度开源 UID 与项目自研雪花实现的公共契约。',
     ['接口定义','id-generator','雪花算法','api'], 'moderate')
file(ID+'buffer/BufferPaddingExecutor.java',
     '环形缓冲区填充执行器，通过定时任务与异步触发按秒批量预生成 UID 写入 RingBuffer，以缓冲换吞吐，避免每次取号都走时间戳计算。',
     ['并发','环形缓冲区','线程池','id-generator'], 'moderate',
     '同时使用 ScheduledThreadPoolExecutor（定时填充）与 ThreadPoolExecutor（异步填充）两条触发路径。')
file(ID+'buffer/RejectedPutBufferHandler.java',
     '函数式接口，定义环形缓冲区写满时丢弃 UID 的拒绝策略，支持 Lambda 实现。',
     ['接口定义','回调','环形缓冲区','id-generator'], 'simple',
     '@FunctionalInterface：允许以 Lambda 形式注入环形缓冲区满时的处理策略。')
file(ID+'buffer/RejectedTakeBufferHandler.java',
     '函数式接口，定义环形缓冲区为空时取号失败的拒绝策略，支持 Lambda 实现。',
     ['接口定义','回调','环形缓冲区','id-generator'], 'simple',
     '@FunctionalInterface：允许以 Lambda 形式注入环形缓冲区空时的处理策略。')
file(ID+'buffer/RingBuffer.java',
     '基于数组与 CAS 的环形缓冲区，用双原子游标（tail/cursor）和标志位数组实现无锁生产/消费，为 UID 生成器提供内存级取号缓冲。',
     ['并发','环形缓冲区','无锁','id-generator'], 'complex',
     '以 PaddedAtomicLong + 标志位数组实现无锁环形队列，填充阈值触达时自动申请异步填充。')
file(ID+'config/IdGeneratorRedisConfig.java',
     '条件装配的 Redis 适配配置，提供 idGeneratorRedisTemplate 与基于 Redis 的 WorkerIdAssigner，使百度 UID 的 workerId 可在集群范围内分配。',
     ['配置','redis','自动装配','id-generator'], 'simple',
     '@ConditionalOnProperty("spring.data.redis.host")：仅在配置了 Redis 地址时生效。')
file(ID+'config/WorkerNodeConfig.java',
     '百度 UID 集成配置类，把 Redis WorkerIdAssigner 与自研 SnowflakeIdGenerator 组装进 CachedUidGenerator 并注册为 UID 生成器 Bean。',
     ['配置','自动装配','id-generator','spring'], 'simple')
file(ID+'exception/UidGenerateException.java',
     'UID 生成异常类，提供多个构造重载并支持消息格式化，用于统一暴露取号失败错误。',
     ['异常处理','id-generator','错误处理'], 'moderate')
file(ID+'impl/CachedUidGenerator.java',
     '带缓存的 UID 生成器实现，继承 DefaultUidGenerator，通过 RingBuffer + BufferPaddingExecutor 提前缓存 UID，把取号降级为内存操作以支撑高并发。',
     ['id-generator','雪花算法','缓存','高性能','并发'], 'moderate',
     '继承 DefaultUidGenerator 并实现 DisposableBean，兼顾位分配器复用与容器销毁时释放缓冲区资源。')
file(ID+'impl/DefaultUidGenerator.java',
     '默认 UID 生成器实现，基于位分配器和秒级时间戳，用同步 nextId 保证同一秒内序列自增，并处理序列耗尽与时钟回拨。',
     ['id-generator','雪花算法','并发','基础实现'], 'complex',
     '实现 InitializingBean，在 afterPropertiesSet 中完成位分配器构建、workerId 校验与分配。')
file(ID+'utils/AbstractDateUtils.java',
     '抽象日期工具类，扩展 Apache commons DateUtils，提供按天/按时间戳模式的字符串解析与格式化，供 UID 解析时间戳与设置 epoch 使用。',
     ['工具类','日期时间','格式化','abstract'], 'moderate')
file(ID+'utils/NamingThreadFactory.java',
     '可命名的线程工厂，按调用方类名加自增序号生成线程名，并统一设置守护态与未捕获异常处理器，便于线上排查并发问题。',
     ['并发','线程工厂','工具类','工厂'], 'moderate')
file(ID+'utils/PaddedAtomicLong.java',
     '在 AtomicLong 前后填充伪字段以消除 CPU 缓存行伪共享的高性能原子计数器，用作环形缓冲区的读写游标。',
     ['并发','工具类','伪共享','原子操作'], 'simple',
     '通过 6 个无业务含义的填充字段撑开缓存行，规避 False Sharing 带来的性能抖动。')
file(ID+'worker/WorkerIdAssigner.java',
     'workerId（机器号）分配器接口，把机器号的获取方式（Redis、数据库、IP 等）与 UID 生成逻辑解耦。',
     ['接口定义','id-generator','扩展点'], 'simple')
file(D+'config/IdGeneratorAutoConfig.java',
     '分布式 ID 自动装配配置，依次注册 WorkAndDataCenterIdHandler、WorkDataCenterId 与 SnowflakeIdGenerator 三个 Bean。',
     ['配置','自动装配','id-generator','spring'], 'simple')
file(D+'toolkit/SnowflakeIdGenerator.java',
     '项目自研雪花算法 ID 生成器，采用 5 位数据中心 + 5 位机器位 + 12 位序列的划分，支持从 Redis 获取机器位、生成订单号以及反解 ID 生成时间。',
     ['id-generator','雪花算法','分布式','高性能'], 'moderate',
     '以 BASIS_TIME 为起始偏移，时钟回拨时用 tilNextMillis 自旋等待下一毫秒。')
file(D+'toolkit/WorkAndDataCenterIdHandler.java',
     '通过 Lua 脚本在 Redis 中原子申请 workerId 与 dataCenterId，避免集群多实例启动时机器号冲突。',
     ['分布式','redis','lua','id-generator'], 'simple')
file(D+'toolkit/WorkDataCenterId.java',
     'workerId 与 dataCenterId 的简单值对象，作为雪花 ID 生成器的构造参数。',
     ['数据模型','dto','id-generator'], 'simple')
file(DQ+'config/DelayQueueAutoConfig.java',
     '延迟队列自动装配配置，注册 DelayQueueInitHandler、DelayQueueBasePart 与 DelayQueueContext，并开启 DelayQueueProperties 配置绑定。',
     ['配置','自动装配','延迟队列','spring'], 'simple')
file(DQ+'config/DelayQueueProperties.java',
     '延迟队列配置属性（前缀 delay.queue），控制消费线程池的核数/最大值/队列容量以及 Redis 隔离分区数。',
     ['配置','延迟队列','线程池','properties'], 'simple',
     '注解 @ConfigurationProperties(prefix = "delay.queue")：同一 topic 的生产者与消费者隔离分区数必须一致。')
file(DQ+'context/DelayQueueBasePart.java',
     '延迟队列基础上下文，持有 RedissonClient 与配置属性，供生产者与消费者共享。',
     ['上下文','延迟队列','redisson'], 'simple')
file(DQ+'context/DelayQueuePart.java',
     '单个主题的延迟队列上下文，把基础上下文与具体的 ConsumerTask 实现绑定在一起。',
     ['上下文','延迟队列','数据模型'], 'simple')
file(DQ+'core/ConsumerTask.java',
     '延迟队列消费者接口，业务方实现 execute 处理消息、实现 topic 声明订阅主题。',
     ['接口定义','延迟队列','扩展点'], 'simple')
file(DQ+'core/DelayConsumerQueue.java',
     '延迟队列消费者，继承 DelayBaseQueue，用单线程监听池阻塞拉取 Redisson 队列消息，再交给业务线程池并发执行 ConsumerTask。',
     ['延迟队列','并发','线程池','redisson'], 'moderate',
     '继承 DelayBaseQueue 复用 Redisson 阻塞队列，监听线程与业务执行线程分离，避免拉取阻塞影响消费。')
file(DQ+'event/DelayQueueInitHandler.java',
     '应用启动事件监听器，扫描容器内所有 ConsumerTask Bean，按隔离分区数为每个主题创建并启动 DelayConsumerQueue。',
     ['事件监听','延迟队列','启动初始化','spring'], 'simple')
file(PC+'dto/TestDto.java',
     '测试用请求 DTO，仅包含一个 id 字段。',
     ['数据模型','dto','test'], 'simple')
file(PC+'dto/TestSendDto.java',
     '测试消息 DTO，包含 count/message/time 字段，既作延迟队列消息体也作计数器重置请求体。',
     ['数据模型','dto','test'], 'simple')
file(BD+'controller/TestController.java',
     'base-data 服务的测试控制层，暴露 /test/testData1-3 三个接口，用于演示线程与线程池场景下 traceId 的传递效果。',
     ['api-handler','controller','test','入口点'], 'simple')
file(BD+'service/TestService.java',
     '测试服务，横向对比 ThreadLocal、InheritableThreadLocal 与 TransmittableThreadLocal 在普通线程和线程池中的 traceId 传递差异，并演示通过 beforeExecute/afterExecute 同步 MDC 日志链路。',
     ['test','并发','thread-local','日志链路'], 'moderate',
     '同时使用 JDK ThreadLocal、InheritableThreadLocal 与阿里 TransmittableThreadLocal + TtlRunnable，专门用于验证线程池上下文透传。')
file(OD+'service/test/Test.java',
     '订单服务的延迟队列测试消费者，实现 ConsumerTask，解析 TestSendDto 并打印消息从发送到消费的实际延时。',
     ['test','延迟队列','event-handler'], 'simple')
file(PG+'controller/TestController.java',
     'program 服务的测试控制层，提供 /test/reset 重置消息计数器与 /test/test 占位接口。',
     ['api-handler','controller','test','入口点'], 'simple')
file(PG+'service/TestService.java',
     '测试服务，用 PaddedAtomicLong 维护一个可被重置的原子计数器，配合延迟队列压测统计消费量。',
     ['test','原子操作','service'], 'simple')

# ---------------------------------------------------------------- class nodes
C = []
def cls(p, name, rng, summary, tags, cx, notes=None):
    C.append((p, name, rng, summary, tags, cx, notes))

cls(ID+'BitsAllocator.java','BitsAllocator',[28,136],
    '位分配器，负责按各段位宽计算最大时间戳增量、最大机器号与最大序列号，并把三者按位拼装/拆解为 UID。',
    ['工具类','位运算','雪花算法','id-generator'],'moderate')
cls(ID+'UidGenerator.java','UidGenerator',[25,58],
    'UID 生成器接口，声明生成唯一 ID、生成订单号与解析 UID 的契约，由百度 DefaultUidGenerator 与本项目雪花实现共同实现。',
    ['接口定义','id-generator','雪花算法','api'],'moderate')
cls(ID+'buffer/BufferPaddingExecutor.java','BufferPaddingExecutor',[39,181],
    '缓冲区填充执行器，维护 lastSecond 与运行标志，按秒批量向 RingBuffer 预填 UID，支持定时与异步两种触发方式以及优雅停机。',
    ['并发','环形缓冲区','线程池','id-generator'],'moderate')
cls(ID+'buffer/RejectedPutBufferHandler.java','RejectedPutBufferHandler',[24,34],
    '函数式接口，声明环形缓冲区写满时的处理策略 rejectPutBuffer。',
    ['接口定义','回调','环形缓冲区','id-generator'],'simple')
cls(ID+'buffer/RejectedTakeBufferHandler.java','RejectedTakeBufferHandler',[24,33],
    '函数式接口，声明环形缓冲区为空时的处理策略 rejectTakeBuffer。',
    ['接口定义','回调','环形缓冲区','id-generator'],'simple')
cls(ID+'buffer/RingBuffer.java','RingBuffer',[39,261],
    '无锁环形缓冲区，以 slots 数组存放 UID、flags 数组标记槽位可读性、tail/cursor 双游标记录写入与读取位置，实现内存级高性能取号。',
    ['并发','环形缓冲区','无锁','id-generator'],'complex')
cls(ID+'config/IdGeneratorRedisConfig.java','IdGeneratorRedisConfig',[19,35],
    'Redis 适配配置类，提供 StringRedisSerializer 的 idGeneratorRedisTemplate 与 Redis 版 WorkerIdAssigner。',
    ['配置','redis','自动装配','id-generator'],'simple')
cls(ID+'config/WorkerNodeConfig.java','WorkerNodeConfig',[16,33],
    '百度 UID 集成配置类，创建名为 cachedUidGenerator 的 UidGenerator Bean 并注入 workerId 分配器与雪花生成器。',
    ['配置','自动装配','id-generator','spring'],'simple')
cls(ID+'exception/UidGenerateException.java','UidGenerateException',[23,75],
    '继承 RuntimeException 的 UID 生成异常，提供空参、消息、原因、格式化消息等构造重载。',
    ['异常处理','id-generator','错误处理'],'moderate')
cls(ID+'impl/CachedUidGenerator.java','CachedUidGenerator',[50,183],
    '缓存型 UID 生成器，在 DefaultUidGenerator 基础上增加 RingBuffer 缓冲与填充执行器，桶容量按 maxSequence 放大 boostPower 倍以获得更大缓冲。',
    ['id-generator','雪花算法','缓存','高性能','并发'],'moderate')
cls(ID+'impl/DefaultUidGenerator.java','DefaultUidGenerator',[61,237],
    '默认 UID 生成器，使用 BitsAllocator 分配位段，nextId 同步生成 ID，并实现从 ID 反解时间戳/机器号/序列号的 parseUid。',
    ['id-generator','雪花算法','并发','基础实现'],'complex')
cls(ID+'utils/AbstractDateUtils.java','AbstractDateUtils',[29,121],
    '抽象日期工具类，继承 Apache commons 的 DateUtils，提供按天与按时间戳两种模式的解析与格式化方法。',
    ['工具类','日期时间','格式化','abstract'],'moderate')
cls(ID+'utils/NamingThreadFactory.java','NamingThreadFactory',[34,165],
    '线程工厂实现，按调用方类名生成线程名前缀并用并发 Map 维护自增序号，同时统一注入守护态与未捕获异常处理器。',
    ['并发','线程工厂','工具类','工厂'],'moderate')
cls(ID+'utils/PaddedAtomicLong.java','PaddedAtomicLong',[28,52],
    '继承 AtomicLong 并填充伪字段的高性能原子计数器，用于消除缓存行伪共享。',
    ['并发','工具类','伪共享','原子操作'],'simple')
cls(ID+'worker/WorkerIdAssigner.java','WorkerIdAssigner',[23,33],
    'workerId 分配器接口，声明 assignWorkerId 用于为 UID 生成器分配唯一机器号。',
    ['接口定义','id-generator','扩展点'],'simple')
cls(D+'config/IdGeneratorAutoConfig.java','IdGeneratorAutoConfig',[14,30],
    '分布式 ID 配置类，提供 workAndDataCenterIdHandler、workDataCenterId、snowflakeIdGenerator 三个 Bean。',
    ['配置','自动装配','id-generator','spring'],'simple')
cls(D+'toolkit/SnowflakeIdGenerator.java','SnowflakeIdGenerator',[19,199],
    '自研雪花 ID 生成器，5 位数据中心 + 5 位机器位 + 12 位序列，可从 Redis 读取机器位，支持生成订单号与反解 ID 时间戳。',
    ['id-generator','雪花算法','分布式','高性能'],'moderate')
cls(D+'toolkit/WorkAndDataCenterIdHandler.java','WorkAndDataCenterIdHandler',[19,56],
    '机器号获取器，加载 lua/workAndDataCenterId.lua 脚本并借 StringRedisTemplate 执行，原子取得 workerId 与 dataCenterId。',
    ['分布式','redis','lua','id-generator'],'simple')
cls(D+'toolkit/WorkDataCenterId.java','WorkDataCenterId',[10,16],
    '机器号值对象，承载 workId 与 dataCenterId 两个字段。',
    ['数据模型','dto','id-generator'],'simple')
cls(DQ+'config/DelayQueueAutoConfig.java','DelayQueueAutoConfig',[16,33],
    '延迟队列配置类，装配初始化监听器、基础上下文与消息发送上下文，并启用 DelayQueueProperties 属性绑定。',
    ['配置','自动装配','延迟队列','spring'],'simple')
cls(DQ+'config/DelayQueueProperties.java','DelayQueueProperties',[15,47],
    '延迟队列配置属性类，包含消费线程池核心/最大线程数、回收时间、工作队列容量与隔离分区数等字段。',
    ['配置','延迟队列','线程池','properties'],'simple')
cls(DQ+'context/DelayQueueBasePart.java','DelayQueueBasePart',[13,20],
    '延迟队列基础上下文，聚合 RedissonClient 与 DelayQueueProperties。',
    ['上下文','延迟队列','redisson'],'simple')
cls(DQ+'context/DelayQueuePart.java','DelayQueuePart',[11,22],
    '主题级延迟队列上下文，聚合基础上下文与 ConsumerTask。',
    ['上下文','延迟队列','数据模型'],'simple')
cls(DQ+'core/ConsumerTask.java','ConsumerTask',[8,20],
    '延迟队列消费者接口，声明 execute 消费方法与 topic 主题方法。',
    ['接口定义','延迟队列','扩展点'],'simple')
cls(DQ+'core/DelayConsumerQueue.java','DelayConsumerQueue',[19,84],
    '延迟队列消费者实现，继承 DelayBaseQueue 复用 Redisson 阻塞队列，用监听线程池拉取消息、业务线程池并发执行。',
    ['延迟队列','并发','线程池','redisson'],'moderate')
cls(DQ+'event/DelayQueueInitHandler.java','DelayQueueInitHandler',[19,43],
    '应用启动监听器，在 ApplicationStartedEvent 之后为每个 ConsumerTask 按隔离分区数启动对应的延迟消费队列。',
    ['事件监听','延迟队列','启动初始化','spring'],'simple')
cls(PC+'dto/TestDto.java','TestDto',[13,19],
    '测试请求 DTO，包含 id 字段。',
    ['数据模型','dto','test'],'simple')
cls(PC+'dto/TestSendDto.java','TestSendDto',[13,23],
    '测试消息 DTO，包含 count、message、time 三个字段。',
    ['数据模型','dto','test'],'simple')
cls(BD+'controller/TestController.java','TestController',[18,47],
    'base-data 测试控制层，提供 /test/testData、/test/testData2、/test/testData3 三个 POST 接口。',
    ['api-handler','controller','test','入口点'],'simple')
cls(BD+'service/TestService.java','TestService',[20,97],
    '测试服务，定义多种 ThreadLocal 与线程池组合，用于验证 traceId 在主线程与线程池线程之间的传递与 MDC 日志链路。',
    ['test','并发','thread-local','日志链路'],'moderate')
cls(OD+'service/test/Test.java','Test',[14,29],
    '订单服务延迟队列消费者，实现 ConsumerTask 解析 TestSendDto 并输出消息实际延时。',
    ['test','延迟队列','event-handler'],'simple')
cls(PG+'controller/TestController.java','TestController',[20,37],
    'program 测试控制层，提供重置消息计数器与占位测试两个接口。',
    ['api-handler','controller','test','入口点'],'simple')
cls(PG+'service/TestService.java','TestService',[20,25],
    'program 测试服务，使用 PaddedAtomicLong 维护可重置的原子计数器。',
    ['test','原子操作','service'],'simple')

# ---------------------------------------------------------------- function nodes
FN = []
def fn(p, name, rng, summary, tags, cx):
    FN.append((p, name, rng, summary, tags, cx))

fn(ID+'BitsAllocator.java','allocate',[88,90],
   '把时间戳增量、workerId 与序列号按各自位宽左移后按位或，拼装出最终 UID。',
   ['位运算','雪花算法','id-generator'],'simple')
fn(ID+'buffer/BufferPaddingExecutor.java','start',[109,113],
   '启动定时填充任务，按 scheduleInterval 秒周期性调用 paddingBuffer。',
   ['并发','定时任务','id-generator'],'simple')
fn(ID+'buffer/BufferPaddingExecutor.java','shutdown',[118,126],
   '关闭定时填充线程与填充线程池，用于应用停机时释放资源。',
   ['并发','优雅停机','id-generator'],'simple')
fn(ID+'buffer/BufferPaddingExecutor.java','asyncPadding',[140,142],
   '向填充线程池提交一次异步填充任务，供取号线程在缓冲不足时主动触发。',
   ['并发','异步','id-generator'],'simple')
fn(ID+'buffer/BufferPaddingExecutor.java','paddingBuffer',[147,171],
   '单次填充逻辑：以当前秒为界批量预生成该秒 UID 并 put 入 RingBuffer，缓冲区满时用 CAS 收敛 running 标志结束本轮。',
   ['并发','环形缓冲区','id-generator'],'moderate')
fn(ID+'buffer/BufferPaddingExecutor.java','setScheduleInterval',[176,179],
   '校验并设置定时填充的秒级间隔。',
   ['配置','校验','id-generator'],'simple')
fn(ID+'buffer/RingBuffer.java','put',[111,139],
   '向环形缓冲区写入 UID：校验槽位可写、写入并与 tail 比较、置可读标志；缓冲区满时交由 RejectedPutBufferHandler 处理。',
   ['并发','环形缓冲区','无锁','id-generator'],'moderate')
fn(ID+'buffer/RingBuffer.java','take',[151,184],
   '从环形缓冲区读取 UID：CAS 前移 cursor、取 UID 并置不可读标志；缓冲区空时触发异步填充并调用 RejectedTakeBufferHandler。',
   ['并发','环形缓冲区','无锁','id-generator'],'moderate')
fn(ID+'buffer/RingBuffer.java','toString',[250,259],
   '输出缓冲区大小、tail、cursor 与填充阈值等运行状态，便于监控与排查。',
   ['调试','监控','id-generator'],'simple')
fn(ID+'impl/CachedUidGenerator.java','afterPropertiesSet',[66,74],
   'Bean 初始化回调：先完成父类位分配器初始化，再构建环形缓冲区与填充执行器。',
   ['初始化','spring','id-generator','缓存'],'simple')
fn(ID+'impl/CachedUidGenerator.java','destroy',[101,104],
   'Bean 销毁回调，关闭缓冲填充执行器，释放定时任务与线程池资源。',
   ['优雅停机','spring','id-generator'],'simple')
fn(ID+'impl/CachedUidGenerator.java','getUid',[76,84],
   '直接从环形缓冲区取 UID，取号失败时包装为 UidGenerateException 抛出。',
   ['id-generator','缓存','api'],'simple')
fn(ID+'impl/CachedUidGenerator.java','nextIdsForOneSecond',[112,124],
   '为指定秒批量生成该秒内全部可用 UID，作为环形缓冲区的数据来源。',
   ['id-generator','批量生成','雪花算法'],'moderate')
fn(ID+'impl/CachedUidGenerator.java','initRingBuffer',[129,158],
   '初始化环形缓冲区与填充执行器：按 maxSequence 与 boostPower 计算桶容量，设置拒绝策略和填充触发，并启动定时填充。',
   ['id-generator','初始化','环形缓冲区'],'moderate')
fn(ID+'impl/DefaultUidGenerator.java','getUid',[103,111],
   '生成并返回一个 UID，异常统一包装为 UidGenerateException。',
   ['id-generator','api','雪花算法'],'simple')
fn(ID+'impl/DefaultUidGenerator.java','getId',[113,116],
   '委托自研 SnowflakeIdGenerator 生成全局唯一 ID。',
   ['id-generator','雪花算法','api'],'simple')
fn(ID+'impl/DefaultUidGenerator.java','getOrderNumber',[118,121],
   '委托自研雪花生成器，按用户 ID 与分表数量生成订单编号。',
   ['id-generator','订单号','api'],'simple')
fn(ID+'impl/DefaultUidGenerator.java','afterPropertiesSet',[89,101],
   'Bean 初始化回调：构建 BitsAllocator、校验 workerId 上限并调用 WorkerIdAssigner 分配机器号。',
   ['初始化','spring','id-generator'],'moderate')
fn(ID+'impl/DefaultUidGenerator.java','parseUid',[123,142],
   '按位宽反解 UID，还原生成时间戳、workerId 与序列号，并格式化为可读字符串。',
   ['id-generator','位运算','调试'],'moderate')
fn(ID+'impl/DefaultUidGenerator.java','nextId',[150,176],
   '核心取号逻辑：同一秒内序列自增并检测溢出，跨秒或序列耗尽时切换/等待下一秒，最后用 BitsAllocator 拼装 UID。',
   ['id-generator','雪花算法','并发','位运算'],'complex')
fn(ID+'impl/DefaultUidGenerator.java','setWorkerIdAssigner',[205,207],
   '注入 workerId 分配器实现。',
   ['setter','配置','id-generator'],'simple')
fn(ID+'impl/DefaultUidGenerator.java','setSnowflakeIdGenerator',[234,236],
   '注入自研雪花 ID 生成器，用于 ID 与订单号生成。',
   ['setter','配置','id-generator'],'simple')
fn(ID+'utils/AbstractDateUtils.java','formatByDateTimePattern',[106,108],
   '按 yyyy-MM-dd HH:mm:ss 模式格式化日期，供 UID 解析结果展示。',
   ['日期时间','格式化','工具类'],'simple')
fn(ID+'utils/NamingThreadFactory.java','newThread',[76,102],
   '创建线程：根据调用方类名与自增序号设置线程名，并应用守护态与未捕获异常处理器配置。',
   ['并发','线程工厂','工具类'],'moderate')
fn(ID+'utils/NamingThreadFactory.java','getSequence',[125,136],
   '按调用方名称维护线程自增序号，使用 ConcurrentHashMap 保证并发安全。',
   ['并发','工具类','原子操作'],'moderate')
fn(D+'toolkit/SnowflakeIdGenerator.java','nextId',[149,156],
   '生成下一个雪花 ID：同一毫秒内序列自增，跨毫秒重置序列，出现时钟回拨时等待下一毫秒。',
   ['雪花算法','并发','id-generator'],'simple')
fn(D+'toolkit/SnowflakeIdGenerator.java','getOrderNumber',[162,170],
   '结合雪花 ID 与分表数量计算订单号，按用户维度散列以支撑分库分表。',
   ['订单号','分表','id-generator'],'simple')
fn(D+'toolkit/SnowflakeIdGenerator.java','getDatacenterId',[89,109],
   '通过本机网卡 MAC 地址计算 dataCenterId，取不到网卡时回退为随机数，避免多实例数据中心号冲突。',
   ['分布式','id-generator','网络'],'moderate')
fn(D+'toolkit/SnowflakeIdGenerator.java','getBase',[111,147],
   '雪花 ID 核心生成逻辑：取当前毫秒时间戳、处理时钟回拨与毫秒内序列溢出，再按位移拼装出 64 位 ID。',
   ['雪花算法','位运算','并发','id-generator'],'moderate')
fn(D+'toolkit/WorkAndDataCenterIdHandler.java','getWorkAndDataCenterId',[43,55],
   '执行 Lua 脚本从 Redis 原子分配机器号与数据中心号，并反序列化为 WorkDataCenterId。',
   ['分布式','redis','lua','id-generator'],'moderate')
fn(DQ+'core/DelayConsumerQueue.java','listenStart',[50,73],
   '启动单线程监听循环：阻塞 take Redisson 阻塞队列消息，并提交业务线程池执行 ConsumerTask，异常时记录日志或触发销毁。',
   ['延迟队列','并发','线程池','redisson'],'moderate')
fn(DQ+'core/DelayConsumerQueue.java','destroy',[75,83],
   '优雅关闭传入的线程池，异常仅记录日志不向上抛出。',
   ['优雅停机','并发','延迟队列'],'simple')
fn(DQ+'core/ConsumerTask.java','execute',[14,14],
   '消费一条延迟消息，由业务方实现具体处理逻辑。',
   ['接口方法','延迟队列','扩展点'],'simple')
fn(DQ+'core/ConsumerTask.java','topic',[19,19],
   '返回消费者订阅的主题名，决定消息的隔离分区前缀。',
   ['接口方法','延迟队列','topic'],'simple')
fn(DQ+'event/DelayQueueInitHandler.java','onApplicationEvent',[24,42],
   '应用启动完成后扫描容器内全部 ConsumerTask Bean，按隔离分区数为每个主题创建并启动 DelayConsumerQueue。',
   ['事件监听','启动初始化','延迟队列','spring'],'moderate')
fn(BD+'controller/TestController.java','testData',[27,32],
   'POST /test/testData，调用服务层演示 ThreadLocal 场景下的 traceId 传递。',
   ['api-handler','controller','入口点','test'],'simple')
fn(BD+'controller/TestController.java','testData2',[34,39],
   'POST /test/testData2，调用服务层演示 InheritableThreadLocal 场景下的 traceId 传递。',
   ['api-handler','controller','入口点','test'],'simple')
fn(BD+'controller/TestController.java','testData3',[41,46],
   'POST /test/testData3，调用服务层演示线程池场景下的 traceId 传递。',
   ['api-handler','controller','入口点','test'],'simple')
fn(BD+'service/TestService.java','testData',[47,55],
   '使用普通 ThreadLocal 保存 traceId，验证 new Thread 创建的子线程无法继承主线程上下文。',
   ['test','thread-local','并发'],'simple')
fn(BD+'service/TestService.java','testData2',[57,65],
   '使用 InheritableThreadLocal 保存 traceId，验证直接创建子线程时可以继承上下文。',
   ['test','thread-local','并发'],'simple')
fn(BD+'service/TestService.java','testData3',[67,75],
   '使用 InheritableThreadLocal 配合固定线程池，验证线程池复用线程时上下文继承失效。',
   ['test','thread-local','线程池','并发'],'simple')
fn(BD+'service/TestService.java','testData4',[77,85],
   '使用 TransmittableThreadLocal 配合 TtlRunnable 包装任务，验证线程池场景下上下文可正确透传。',
   ['test','thread-local','线程池','阿里ttl'],'simple')
fn(BD+'service/TestService.java','testData5',[87,96],
   '重写 TransmittableThreadLocal 的 beforeExecute/afterExecute 在线程池线程中设置与清理 MDC，验证日志链路 traceId 输出。',
   ['test','thread-local','日志链路','并发'],'simple')
fn(OD+'service/test/Test.java','execute',[19,23],
   '解析 TestSendDto 消息内容，并打印消息从发送到消费的实际延时毫秒数。',
   ['test','延迟队列','event-handler'],'simple')
fn(OD+'service/test/Test.java','topic',[25,28],
   '返回消费者订阅的主题名 test-topic。',
   ['test','延迟队列','topic'],'simple')
fn(PG+'controller/TestController.java','reset',[27,31],
   'POST /test/reset，调用服务层把消息计数器重置为零。',
   ['api-handler','controller','入口点','test'],'simple')
fn(PG+'controller/TestController.java','test',[33,36],
   'POST /test/test 占位接口，仅返回成功响应。',
   ['api-handler','controller','test'],'simple')
fn(PG+'service/TestService.java','reset',[21,24],
   '把 PaddedAtomicLong 计数器重置为零并返回成功。',
   ['test','原子操作','service'],'simple')

# ---------------------------------------------------------------- nodes
nodes = []
for p, s, t, cx, notes in F:
    n = {'id':'file:'+p, 'type':'file', 'name':p.split('/')[-1], 'filePath':p,
         'summary':s, 'tags':t, 'complexity':cx}
    if notes: n['languageNotes'] = notes
    nodes.append(n)
for p, name, rng, s, t, cx, notes in C:
    n = {'id':'class:'+p+':'+name, 'type':'class', 'name':name, 'filePath':p,
         'lineRange':rng, 'summary':s, 'tags':t, 'complexity':cx}
    if notes: n['languageNotes'] = notes
    nodes.append(n)
for p, name, rng, s, t, cx in FN:
    nodes.append({'id':'function:'+p+':'+name, 'type':'function', 'name':name,
                  'filePath':p, 'lineRange':rng, 'summary':s, 'tags':t, 'complexity':cx})

# ---------------------------------------------------------------- edges
E = []
def edge(src, dst, typ, owner, w):
    if src == dst: return
    E.append({'source':src, 'target':dst, 'type':typ, 'direction':'forward',
              'weight':w, '_owner':owner})

def FID(p): return 'file:'+p
def CID(p, n): return 'class:'+p+':'+n
def FUID(p, n): return 'function:'+p+':'+n

importData = json.load(io.open(ROOT+'/.ua/tmp/ua-file-analyzer-input-1.json', encoding='utf-8'))['batchImportData']

# imports — 1:1 with batchImportData
for p, targets in importData.items():
    for t in targets:
        edge(FID(p), FID(t), 'imports', p, 0.7)

# contains (file -> class) and exports (file -> class)
for p, name, rng, s, t, cx, notes in C:
    edge(FID(p), CID(p, name), 'contains', p, 1.0)
    edge(FID(p), CID(p, name), 'exports', p, 0.8)
# contains (file -> function)
for p, name, rng, s, t, cx in FN:
    edge(FID(p), FUID(p, name), 'contains', p, 1.0)

# inherits / implements
edge(CID(ID+'impl/CachedUidGenerator.java','CachedUidGenerator'),
     CID(ID+'impl/DefaultUidGenerator.java','DefaultUidGenerator'), 'inherits',
     ID+'impl/CachedUidGenerator.java', 0.9)
edge(CID(DQ+'core/DelayConsumerQueue.java','DelayConsumerQueue'),
     'class:'+BASEQ+':DelayBaseQueue', 'inherits',
     DQ+'core/DelayConsumerQueue.java', 0.9)
edge(CID(ID+'impl/DefaultUidGenerator.java','DefaultUidGenerator'),
     CID(ID+'UidGenerator.java','UidGenerator'), 'implements',
     ID+'impl/DefaultUidGenerator.java', 0.9)
edge(CID(OD+'service/test/Test.java','Test'),
     CID(DQ+'core/ConsumerTask.java','ConsumerTask'), 'implements',
     OD+'service/test/Test.java', 0.9)

# calls
def C_(src_path, src_name, dst_path, dst_name, kind='function'):
    if kind == 'class':
        edge(FUID(src_path, src_name), CID(dst_path, dst_name), 'calls', src_path, 0.8)
    elif kind == 'clscall':
        edge(CID(src_path, src_name), CID(dst_path, dst_name), 'calls', src_path, 0.8)
    else:
        edge(FUID(src_path, src_name), FUID(dst_path, dst_name), 'calls', src_path, 0.8)

B = ID+'impl/CachedUidGenerator.java'
DU = ID+'impl/DefaultUidGenerator.java'

C_(B,'nextIdsForOneSecond', ID+'BitsAllocator.java','allocate')
C_(B,'initRingBuffer',       ID+'buffer/BufferPaddingExecutor.java','paddingBuffer')
C_(B,'initRingBuffer',       ID+'buffer/BufferPaddingExecutor.java','setScheduleInterval')
C_(B,'initRingBuffer',       ID+'buffer/BufferPaddingExecutor.java','start')
C_(B,'afterPropertiesSet',   B,'initRingBuffer')
C_(B,'getUid',               ID+'buffer/RingBuffer.java','take')
C_(B,'destroy',              ID+'buffer/BufferPaddingExecutor.java','shutdown')
C_(B,'initRingBuffer',       ID+'buffer/RingBuffer.java','RingBuffer', kind='class')
C_(DU,'nextId',              ID+'BitsAllocator.java','allocate')
C_(DU,'afterPropertiesSet',  ID+'worker/WorkerIdAssigner.java','assignWorkerId')
C_(DU,'afterPropertiesSet',  ID+'BitsAllocator.java','BitsAllocator', kind='class')
C_(DU,'parseUid',            ID+'utils/AbstractDateUtils.java','formatByDateTimePattern')
C_(DU,'getId',               D+'toolkit/SnowflakeIdGenerator.java','nextId')
C_(DU,'getOrderNumber',      D+'toolkit/SnowflakeIdGenerator.java','getOrderNumber')
C_(ID+'buffer/BufferPaddingExecutor.java','paddingBuffer',
   ID+'buffer/RingBuffer.java','put')
edge(CID(ID+'buffer/BufferPaddingExecutor.java','BufferPaddingExecutor'),
     CID(ID+'buffer/RingBuffer.java','RingBuffer'), 'calls',
     ID+'buffer/BufferPaddingExecutor.java', 0.8)
edge(CID(ID+'buffer/BufferPaddingExecutor.java','BufferPaddingExecutor'),
     CID(ID+'utils/NamingThreadFactory.java','NamingThreadFactory'), 'calls',
     ID+'buffer/BufferPaddingExecutor.java', 0.8)
edge(CID(ID+'buffer/BufferPaddingExecutor.java','BufferPaddingExecutor'),
     CID(ID+'utils/PaddedAtomicLong.java','PaddedAtomicLong'), 'calls',
     ID+'buffer/BufferPaddingExecutor.java', 0.8)
edge(CID(ID+'buffer/RingBuffer.java','RingBuffer'),
     CID(ID+'utils/PaddedAtomicLong.java','PaddedAtomicLong'), 'calls',
     ID+'buffer/RingBuffer.java', 0.8)
C_(ID+'buffer/RingBuffer.java','put',
   ID+'buffer/RejectedPutBufferHandler.java','rejectPutBuffer')
C_(ID+'buffer/RingBuffer.java','take',
   ID+'buffer/RejectedTakeBufferHandler.java','rejectTakeBuffer')
C_(ID+'buffer/RingBuffer.java','take',
   ID+'buffer/BufferPaddingExecutor.java','asyncPadding')
C_(ID+'utils/NamingThreadFactory.java','newThread',
   ID+'utils/NamingThreadFactory.java','getSequence')
edge(CID(ID+'config/WorkerNodeConfig.java','WorkerNodeConfig'),
     CID(B,'CachedUidGenerator'), 'calls', ID+'config/WorkerNodeConfig.java', 0.8)
edge(CID(ID+'config/IdGeneratorRedisConfig.java','IdGeneratorRedisConfig'),
     'class:'+REDIS_WIA+':RedisDisposableWorkerIdAssigner', 'calls',
     ID+'config/IdGeneratorRedisConfig.java', 0.8)
edge(CID(D+'config/IdGeneratorAutoConfig.java','IdGeneratorAutoConfig'),
     FUID(D+'toolkit/WorkAndDataCenterIdHandler.java','getWorkAndDataCenterId'),
     'calls', D+'config/IdGeneratorAutoConfig.java', 0.8)
edge(CID(D+'config/IdGeneratorAutoConfig.java','IdGeneratorAutoConfig'),
     CID(D+'toolkit/SnowflakeIdGenerator.java','SnowflakeIdGenerator'), 'calls',
     D+'config/IdGeneratorAutoConfig.java', 0.8)
C_(D+'toolkit/WorkAndDataCenterIdHandler.java','getWorkAndDataCenterId',
   D+'toolkit/WorkDataCenterId.java','WorkDataCenterId', kind='class')
C_(DQ+'event/DelayQueueInitHandler.java','onApplicationEvent',
   DQ+'core/DelayConsumerQueue.java','listenStart')
C_(DQ+'event/DelayQueueInitHandler.java','onApplicationEvent',
   DQ+'core/ConsumerTask.java','topic')
edge(FUID(DQ+'event/DelayQueueInitHandler.java','onApplicationEvent'),
     CID(DQ+'core/DelayConsumerQueue.java','DelayConsumerQueue'), 'calls',
     DQ+'event/DelayQueueInitHandler.java', 0.8)
edge(FUID(DQ+'event/DelayQueueInitHandler.java','onApplicationEvent'),
     CID(DQ+'context/DelayQueuePart.java','DelayQueuePart'), 'calls',
     DQ+'event/DelayQueueInitHandler.java', 0.8)
C_(DQ+'core/DelayConsumerQueue.java','listenStart',
   DQ+'core/ConsumerTask.java','execute')
C_(DQ+'core/DelayConsumerQueue.java','listenStart',
   DQ+'core/DelayConsumerQueue.java','destroy')
edge(CID(DQ+'config/DelayQueueAutoConfig.java','DelayQueueAutoConfig'),
     CID(DQ+'event/DelayQueueInitHandler.java','DelayQueueInitHandler'), 'calls',
     DQ+'config/DelayQueueAutoConfig.java', 0.8)
edge(CID(DQ+'config/DelayQueueAutoConfig.java','DelayQueueAutoConfig'),
     CID(DQ+'context/DelayQueueBasePart.java','DelayQueueBasePart'), 'calls',
     DQ+'config/DelayQueueAutoConfig.java', 0.8)
edge(CID(DQ+'config/DelayQueueAutoConfig.java','DelayQueueAutoConfig'),
     'class:'+DQC+':DelayQueueContext', 'calls',
     DQ+'config/DelayQueueAutoConfig.java', 0.8)
# cross-batch: delay queue consumers implement ConsumerTask (informational, in-batch target only emitted above)

C_(BD+'controller/TestController.java','testData',
   BD+'service/TestService.java','testData')
C_(BD+'controller/TestController.java','testData2',
   BD+'service/TestService.java','testData2')
C_(BD+'controller/TestController.java','testData3',
   BD+'service/TestService.java','testData3')
C_(BD+'controller/TestController.java','testData',
   COMMON_API,'ok')
C_(PG+'controller/TestController.java','reset',
   PG+'service/TestService.java','reset')
C_(PG+'controller/TestController.java','reset',
   COMMON_API,'ok')
C_(BD+'service/TestService.java','testData5', COMMON_STR,'isNotEmpty')

# ---------------------------------------------------------------- partition
paths = sorted([p for p, *_ in F])
nodeCount = len(nodes)
edgeCount = len(E)

def owner_of(nid):
    body = nid.split(':', 1)[1]
    if ':' in body:
        # function:/class: -> strip trailing symbol
        cand = body.rsplit(':', 1)[0]
        if cand.endswith(('.java', '.ts', '.js', '.py', '.go', '.md', '.yml', '.yaml', '.json', '.xml', '.sql', '.sh', '.properties')):
            return cand
        return body
    return body

parts = max(1, math.ceil(max(nodeCount/60.0, edgeCount/120.0)))
while parts < 12:
    chunk = math.ceil(len(paths)/float(parts))
    groups = [paths[i*chunk:(i+1)*chunk] for i in range(parts)]
    groups = [g for g in groups if g]
    ok = True
    for g in groups:
        gs = set(g)
        if len([n for n in nodes if owner_of(n['id']) in gs]) > 60: ok = False
        if len([e for e in E if owner_of(e['source']) in gs]) > 120: ok = False
    if ok: break
    parts += 1

def owner_of(nid):
    body = nid.split(':', 1)[1]
    if ':' in body:
        # function:/class: -> strip trailing symbol
        cand = body.rsplit(':', 1)[0]
        if cand.endswith(('.java', '.ts', '.js', '.py', '.go', '.md', '.yml', '.yaml', '.json', '.xml', '.sql', '.sh', '.properties')):
            return cand
        return body
    return body

filePart = {}
for i, g in enumerate(groups):
    for p in g:
        filePart[p] = i

import glob, os
for stale in glob.glob(ROOT+'/.ua/intermediate/batch-1*.json'):
    os.remove(stale)

written = []
for i, g in enumerate(groups, 1):
    gset = set(g)
    pn = [n for n in nodes if owner_of(n['id']) in gset]
    pe = []
    for e in E:
        if owner_of(e['source']) in gset:
            ee = {k: v for k, v in e.items() if k != '_owner'}
            pe.append(ee)
    out = {'nodes': pn, 'edges': pe}
    if len(groups) == 1:
        fp = ROOT+'/.ua/intermediate/batch-1.json'
    else:
        fp = ROOT+'/.ua/intermediate/batch-1-part-%d.json' % i
    with io.open(fp, 'w', encoding='utf-8') as f:
        json.dump(out, f, ensure_ascii=False, indent=2)
    written.append((fp, len(pn), len(pe)))

print('files', len(paths), 'nodes', nodeCount, 'edges', edgeCount, 'parts', len(groups))
print('imports_expected', sum(len(v) for v in importData.values()),
      'imports_emitted', sum(1 for e in E if e['type'] == 'imports'))
for fp, n, e in written:
    print('WROTE', fp, 'nodes', n, 'edges', e)
