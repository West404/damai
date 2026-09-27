import json, math, os

UA = "E:/WorkSpace/damai/.ua"
inp = json.load(open(f"{UA}/intermediate/batch-input-1.json"))
ext = json.load(open(f"{UA}/tmp/ua-file-extract-results-0.json"))
imports = inp["batchImportData"]
results = {r["path"]: r for r in ext["results"]}

# per-file metadata: (summary, tags, complexity, class_summary, class_tags)
META = {
"damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/BitsAllocator.java": (
 "Bit allocation strategy for 64-bit UIDs, dividing bits among sign, timestamp delta, worker id, and sequence fields.",
 ["id-generator","bit-allocation","snowflake","uid"], "moderate",
 "Allocates and validates the bit budget of a 64-bit UID across timestamp, worker id, and sequence segments.",
 ["id-generator","bit-allocation","uid"]),
"damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/UidGenerator.java": (
 "Interface defining the UID generation contract used by all generator implementations.",
 ["id-generator","interface","contract","uid"], "simple",
 "Contract for UID generators exposing getUid, getId, getOrderNumber, and parseUid operations.",
 ["id-generator","interface","uid"]),
"damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/buffer/BufferPaddingExecutor.java": (
 "Executor that asynchronously pads the RingBuffer with freshly generated UIDs using a scheduled thread pool.",
 ["id-generator","buffer","thread-pool","async"], "moderate",
 "Manages scheduled and on-demand padding of the UID ring buffer via an executor service.",
 ["id-generator","buffer","async","thread-pool"]),
"damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/buffer/RejectedPutBufferHandler.java": (
 "Functional interface for handling RingBuffer put operations rejected when the buffer is full.",
 ["id-generator","buffer","interface","rejection-handler"], "simple",
 "Callback interface invoked when a UID cannot be put into a full ring buffer.",
 ["id-generator","interface","rejection-handler"]),
"damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/buffer/RejectedTakeBufferHandler.java": (
 "Functional interface for handling RingBuffer take operations rejected when the buffer is empty.",
 ["id-generator","buffer","interface","rejection-handler"], "simple",
 "Callback interface invoked when a UID cannot be taken from an empty ring buffer.",
 ["id-generator","interface","rejection-handler"]),
"damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/buffer/RingBuffer.java": (
 "Circular buffer caching pre-generated UIDs with tail/cursor pointers and slot padding flags for high-throughput id consumption.",
 ["id-generator","ring-buffer","cache","concurrency"], "complex",
 "Ring buffer storing pre-generated UIDs and padding flags, supporting atomic put/take with rejection handlers.",
 ["id-generator","ring-buffer","concurrency","cache"]),
"damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/config/IdGeneratorRedisConfig.java": (
 "Spring configuration providing the RedisTemplate and RedisDisposableWorkerIdAssigner beans for worker id assignment.",
 ["configuration","spring","redis","id-generator"], "simple",
 "Configuration class wiring Redis-backed worker id assignment beans.",
 ["configuration","spring","redis"]),
"damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/config/WorkerNodeConfig.java": (
 "Spring configuration wiring the CachedUidGenerator bean with buffer rejection handlers and schedule interval.",
 ["configuration","spring","id-generator"], "simple",
 "Configuration class declaring the cached UID generator bean.",
 ["configuration","spring","id-generator"]),
"damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/exception/UidGenerateException.java": (
 "Runtime exception raised when UID generation fails, with standard exception constructors.",
 ["exception","id-generator","error-handling"], "simple",
 "Unchecked exception for UID generation failures.",
 ["exception","error-handling"]),
"damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/impl/CachedUidGenerator.java": (
 "RingBuffer-backed cached UID generator implementation delivering high-throughput id generation via asynchronous buffer padding.",
 ["id-generator","ring-buffer","cache","implementation"], "complex",
 "UidGenerator implementation that serves ids from a ring buffer refilled asynchronously by a padding executor.",
 ["id-generator","cache","ring-buffer"]),
"damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/impl/DefaultUidGenerator.java": (
 "Default Snowflake-style UID generator computing ids directly from timestamp delta, worker id, and sequence bits.",
 ["id-generator","snowflake","implementation"], "complex",
 "UidGenerator implementation producing Snowflake-like ids on demand with clock-drift protection.",
 ["id-generator","snowflake"]),
"damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/utils/AbstractDateUtils.java": (
 "Date parsing and formatting utilities backed by cached Apache Commons FastDateFormat patterns.",
 ["utility","date","formatting"], "moderate",
 "Abstract utility base providing thread-safe date parse/format helpers for day and datetime patterns.",
 ["utility","date","formatting"]),
"damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/utils/NamingThreadFactory.java": (
 "ThreadFactory producing named, optionally daemon threads with a shared uncaught exception handler.",
 ["utility","thread-pool","factory"], "moderate",
 "Thread factory that names threads with a sequence suffix and applies daemon and exception-handler settings.",
 ["factory","thread-pool","utility"]),
"damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/utils/PaddedAtomicLong.java": (
 "Cache-line-padded AtomicLong preventing false sharing between ring buffer cursor and tail counters.",
 ["utility","concurrency","atomic","performance"], "simple",
 "AtomicLong subclass padded with long fields to occupy a full cache line and avoid false sharing.",
 ["concurrency","atomic","performance"]),
"damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/worker/WorkerIdAssigner.java": (
 "Interface for assigning a unique worker node id to UID generator instances.",
 ["id-generator","interface","worker-id"], "simple",
 "Contract for worker id assignment strategies used by UID generators.",
 ["interface","worker-id","id-generator"]),
"damai-id-generator-framework/src/main/java/com/damai/config/IdGeneratorAutoConfig.java": (
 "Spring Boot auto-configuration registering SnowflakeIdGenerator and worker/datacenter id holder beans.",
 ["configuration","spring-boot","auto-configuration","id-generator"], "simple",
 "Auto-configuration wiring Snowflake id generation infrastructure beans.",
 ["auto-configuration","spring-boot","id-generator"]),
"damai-id-generator-framework/src/main/java/com/damai/toolkit/SnowflakeIdGenerator.java": (
 "Snowflake ID generator resolving worker and datacenter ids from network interface and configuration, with timestamp extraction helpers.",
 ["id-generator","snowflake","distributed-id"], "complex",
 "Generates distributed Snowflake ids and derives datacenter/worker ids from MAC address and system properties.",
 ["id-generator","snowflake","distributed-id"]),
"damai-id-generator-framework/src/main/java/com/damai/toolkit/WorkAndDataCenterIdHandler.java": (
 "Handler resolving worker id and datacenter id pairs from the local MAC address hash and system properties.",
 ["id-generator","worker-id","resolution"], "moderate",
 "Computes the WorkDataCenterId pair used to seed Snowflake id generation.",
 ["id-generator","worker-id"]),
"damai-id-generator-framework/src/main/java/com/damai/toolkit/WorkDataCenterId.java": (
 "Simple value holder for a worker id and datacenter id pair.",
 ["data-model","id-generator","value-object"], "simple",
 "Immutable-style holder carrying workerId and datacenterId.",
 ["data-model","value-object"]),
"damai-redisson-framework/damai-service-delay-queue-framework/src/main/java/com/damai/config/DelayQueueAutoConfig.java": (
 "Spring Boot auto-configuration registering the delay queue context, base part, and init handler beans.",
 ["configuration","spring-boot","auto-configuration","delay-queue"], "simple",
 "Auto-configuration wiring Redisson delay queue infrastructure beans.",
 ["auto-configuration","delay-queue","spring-boot"]),
"damai-redisson-framework/damai-service-delay-queue-framework/src/main/java/com/damai/config/DelayQueueProperties.java": (
 "Configuration properties for the Redisson delay queue framework such as isolation and topic key prefixes.",
 ["configuration","properties","delay-queue"], "simple",
 "Properties holder for delay queue settings bound from the environment.",
 ["configuration","properties","delay-queue"]),
"damai-redisson-framework/damai-service-delay-queue-framework/src/main/java/com/damai/context/DelayQueueBasePart.java": (
 "Base context object carrying shared DelayQueueProperties for delay queue consumers.",
 ["delay-queue","context","configuration"], "simple",
 "Holds shared delay queue properties for consumer context parts.",
 ["delay-queue","context"]),
"damai-redisson-framework/damai-service-delay-queue-framework/src/main/java/com/damai/context/DelayQueuePart.java": (
 "Context part wrapping a single ConsumerTask for delay queue consumption.",
 ["delay-queue","context","consumer"], "simple",
 "Pairs a consumer task with shared delay queue properties.",
 ["delay-queue","consumer","context"]),
"damai-redisson-framework/damai-service-delay-queue-framework/src/main/java/com/damai/core/ConsumerTask.java": (
 "Interface for delay queue consumer tasks, exposing the topic and the execute callback.",
 ["delay-queue","interface","consumer","messaging"], "simple",
 "Contract implemented by delay queue message consumers.",
 ["delay-queue","interface","consumer"]),
"damai-redisson-framework/damai-service-delay-queue-framework/src/main/java/com/damai/core/DelayConsumerQueue.java": (
 "Redisson delayed-queue listener that polls a topic and dispatches messages to its consumer task on a dedicated thread.",
 ["delay-queue","redisson","consumer","messaging"], "moderate",
 "Blocking-queue consumer loop reading delayed messages and executing the bound ConsumerTask.",
 ["delay-queue","redisson","consumer"]),
"damai-redisson-framework/damai-service-delay-queue-framework/src/main/java/com/damai/event/DelayQueueInitHandler.java": (
 "ApplicationListener that builds DelayQueuePart/DelayConsumerQueue instances for every ConsumerTask bean on context refresh.",
 ["delay-queue","spring","event-handler","initialization"], "moderate",
 "Initializes and starts one delay consumer queue per registered ConsumerTask after Spring context refresh.",
 ["delay-queue","event-handler","initialization"]),
"damai-server-client/damai-program-client/src/main/java/com/damai/dto/TestDto.java": (
 "Test DTO used by program service messaging and controller test endpoints.",
 ["data-model","dto","test"], "simple",
 "Simple test data transfer object.",
 ["dto","test","data-model"]),
"damai-server-client/damai-program-client/src/main/java/com/damai/dto/TestSendDto.java": (
 "Test DTO carrying payload for delay queue send test scenarios.",
 ["data-model","dto","test","delay-queue"], "simple",
 "Test data transfer object for delayed message sending.",
 ["dto","test","data-model"]),
"damai-server/damai-base-data-service/src/main/java/com/damai/controller/TestController.java": (
 "Test REST controller in base-data service exercising service-layer data paths for verification.",
 ["api-handler","test","controller"], "simple",
 "REST controller exposing test endpoints delegating to TestService.",
 ["controller","test","api-handler"]),
"damai-server/damai-base-data-service/src/main/java/com/damai/service/TestService.java": (
 "Test service in base-data service demonstrating data operations and string utility usage.",
 ["service","test","base-data"], "moderate",
 "Service layer with test data routines used by the base-data test controller.",
 ["service","test"]),
"damai-server/damai-order-service/src/main/java/com/damai/service/test/Test.java": (
 "Test delay queue ConsumerTask implementation in order service used to verify delayed message consumption.",
 ["delay-queue","consumer","test","order-service"], "simple",
 "ConsumerTask test implementation executing on delayed order-service messages.",
 ["delay-queue","consumer","test"]),
"damai-server/damai-program-service/src/main/java/com/damai/controller/TestController.java": (
 "Test REST controller in program service for verifying service and id generation behaviors.",
 ["api-handler","test","controller"], "simple",
 "REST controller exposing program-service test endpoints.",
 ["controller","test","api-handler"]),
"damai-server/damai-program-service/src/main/java/com/damai/service/TestService.java": (
 "Test service in program service exercising UID generator and delay queue send paths.",
 ["service","test","program-service"], "simple",
 "Service layer test routines for program service.",
 ["service","test"]),
}

# significant functions (10+ lines) to emit: path -> {name: (summary, tags)}
FN_META = {
"damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/BitsAllocator.java": {
 "BitsAllocator": ("Constructs the allocator, validating that sign, timestamp, worker id, and sequence bit widths total 64.", ["id-generator","bit-allocation","validation"]),
},
"damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/buffer/BufferPaddingExecutor.java": {
 "BufferPaddingExecutor": ("Creates the padding executor with scheduled and worker thread pools bound to a ring buffer.", ["id-generator","buffer","initialization"]),
 "paddingBuffer": ("Generates UIDs and writes them into free ring buffer slots until the buffer is full.", ["id-generator","buffer","padding"]),
},
"damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/buffer/RingBuffer.java": {
 "RingBuffer": ("Initializes the ring buffer slots, padding flags, and tail/cursor padded atomics.", ["ring-buffer","initialization","concurrency"]),
 "put": ("Atomically writes a generated UID into the next free buffer slot, delegating to the rejection handler when full.", ["ring-buffer","producer","concurrency"]),
 "take": ("Atomically reads the next available UID from the buffer and flags the slot for asynchronous padding.", ["ring-buffer","consumer","concurrency"]),
},
"damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/impl/CachedUidGenerator.java": {
 "nextIdsForOneSecond": ("Fills a list with ids for one second, used during ring buffer initialization.", ["id-generator","batch","initialization"]),
 "initRingBuffer": ("Creates and pre-fills the ring buffer, padding executor, and rejection handlers during startup.", ["ring-buffer","initialization","id-generator"]),
},
"damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/impl/DefaultUidGenerator.java": {
 "afterPropertiesSet": ("Initializes bit allocation, assigns the worker id, and validates configuration after Spring property injection.", ["id-generator","initialization","spring"]),
 "parseUid": ("Decodes a UID into its timestamp, worker id, and sequence components for diagnostics.", ["id-generator","parsing","utility"]),
 "nextId": ("Computes the next UID with sequence rollover and clock-moved-backwards protection.", ["id-generator","snowflake","concurrency"]),
},
"damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/utils/NamingThreadFactory.java": {
 "newThread": ("Builds a named thread, applying daemon flag and the shared uncaught exception handler.", ["factory","thread-pool"]),
 "getSequence": ("Returns the per-factory thread name sequence counter, initializing it lazily.", ["factory","utility"]),
},
"damai-id-generator-framework/src/main/java/com/damai/toolkit/SnowflakeIdGenerator.java": {
 "getDatacenterId": ("Derives the datacenter id from the local MAC address hash constrained to the max datacenter value.", ["snowflake","datacenter-id","resolution"]),
 "getBase": ("Resolves worker/datacenter id input from system properties, environment, or network interface as the id base.", ["snowflake","worker-id","resolution"]),
},
"damai-id-generator-framework/src/main/java/com/damai/toolkit/WorkAndDataCenterIdHandler.java": {
 "WorkAndDataCenterIdHandler": ("Initializes the handler with the WorkDataCenterId holder used for resolution output.", ["worker-id","initialization"]),
 "getWorkAndDataCenterId": ("Computes worker and datacenter ids from MAC address and configured datacenter id, storing them in the holder.", ["worker-id","datacenter-id","resolution"]),
},
"damai-redisson-framework/damai-service-delay-queue-framework/src/main/java/com/damai/core/DelayConsumerQueue.java": {
 "DelayConsumerQueue": ("Constructs the consumer queue, creating the Redisson blocking/delayed queue clients for its topic.", ["delay-queue","redisson","initialization"]),
 "listenStart": ("Starts the consumer thread loop that takes delayed messages and executes the bound consumer task.", ["delay-queue","consumer","listener"]),
},
"damai-redisson-framework/damai-service-delay-queue-framework/src/main/java/com/damai/event/DelayQueueInitHandler.java": {
 "onApplicationEvent": ("On context refresh, wraps every ConsumerTask bean in a DelayQueuePart and starts a DelayConsumerQueue per topic.", ["delay-queue","event-handler","initialization"]),
},
}

IMPLEMENTS = [
 ("damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/impl/CachedUidGenerator.java","CachedUidGenerator",
  "damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/UidGenerator.java","UidGenerator"),
 ("damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/impl/DefaultUidGenerator.java","DefaultUidGenerator",
  "damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/UidGenerator.java","UidGenerator"),
 ("damai-server/damai-order-service/src/main/java/com/damai/service/test/Test.java","Test",
  "damai-redisson-framework/damai-service-delay-queue-framework/src/main/java/com/damai/core/ConsumerTask.java","ConsumerTask"),
]

CALLS = [
 ("function","damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/impl/CachedUidGenerator.java","initRingBuffer",
  "class","damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/buffer/RingBuffer.java","RingBuffer"),
 ("function","damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/impl/CachedUidGenerator.java","initRingBuffer",
  "class","damai-id-generator-framework/src/main/java/com/baidu/fsg/uid/buffer/BufferPaddingExecutor.java","BufferPaddingExecutor"),
 ("function","damai-redisson-framework/damai-service-delay-queue-framework/src/main/java/com/damai/core/DelayConsumerQueue.java","listenStart",
  "class","damai-redisson-framework/damai-service-delay-queue-framework/src/main/java/com/damai/core/ConsumerTask.java","ConsumerTask"),
 ("function","damai-redisson-framework/damai-service-delay-queue-framework/src/main/java/com/damai/core/DelayConsumerQueue.java","DelayConsumerQueue",
  "class","damai-redisson-framework/damai-service-delay-queue-framework/src/main/java/com/damai/context/DelayQueuePart.java","DelayQueuePart"),
 ("function","damai-redisson-framework/damai-service-delay-queue-framework/src/main/java/com/damai/event/DelayQueueInitHandler.java","onApplicationEvent",
  "class","damai-redisson-framework/damai-service-delay-queue-framework/src/main/java/com/damai/core/DelayConsumerQueue.java","DelayConsumerQueue"),
 ("function","damai-redisson-framework/damai-service-delay-queue-framework/src/main/java/com/damai/event/DelayQueueInitHandler.java","onApplicationEvent",
  "class","damai-redisson-framework/damai-service-delay-queue-framework/src/main/java/com/damai/context/DelayQueuePart.java","DelayQueuePart"),
 ("function","damai-id-generator-framework/src/main/java/com/damai/toolkit/WorkAndDataCenterIdHandler.java","getWorkAndDataCenterId",
  "class","damai-id-generator-framework/src/main/java/com/damai/toolkit/WorkDataCenterId.java","WorkDataCenterId"),
]

nodes, edges = [], []
for path, meta in META.items():
    r = results[path]
    fsum, ftags, fcomp, csum, ctags = meta
    fname = path.split("/")[-1]
    nodes.append({"id": f"file:{path}", "type": "file", "name": fname, "filePath": path,
                  "summary": fsum, "tags": ftags, "complexity": fcomp})
    cls = r["classes"][0]
    cname = cls["name"]
    clen = cls["endLine"] - cls["startLine"] + 1
    ccomp = "complex" if clen > 200 else ("moderate" if clen > 50 else "simple")
    cid = f"class:{path}:{cname}"
    nodes.append({"id": cid, "type": "class", "name": cname, "filePath": path,
                  "lineRange": [cls["startLine"], cls["endLine"]],
                  "summary": csum, "tags": ctags, "complexity": ccomp})
    edges.append({"source": f"file:{path}", "target": cid, "type": "contains", "direction": "forward", "weight": 1.0})
    # significant functions
    fmeta = FN_META.get(path, {})
    for f in r.get("functions", []):
        if f["endLine"] - f["startLine"] + 1 >= 10:
            nm = f["name"]
            s, t = fmeta.get(nm, (f"Method {nm} of {cname}.", ["method"]))
            fid = f"function:{path}:{nm}"
            nodes.append({"id": fid, "type": "function", "name": nm, "filePath": path,
                          "lineRange": [f["startLine"], f["endLine"]],
                          "summary": s, "tags": t, "complexity": "simple"})
            edges.append({"source": f"file:{path}", "target": fid, "type": "contains", "direction": "forward", "weight": 1.0})
    # imports
    for tgt in imports.get(path, []):
        edges.append({"source": f"file:{path}", "target": f"file:{tgt}", "type": "imports", "direction": "forward", "weight": 0.7})

for sp, sc, tp, tc in IMPLEMENTS:
    edges.append({"source": f"class:{sp}:{sc}", "target": f"class:{tp}:{tc}", "type": "implements", "direction": "forward", "weight": 0.9})
for sk, sp, sn, tk, tp, tn in CALLS:
    edges.append({"source": f"{sk}:{sp}:{sn}", "target": f"{tk}:{tp}:{tn}", "type": "calls", "direction": "forward", "weight": 0.8})

# verify import edge count
imp_expected = sum(len(v) for v in imports.values())
imp_actual = sum(1 for e in edges if e["type"] == "imports")
assert imp_expected == imp_actual, (imp_expected, imp_actual)

# dedupe safety
ids = [n["id"] for n in nodes]
assert len(ids) == len(set(ids)), "dup node ids"
srcs = {e["source"] for e in edges}
for e in edges:
    assert e["source"] != e["target"]

print("total nodes", len(nodes), "edges", len(edges))

# split: Step C partition
files_sorted = sorted(META.keys())
node_count, edge_count = len(nodes), len(edges)
if node_count <= 60 and edge_count <= 120:
    json.dump({"nodes": nodes, "edges": edges}, open(f"{UA}/intermediate/batch-1.json", "w"), indent=1)
    print("wrote batch-1.json")
else:
    parts = math.ceil(max(node_count/60, edge_count/120))
    chunk = math.ceil(len(files_sorted)/parts)
    for k in range(parts):
        group = set(files_sorted[k*chunk:(k+1)*chunk])
        pnodes = [n for n in nodes if n.get("filePath") in group]
        pids = {n["id"] for n in pnodes}
        pedges = [e for e in edges if e["source"] in pids]
        out = f"{UA}/intermediate/batch-1-part-{k+1}.json"
        json.dump({"nodes": pnodes, "edges": pedges}, open(out, "w"), indent=1)
        print(f"wrote batch-1-part-{k+1}.json nodes={len(pnodes)} edges={len(pedges)}")
