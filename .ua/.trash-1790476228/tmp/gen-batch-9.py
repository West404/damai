import json

inp = json.load(open('E:/WorkSpace/damai/.ua/intermediate/batch-input-9.json'))
imp = inp['batchImportData']

P = 'damai-server-client/damai-program-client/src/main/java/com/damai/vo/'
GW = 'damai-server/damai-gateway-service/src/main/java/com/damai/kafka/'
OS = 'damai-server/damai-order-service/src/main/java/com/damai/'
PS = 'damai-server/damai-program-service/src/main/java/com/damai/'

def fnode(path, summary, tags, complexity, notes=None):
    n = {"id": "file:"+path, "type": "file", "name": path.split('/')[-1],
         "filePath": path, "summary": summary, "tags": tags, "complexity": complexity}
    if notes: n["languageNotes"] = notes
    return n

def cnode(path, cname, lr, summary, tags, complexity="simple"):
    return {"id": "class:%s:%s" % (path, cname), "type": "class", "name": cname,
            "filePath": path, "lineRange": lr, "summary": summary, "tags": tags, "complexity": complexity}

def fnode2(path, fname, lr, summary, tags, complexity="simple"):
    return {"id": "function:%s:%s" % (path, fname), "type": "function", "name": fname,
            "filePath": path, "lineRange": lr, "summary": summary, "tags": tags, "complexity": complexity}

def edge(s, t, ty, w):
    return {"source": s, "target": t, "type": ty, "direction": "forward", "weight": w}

part1_files = [
    P+'ProgramGroupVo.java', P+'ProgramHomeVo.java', P+'ProgramListVo.java',
    P+'ProgramSimpleInfoVo.java', P+'ProgramVo.java',
    GW+'ApiDataMessageSend.java',
    OS+'scheduletask/OrderDataTask.java',
    OS+'service/delaysend/DelayOperateProgramDataSend.java',
    PS+'controller/ProgramController.java',
    PS+'controller/ProgramResetController.java',
    PS+'controller/ProgramShowTimeController.java',
    PS+'entity/Program.java',
    PS+'entity/ProgramGroup.java',
]
part2_files = [
    PS+'entity/ProgramJoinShowTime.java',
    PS+'entity/ProgramShowTime.java',
    PS+'entity/TicketCategory.java',
    PS+'entity/TicketCategoryAggregate.java',
    PS+'mapper/ProgramGroupMapper.java',
    PS+'mapper/ProgramMapper.java',
    PS+'mapper/ProgramShowTimeMapper.java',
    PS+'mapper/TicketCategoryMapper.java',
    PS+'service/ProgramService.java',
    PS+'service/ProgramShowTimeService.java',
    PS+'service/cache/local/LocalCacheProgram.java',
    PS+'service/cache/local/LocalCacheProgramGroup.java',
]

file_meta = {
 P+'ProgramGroupVo.java': ("View object carrying program group (performing-artist group) data returned to clients, including artist names and program count.", ["data-model","vo","program-group","serialization"], "simple", None),
 P+'ProgramHomeVo.java': ("View object representing a program entry on the homepage, with program id, title, poster and minimum price for listing rendering.", ["data-model","vo","homepage","serialization"], "simple", None),
 P+'ProgramListVo.java': ("View object for program list/search results, bundling program details, show time, ticket price range and category info.", ["data-model","vo","search-result","serialization"], "simple", None),
 P+'ProgramSimpleInfoVo.java': ("Lightweight view object with minimal program fields (id, title) used for cache-friendly quick lookups.", ["data-model","vo","lightweight","serialization"], "simple", None),
 P+'ProgramVo.java': ("Full view object describing a program (performance event) with title, place, show times, ticket categories and descriptive content for detail pages.", ["data-model","vo","program-detail","serialization"], "moderate", None),
 GW+'ApiDataMessageSend.java': ("Kafka message producer in the gateway service that sends API request data records to a topic prefixed with the application distinction name.", ["kafka","message-producer","gateway","api-data"], "simple", None),
 OS+'scheduletask/OrderDataTask.java': ("Scheduled task in the order service that runs daily at 23:00 to asynchronously clean up orders and order-ticket-user records via the business thread pool.", ["scheduled-task","order","cleanup","async"], "simple", None),
 OS+'service/delaysend/DelayOperateProgramDataSend.java': ("Delay-queue message sender that schedules program-data update messages after an order is paid, using the Redisson-based delay queue.", ["delay-queue","message-producer","order","program-data"], "simple", None),
 PS+'controller/ProgramController.java': ("REST controller exposing program endpoints: add, search, homepage list, paged list, recommendations, multi-version detail queries, invalidation and local-cache detail.", ["api-handler","rest-controller","program","endpoint"], "moderate", None),
 PS+'controller/ProgramResetController.java': ("REST controller exposing the endpoint to reset a program's execution data (idempotent program-data reset).", ["api-handler","rest-controller","program-reset"], "simple", None),
 PS+'controller/ProgramShowTimeController.java': ("REST controller exposing the endpoint to add a show time for a program.", ["api-handler","rest-controller","show-time"], "simple", None),
 PS+'entity/Program.java': ("MyBatis-Plus entity mapping the program table: core performance-event record with title, category, area, time, sales status and descriptive fields.", ["data-model","entity","mybatis-plus","program"], "complex", "Lombok @Data entity extending BaseTableData for shared audit fields."),
 PS+'entity/ProgramGroup.java': ("MyBatis-Plus entity mapping the program_group table, linking a program to a group of performing artists.", ["data-model","entity","mybatis-plus","program-group"], "simple", None),
 PS+'entity/ProgramJoinShowTime.java': ("Entity joining program and show-time data, used as a flat query result for program listings with their earliest show time.", ["data-model","entity","join-result","show-time"], "simple", None),
 PS+'entity/ProgramShowTime.java': ("MyBatis-Plus entity mapping the program_show_time table, recording each performance date/time of a program.", ["data-model","entity","mybatis-plus","show-time"], "simple", None),
 PS+'entity/TicketCategory.java': ("MyBatis-Plus entity mapping the ticket_category table: price tiers for a program with stock (total/remain number) and sell status.", ["data-model","entity","mybatis-plus","ticket-category"], "moderate", None),
 PS+'entity/TicketCategoryAggregate.java': ("Aggregation result entity holding per-program ticket category statistics such as minimum price.", ["data-model","entity","aggregation","ticket-category"], "simple", None),
 PS+'mapper/ProgramGroupMapper.java': ("MyBatis-Plus mapper interface for CRUD operations on the program_group table.", ["mapper","mybatis-plus","data-access"], "simple", None),
 PS+'mapper/ProgramMapper.java': ("MyBatis-Plus mapper interface for the program table with custom join and paged-list queries.", ["mapper","mybatis-plus","data-access","program"], "simple", None),
 PS+'mapper/ProgramShowTimeMapper.java': ("MyBatis-Plus mapper interface for CRUD operations on the program_show_time table.", ["mapper","mybatis-plus","data-access"], "simple", None),
 PS+'mapper/TicketCategoryMapper.java': ("MyBatis-Plus mapper interface for the ticket_category table with custom aggregate and count queries.", ["mapper","mybatis-plus","data-access","ticket-category"], "simple", None),
 PS+'service/ProgramService.java': ("Core service of the program domain: program CRUD, multi-level caching (local cache, Redis, Elasticsearch), detail/search/list queries with distributed locks, and preloading of ticket-user and order-count data.", ["service","program","multi-level-cache","distributed-lock","elasticsearch"], "complex", "Combines local cache, RedisCache and Elasticsearch with ServiceLockTool and repeat-execute limiting for high-concurrency reads."),
 PS+'service/ProgramShowTimeService.java': ("Service managing program show times: adding show times with generated ids and multi-level cached lookup plus Redis-lock-protected renewal of show-time data.", ["service","show-time","cache","distributed-lock"], "moderate", None),
 PS+'service/cache/local/LocalCacheProgram.java': ("Caffeine-based local in-memory cache for ProgramVo detail data with lock-protected initialization and TTL expiry.", ["local-cache","caffeine","program","performance"], "moderate", None),
 PS+'service/cache/local/LocalCacheProgramGroup.java': ("Caffeine-based local in-memory cache for ProgramGroupVo data with lock-protected initialization and TTL expiry.", ["local-cache","caffeine","program-group","performance"], "moderate", None),
}

classes = [
 (P+'ProgramGroupVo.java','ProgramGroupVo',[16,31],"Program group view object exposing artist group and program summary fields.",["data-model","vo"]),
 (P+'ProgramHomeVo.java','ProgramHomeVo',[15,30],"Homepage program view object with id, title, poster and price fields.",["data-model","vo"]),
 (P+'ProgramListVo.java','ProgramListVo',[16,75],"Program list view object combining program, show-time and pricing fields for search/list responses.",["data-model","vo"]),
 (P+'ProgramSimpleInfoVo.java','ProgramSimpleInfoVo',[14,29],"Minimal program view object for lightweight cached lookups.",["data-model","vo"]),
 (P+'ProgramVo.java','ProgramVo',[16,184],"Comprehensive program view object including ticket categories and show-time details.",["data-model","vo"]),
 (GW+'ApiDataMessageSend.java','ApiDataMessageSend',[13,25],"Kafka producer component sending API metrics data with a prefixed topic name.",["kafka","message-producer"]),
 (OS+'scheduletask/OrderDataTask.java','OrderDataTask',[15,33],"Spring scheduled component triggering daily order data cleanup in a background thread pool.",["scheduled-task","order"]),
 (OS+'service/delaysend/DelayOperateProgramDataSend.java','DelayOperateProgramDataSend',[18,34],"Component enqueueing delayed program-data-operation messages into the delay queue.",["delay-queue","message-producer"]),
 (PS+'controller/ProgramController.java','ProgramController',[32,100],"REST controller with 10 endpoints delegating to ProgramService for program queries and mutations.",["api-handler","rest-controller"]),
 (PS+'controller/ProgramResetController.java','ProgramResetController',[20,33],"REST controller delegating program reset-execution requests to ProgramService.",["api-handler","rest-controller"]),
 (PS+'controller/ProgramShowTimeController.java','ProgramShowTimeController',[20,33],"REST controller delegating show-time creation to ProgramShowTimeService.",["api-handler","rest-controller"]),
 (PS+'entity/Program.java','Program',[16,247],"Program table entity extending BaseTableData with full performance-event fields.",["data-model","entity"]),
 (PS+'entity/ProgramGroup.java','ProgramGroup',[16,37],"Program group table entity extending BaseTableData.",["data-model","entity"]),
 (PS+'entity/ProgramJoinShowTime.java','ProgramJoinShowTime',[14,34],"Flat join entity combining program fields with its show time.",["data-model","entity"]),
 (PS+'entity/ProgramShowTime.java','ProgramShowTime',[16,47],"Program show-time table entity extending BaseTableData.",["data-model","entity"]),
 (PS+'entity/TicketCategory.java','TicketCategory',[16,54],"Ticket category table entity extending BaseTableData with price and stock fields.",["data-model","entity"]),
 (PS+'entity/TicketCategoryAggregate.java','TicketCategoryAggregate',[13,30],"Aggregate query result for ticket category statistics per program.",["data-model","entity"]),
 (PS+'mapper/ProgramGroupMapper.java','ProgramGroupMapper',[12,14],"MyBatis-Plus BaseMapper interface for program_group.",["mapper","data-access"]),
 (PS+'mapper/ProgramMapper.java','ProgramMapper',[19,36],"MyBatis-Plus mapper interface with custom program list/page queries.",["mapper","data-access"]),
 (PS+'mapper/ProgramShowTimeMapper.java','ProgramShowTimeMapper',[11,13],"MyBatis-Plus BaseMapper interface for program_show_time.",["mapper","data-access"]),
 (PS+'mapper/TicketCategoryMapper.java','TicketCategoryMapper',[16,43],"MyBatis-Plus mapper interface with custom ticket category aggregate/count queries.",["mapper","data-access"]),
 (PS+'service/ProgramService.java','ProgramService',[115,880],"Program domain service orchestrating DB, local cache, Redis and Elasticsearch access with lock protection.",["service","program","cache"]),
 (PS+'service/ProgramShowTimeService.java','ProgramShowTimeService',[53,201],"Show-time service with cached lookups and lock-protected renewal logic.",["service","show-time"]),
 (PS+'service/cache/local/LocalCacheProgram.java','LocalCacheProgram',[22,77],"Caffeine local cache wrapper for ProgramVo entries.",["local-cache","caffeine"]),
 (PS+'service/cache/local/LocalCacheProgramGroup.java','LocalCacheProgramGroup',[22,74],"Caffeine local cache wrapper for ProgramGroupVo entries.",["local-cache","caffeine"]),
]

functions = [
 (OS+'scheduletask/OrderDataTask.java','executeTask',[22,32],"Cron-triggered task (daily 23:00) submitting order and ticket-user cleanup to BusinessThreadPool.",["scheduled-task","cleanup","async"]),
 (OS+'service/delaysend/DelayOperateProgramDataSend.java','sendMessage',[25,33],"Sends a delayed program-data-operation message via DelayQueueContext with the application-prefixed topic.",["delay-queue","message-producer"]),
 (PS+'service/ProgramService.java','dbSelectHomeList',[242,300],"Queries homepage program lists directly from the database grouped by program category and show time.",["database","homepage","query"],"moderate"),
 (PS+'service/ProgramService.java','setQueryTime',[306,337],"Computes the effective query time range for program listings based on the requested time type.",["time-range","query-helper"],"moderate"),
 (PS+'service/ProgramService.java','dbSelectPage',[368,407],"Executes a paged program query against the database, resolving area names via the base-data client.",["database","pagination","query"],"moderate"),
 (PS+'service/ProgramService.java','getDetail',[444,474],"Assembles a full program detail view including group, category map and ticket category data.",["program-detail","assembly"],"moderate"),
 (PS+'service/ProgramService.java','getDetailV2',[481,512],"V2 detail assembly returning the extended ProgramVo with additional preloaded data.",["program-detail","assembly"],"moderate"),
 (PS+'service/ProgramService.java','getByIdMultipleCache',[520,529],"Fetches a ProgramVo through the multi-level cache chain (local cache backed by Redis/DB loader).",["cache","multi-level-cache"]),
 (PS+'service/ProgramService.java','simpleGetProgramAndShowMultipleCache',[540,557],"Fetches a program plus its show time through the multi-level cache, returning a combined view.",["cache","multi-level-cache"]),
 (PS+'service/ProgramService.java','getById',[559,578],"Loads a program detail from cache or DB under a reentrant distributed lock to prevent cache breakdown.",["distributed-lock","cache","database"]),
 (PS+'service/ProgramService.java','getProgramGroup',[585,606],"Loads a program group from cache or DB under a reentrant distributed lock.",["distributed-lock","cache","database"]),
 (PS+'service/ProgramService.java','operateProgramData',[625,659],"Applies program-data updates (e.g. remaining ticket counts) to cache and database after order operations.",["program-data","update","cache"],"moderate"),
 (PS+'service/ProgramService.java','createProgramVo',[661,678],"Builds a ProgramVo from the Program entity, enriching it with area data from the base-data client.",["vo-assembly","enrichment"]),
 (PS+'service/ProgramService.java','getDetailFromDb',[699,721],"Loads the complete program detail from the database as the fallback path of the cache chain.",["database","program-detail"]),
 (PS+'service/ProgramService.java','preloadTicketUserList',[723,756],"Preloads ticket-buyer verification data from the user service into Redis with per-user token expiry.",["preload","redis","user-client"],"moderate"),
 (PS+'service/ProgramService.java','preloadAccountOrderCount',[758,790],"Preloads per-account order counts from the order service into Redis for purchase-limit checks.",["preload","redis","order-client"],"moderate"),
 (PS+'service/ProgramService.java','resetExecute',[801,837],"Resets program execution data: refreshes caches and re-pushes program data for a clean selling state.",["reset","cache","idempotent"],"moderate"),
 (PS+'service/ProgramService.java','delRedisData',[839,852],"Deletes all Redis keys related to a program via the Lua-based cache deletion handler.",["redis","cache-eviction","lua"]),
 (PS+'service/ProgramService.java','invalid',[854,867],"Marks a program as invalid, removes its Elasticsearch document and pushes a Redis Stream message for downstream consumers.",["invalidation","elasticsearch","redis-stream"]),
 (PS+'service/ProgramShowTimeService.java','selectProgramShowTimeByProgramId',[106,131],"Loads the show time for a program from cache or DB under a reentrant distributed lock.",["distributed-lock","cache","show-time"],"moderate"),
 (PS+'service/ProgramShowTimeService.java','renewal',[136,200],"Renews show-time data by reconciling DB state with cached data under lock protection.",["renewal","distributed-lock","show-time"],"complex"),
 (PS+'service/cache/local/LocalCacheProgram.java','localLockCacheInit',[37,61],"Initializes the Caffeine cache with expire-after-write TTL and a loader guarded for single-flight loading.",["cache-init","caffeine"]),
 (PS+'service/cache/local/LocalCacheProgramGroup.java','localLockCacheInit',[37,62],"Initializes the Caffeine cache for program groups with TTL expiry and a guarded loader.",["cache-init","caffeine"]),
]

BTP = 'damai-thread-pool-framework/src/main/java/com/damai/BusinessThreadPool.java'
ORDSVC = 'damai-server/damai-order-service/src/main/java/com/damai/service/OrderService.java'
DQC = 'damai-redisson-framework/damai-service-delay-queue-framework/src/main/java/com/damai/context/DelayQueueContext.java'
BTD = 'damai-spring-cloud-framework/damai-service-common/src/main/java/com/damai/data/BaseTableData.java'
PROGES = 'damai-server/damai-program-service/src/main/java/com/damai/service/es/ProgramEs.java'
SLT = 'damai-redisson-framework/damai-redisson-service-framework/damai-service-lock-framework/src/main/java/com/damai/util/ServiceLockTool.java'
LCP = PS+'service/cache/local/LocalCacheProgram.java'

part_of = {p: 1 for p in part1_files}
part_of.update({p: 2 for p in part2_files})

nodes1, nodes2, edges1, edges2 = [], [], [], []

for path in part1_files + part2_files:
    s, t, c, n = file_meta[path]
    node = fnode(path, s, t, c, n)
    (nodes1 if part_of[path] == 1 else nodes2).append(node)

for path, cname, lr, s, t in classes:
    node = cnode(path, cname, lr, s, t)
    (nodes1 if part_of[path] == 1 else nodes2).append(node)
    tgt = edges1 if part_of[path] == 1 else edges2
    tgt.append(edge("file:"+path, node["id"], "contains", 1.0))
    tgt.append(edge("file:"+path, node["id"], "exports", 0.8))

for item in functions:
    path, fname, lr, s, t = item[0], item[1], item[2], item[3], item[4]
    cx = item[5] if len(item) > 5 else "simple"
    node = fnode2(path, fname, lr, s, t, cx)
    (nodes1 if part_of[path] == 1 else nodes2).append(node)
    (edges1 if part_of[path] == 1 else edges2).append(edge("file:"+path, node["id"], "contains", 1.0))

for path, targets in imp.items():
    tgt_edges = edges1 if part_of[path] == 1 else edges2
    for tp in targets:
        tgt_edges.append(edge("file:"+path, "file:"+tp, "imports", 0.7))

for path, cname in [(PS+'entity/Program.java','Program'), (PS+'entity/ProgramGroup.java','ProgramGroup'),
                    (PS+'entity/ProgramShowTime.java','ProgramShowTime'), (PS+'entity/TicketCategory.java','TicketCategory')]:
    (edges1 if part_of[path] == 1 else edges2).append(
        edge("class:%s:%s" % (path, cname), "class:%s:BaseTableData" % BTD, "inherits", 0.9))

edges1.append(edge("function:%sexecuteTask" % (OS+'scheduletask/OrderDataTask.java:').replace(':.java:', '.java:') if False else "function:%s:executeTask" % (OS+'scheduletask/OrderDataTask.java'),
                   "function:%s:execute" % BTP, "calls", 0.8))
edges1.append(edge("function:%s:executeTask" % (OS+'scheduletask/OrderDataTask.java'),
                   "function:%s:delOrderAndOrderTicketUser" % ORDSVC, "calls", 0.8))
edges1.append(edge("function:%s:sendMessage" % (OS+'service/delaysend/DelayOperateProgramDataSend.java'),
                   "function:%s:sendMessage" % DQC, "calls", 0.8))
edges2.append(edge("function:%s:getByIdMultipleCache" % (PS+'service/ProgramService.java'),
                   "class:%s:LocalCacheProgram" % LCP, "calls", 0.8))
edges2.append(edge("function:%s:invalid" % (PS+'service/ProgramService.java'),
                   "function:%s:deleteByProgramId" % PROGES, "calls", 0.8))
edges2.append(edge("function:%s:selectProgramShowTimeByProgramId" % (PS+'service/ProgramShowTimeService.java'),
                   "function:%s:getLock" % SLT, "calls", 0.8))

json.dump({"nodes": nodes1, "edges": edges1}, open('E:/WorkSpace/damai/.ua/intermediate/batch-9-part-1.json', 'w'), indent=1, ensure_ascii=False)
json.dump({"nodes": nodes2, "edges": edges2}, open('E:/WorkSpace/damai/.ua/intermediate/batch-9-part-2.json', 'w'), indent=1, ensure_ascii=False)

imp_total = sum(len(v) for v in imp.values())
imp_emitted = sum(1 for e in edges1+edges2 if e['type'] == 'imports')
print("part1 nodes", len(nodes1), "edges", len(edges1))
print("part2 nodes", len(nodes2), "edges", len(edges2))
print("imports required", imp_total, "emitted", imp_emitted, "MATCH" if imp_total == imp_emitted else "MISMATCH")
