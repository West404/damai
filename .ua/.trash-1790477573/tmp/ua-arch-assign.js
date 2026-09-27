const fs = require('fs');

const inputPath = process.argv[2];
const outPath = process.argv[3];
if (!inputPath || !outPath) {
  process.stderr.write('usage: node ua-arch-assign.js <input.json> <layers.json>\n');
  process.exit(1);
}

const input = JSON.parse(fs.readFileSync(inputPath, 'utf8'));
const fileNodes = input.fileNodes;
const byId = new Map(fileNodes.map((n) => [n.id, n]));

const LAYER_ORDER = [
  'layer:api-gateway',
  'layer:business-service',
  'layer:service-client',
  'layer:cloud-framework',
  'layer:core-library',
  'layer:middleware-framework',
  'layer:data',
  'layer:config',
  'layer:infrastructure',
  'layer:documentation',
];

const assign = new Map(fileNodes.map((n) => [n.id, null]));
const reason = {};

function put(node, layer) {
  if (!assign.has(node.id)) throw new Error('unknown node ' + node.id);
  if (layers[node.id]) throw new Error('duplicate assignment ' + node.id);
  layers[node.id] = layer;
  reason[layer] = reason[layer] || {};
  const key = node.type + ':' + (ruleOf(node) || 'other');
  reason[layer][key] = (reason[layer][key] || 0) + 1;
}

const layers = {};

let currentRule = '';
function ruleOf() {
  return currentRule;
}

for (const n of fileNodes) {
  const p = n.filePath.replace(/\\/g, '/');
  const isCode = n.type === 'file';

  currentRule = 'rule';
  if (n.type === 'table') {
    put(n, 'layer:data');
  } else if (n.type === 'config') {
    put(n, 'layer:config');
  } else if (n.type === 'document') {
    put(n, 'layer:documentation');
  } else if (n.type === 'service') {
    put(n, 'layer:infrastructure');
  } else if (n.type === 'file') {
    currentRule = 'by-path';
    if (p === 'spotless/license-header') put(n, 'layer:infrastructure');
    else if (p.startsWith('damai-server/damai-gateway-service/')) put(n, 'layer:api-gateway');
    else if (p.startsWith('damai-server/')) put(n, 'layer:business-service');
    else if (p.startsWith('damai-server-client/')) put(n, 'layer:service-client');
    else if (p.startsWith('damai-spring-cloud-framework/')) put(n, 'layer:cloud-framework');
    else if (p.startsWith('damai-common/')) put(n, 'layer:core-library');
    else if (
      p.startsWith('damai-redisson-framework/') ||
      p.startsWith('damai-redis-tool-framework/') ||
      p.startsWith('damai-id-generator-framework/') ||
      p.startsWith('damai-elasticsearch-framework/') ||
      p.startsWith('damai-thread-pool-framework/') ||
      p.startsWith('damai-captcha-manage-framework/')
    )
      put(n, 'layer:middleware-framework');
    else put(n, 'layer:unassigned');
  } else {
    put(n, 'layer:unassigned');
  }
}

// ---- build output ----
const META = {
  'layer:api-gateway': {
    name: 'API 网关与入口层',
    description:
      '基于 Spring Cloud Gateway 的网关服务，承担统一路由转发、全局过滤器链、限流熔断与参数校验入口，是大麦系统对外的唯一流量入口。',
  },
  'layer:business-service': {
    name: '业务微服务层',
    description:
      '用户、订单、支付、节目、基础数据、定制与后台管理等 Spring Cloud 业务微服务，通过 Controller-Service-Mapper 分层实现订票、抢票、支付与订单履约等高并发核心业务。',
  },
  'layer:service-client': {
    name: '微服务客户端层',
    description:
      '各业务微服务对外暴露的 Feign 客户端与请求/响应契约模块，供其他服务远程调用，隔离服务内部实现与跨服务接口约定。',
  },
  'layer:cloud-framework': {
    name: 'Spring Cloud 通用框架层',
    description:
      '面向微服务的通用组件库：服务初始化装配、Nacos/Sentinel 接入、灰度发布（gray-transition）与公共 Web 组件，所有业务服务通过自动装配复用。',
  },
  'layer:core-library': {
    name: '公共基础库层',
    description:
      'damai-common 提供全项目共用的统一响应体、错误码与业务枚举、统一异常、日期与加密工具、JWT、ThreadLocal 上下文及 Log4j2 JSON 日志布局。',
  },
  'layer:middleware-framework': {
    name: '分布式中间件框架层',
    description:
      '自研中间件能力组件库：Redis 缓存与 Stream 消息、Redisson 分布式锁/布隆过滤器/重复执行限制/延迟队列、雪花算法 ID 生成器、Elasticsearch 检索、业务线程池与 AJ-Captcha 图形验证码。',
  },
  'layer:data': {
    name: '数据层',
    description:
      'MySQL 建库建表脚本与分库分表后的物理表结构（订单、支付、用户、节目、基础数据、定制等按分片键拆分的 0/1 分片表），是 ShardingSphere 分片规则对应的落地 Schema。',
  },
  'layer:config': {
    name: '配置层',
    description:
      'Maven 模块 POM 依赖与构建配置、各微服务的 application/log4j2 运行配置、ShardingSphere 分库分表与 MyBatis Mapper XML、代码格式化（spotless）等配置。',
  },
  'layer:infrastructure': {
    name: '部署与基础设施层',
    description:
      'Dockerfile 多阶段镜像构建、docker-compose 编排的 MySQL/Redis/Nacos/Kafka/Elasticsearch 中间件与八个微服务实例，以及 spotless 许可头等工程基础设施资产。',
  },
  'layer:documentation': {
    name: '文档层',
    description:
      '项目总览与技术选型说明、ID 生成器模块说明、Docker 部署说明及验证码字体许可文件。',
  },
};

const out = LAYER_ORDER.map((id) => ({
  id,
  name: META[id].name,
  description: META[id].description,
  nodeIds: [],
}));
const idx = new Map(out.map((l) => [l.id, l]));

for (const n of fileNodes) {
  const layer = layers[n.id];
  if (!layer) throw new Error('unassigned node ' + n.id);
  if (!idx.has(layer)) throw new Error('unknown layer ' + layer + ' for ' + n.id);
  idx.get(layer).nodeIds.push(n.id);
}

// ---- verification ----
let total = 0;
for (const l of out) total += l.nodeIds.length;
const allIds = new Set();
for (const l of out) for (const id of l.nodeIds) {
  if (allIds.has(id)) throw new Error('node in 2 layers: ' + id);
  allIds.add(id);
}
if (total !== fileNodes.length) throw new Error('count mismatch: ' + total + ' vs ' + fileNodes.length);
if (allIds.size !== fileNodes.length) throw new Error('coverage mismatch');
for (const l of out) if (l.nodeIds.length === 0) throw new Error('empty layer ' + l.id);

fs.writeFileSync(outPath, JSON.stringify(out, null, 2));

console.log('layers=' + out.length + ' totalNodes=' + total + ' (expected ' + fileNodes.length + ')');
for (const l of out) {
  const byType = {};
  for (const id of l.nodeIds) {
    const t = byId.get(id).type;
    byType[t] = (byType[t] || 0) + 1;
  }
  console.log(
    String(l.nodeIds.length).padStart(5),
    l.id.padEnd(34),
    JSON.stringify(byType)
  );
}
process.exit(0);
