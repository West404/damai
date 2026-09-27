const fs = require('fs');
const path = require('path');

function fail(msg) {
  process.stderr.write(String(msg) + '\n');
  process.exit(1);
}

const inputPath = process.argv[2];
const outputPath = process.argv[3];
if (!inputPath || !outputPath) fail('usage: node ua-arch-analyze.js <input.json> <output.json>');

let data;
try {
  data = JSON.parse(fs.readFileSync(inputPath, 'utf8'));
} catch (e) {
  fail('failed to read/parse input: ' + e.message);
}

const fileNodes = data.fileNodes || [];
const importEdges = data.importEdges || [];
const allEdges = data.allEdges || [];

// ---------- helpers ----------
const norm = (p) => String(p || '').replace(/\\/g, '/');

function commonPrefixLen(paths) {
  if (!paths.length) return 0;
  const split = paths.map((p) => p.split('/'));
  let n = 0;
  const first = split[0];
  while (n < first.length - 1) {
    const seg = first[n];
    if (split.every((s) => s.length > n + 1 && s[n] === seg)) n++;
    else break;
  }
  return n;
}

const paths = fileNodes.map((n) => norm(n.filePath));
let prefixSegs = commonPrefixLen(paths);
// A common prefix that swallows everything (flat structure) is useless.
const groupKey = (p, depth) => {
  const segs = norm(p).split('/');
  const rest = segs.slice(prefixSegs);
  if (rest.length <= 1) return 'root';
  return rest.slice(0, depth).join('/');
};

const byId = new Map(fileNodes.map((n) => [n.id, n]));

// ---------- A. Directory grouping ----------
const directoryGroups = {};
const directoryGroupsLevel2 = {};
for (const n of fileNodes) {
  const p = norm(n.filePath);
  const g1 = groupKey(p, 1);
  const g2 = groupKey(p, 2);
  (directoryGroups[g1] = directoryGroups[g1] || []).push(n.id);
  (directoryGroupsLevel2[g2] = directoryGroupsLevel2[g2] || []).push(n.id);
}

// ---------- B. Node type grouping ----------
const nodeTypeGroups = {};
for (const n of fileNodes) {
  (nodeTypeGroups[n.type] = nodeTypeGroups[n.type] || []).push(n.id);
}

// ---------- C/D/E/F. Import adjacency ----------
const fanOut = {};
const fanIn = {};
const adjacent = new Map();
for (const e of importEdges) {
  if (!byId.has(e.source) || !byId.has(e.target)) continue;
  fanOut[e.source] = (fanOut[e.source] || 0) + 1;
  fanIn[e.target] = (fanIn[e.target] || 0) + 1;
  if (!adjacent.has(e.source)) adjacent.set(e.source, new Set());
  adjacent.get(e.source).add(e.target);
}

const groupOf = new Map();
for (const [g, ids] of Object.entries(directoryGroups)) for (const id of ids) groupOf.set(id, g);

const interGroupImports = {};
const intraCount = {};
const groupTotalEdges = {};
for (const e of importEdges) {
  const gs = groupOf.get(e.source);
  const gt = groupOf.get(e.target);
  if (!gs || !gt) continue;
  groupTotalEdges[gs] = (groupTotalEdges[gs] || 0) + 1;
  groupTotalEdges[gt] = (groupTotalEdges[gt] || 0) + 1;
  if (gs === gt) {
    intraCount[gs] = (intraCount[gs] || 0) + 1;
  } else {
    const k = gs + '\u0000' + gt;
    interGroupImports[k] = (interGroupImports[k] || 0) + 1;
  }
}

// ---------- D. Cross-category edges ----------
const crossCategoryMap = {};
for (const e of allEdges) {
  const s = byId.get(e.source);
  const t = byId.get(e.target);
  if (!s || !t) continue;
  const k = s.type + '\u0000' + t.type + '\u0000' + e.type;
  crossCategoryMap[k] = (crossCategoryMap[k] || 0) + 1;
}

// ---------- G. Directory pattern matching ----------
const DIRECTORY_PATTERNS = [
  [['routes', 'api', 'controllers', 'controller', 'endpoints', 'handlers', 'serializers', 'routers', 'blueprints'], 'api'],
  [['services', 'service', 'core', 'lib', 'domain', 'logic', 'internal', 'signals', 'mailers', 'jobs', 'channels', 'composables', 'src/main/java'], 'service'],
  [['models', 'db', 'data', 'persistence', 'repository', 'entities', 'entity', 'migrations', 'sql', 'database', 'schema'], 'data'],
  [['components', 'views', 'pages', 'ui', 'layouts', 'screens'], 'ui'],
  [['middleware', 'plugins', 'interceptors', 'guards'], 'middleware'],
  [['utils', 'helpers', 'common', 'shared', 'tools', 'pkg', 'templatetags'], 'utility'],
  [['config', 'constants', 'env', 'settings', 'management', 'commands'], 'config'],
  [['__tests__', 'test', 'tests', 'spec', 'specs'], 'test'],
  [['types', 'interfaces', 'schemas', 'contracts', 'dtos', 'dto', 'request', 'response'], 'types'],
  [['hooks'], 'hooks'],
  [['store', 'state', 'reducers', 'actions', 'slices'], 'state'],
  [['assets', 'static', 'public'], 'assets'],
  [['cmd', 'bin'], 'entry'],
  [['docs', 'documentation', 'wiki'], 'documentation'],
  [['deploy', 'deployment', 'infra', 'infrastructure', 'k8s', 'kubernetes', 'helm', 'charts', 'terraform', 'tf', 'docker'], 'infrastructure'],
  [['.github', '.gitlab', '.circleci'], 'ci-cd'],
];
const EXT_PATTERNS = [
  [/\.sql$/i, 'data'],
  [/\.(md|rst)$/i, 'documentation'],
  [/\.(graphql|gql|proto)$/i, 'types'],
  [/^Dockerfile/i, 'infrastructure'],
  [/^docker-compose.*\.ya?ml$/i, 'infrastructure'],
  [/\.tf(vars)?$/i, 'infrastructure'],
  [/^Makefile$/i, 'infrastructure'],
  [/(^|\/)(pom\.xml|build\.gradle|build\.gradle\.kts|Cargo\.toml|go\.mod|Gemfile|composer\.json)$/i, 'config'],
  [/(^|\/)\.github\/workflows\//i, 'ci-cd'],
  [/(^|\/)(\.gitlab-ci\.yml|Jenkinsfile)/i, 'ci-cd'],
  [/\.d\.ts$/i, 'types'],
];
const FILE_PATTERNS = [
  [/(^|\/)(test|spec)\//i, 'test'],
  [/Test\.java$/i, 'test'],
  [/Tests\.java$/i, 'test'],
  [/IT\.java$/i, 'test'],
  [/(^|\/)Application\.java$/i, 'entry'],
  [/(^|\/)index\.(ts|js)$/i, 'entry'],
  [/(^|\/)__init__\.py$/i, 'entry'],
];

function classify(groupName, samplePaths) {
  const segs = groupName.split('/').map((s) => s.toLowerCase());
  for (const seg of segs) {
    for (const [names, label] of DIRECTORY_PATTERNS) {
      if (names.includes(seg)) return label;
    }
  }
  for (const p of samplePaths) {
    for (const [re, label] of FILE_PATTERNS) if (re.test(p)) return label;
    for (const [re, label] of EXT_PATTERNS) if (re.test(p)) return label;
  }
  return 'unknown';
}

const patternMatches = {};
const patternMatchesLevel2 = {};
for (const [g, ids] of Object.entries(directoryGroups)) {
  patternMatches[g] = classify(g, ids.slice(0, 20).map((id) => norm(byId.get(id).filePath)));
}
for (const [g, ids] of Object.entries(directoryGroupsLevel2)) {
  patternMatchesLevel2[g] = classify(g, ids.slice(0, 20).map((id) => norm(byId.get(id).filePath)));
}

// ---------- H. Deployment topology ----------
const allPaths = fileNodes.map((n) => norm(n.filePath));
const infraFiles = allPaths.filter((p) => /(^|\/)Dockerfile|docker-compose|\.tf$|\.tfvars$|Makefile|(^|\/)(k8s|kubernetes|helm|charts)\//i.test(p));
const hasDockerfile = allPaths.some((p) => /(^|\/)Dockerfile/i.test(p));
const hasCompose = allPaths.some((p) => /docker-compose.*\.ya?ml$/i.test(p));
const hasK8s = allPaths.some((p) => /(^|\/)(k8s|kubernetes)\//i.test(p));
const hasTerraform = allPaths.some((p) => /\.tf(vars)?$/i.test(p));
const hasCI = allPaths.some((p) => /\.github\/workflows\/|\.gitlab-ci\.yml$|Jenkinsfile$/i.test(p));

// ---------- I. Data pipeline ----------
const schemaFiles = allPaths.filter((p) => /schema|\.graphql$|\.gql$|\.proto$|\.prisma$/i.test(p));
const migrationFiles = allPaths.filter((p) => /migration/i.test(p) || (/\.sql$/i.test(p) && /migrat/i.test(p)));
const sqlFiles = allPaths.filter((p) => /\.sql$/i.test(p));
const dataModelFiles = fileNodes.filter((n) => n.type === 'table').map((n) => n.id);
const apiHandlerFiles = fileNodes
  .filter((n) => (n.tags || []).some((t) => /api-handler|控制器|端点|controller/i.test(t)) || /Controller\.java$/.test(norm(n.filePath)))
  .map((n) => n.id);

// ---------- J. Documentation coverage ----------
const groupsWithDocs = [];
const undocumentedGroups = [];
for (const [g, ids] of Object.entries(directoryGroups)) {
  const hasDoc = ids.some((id) => byId.get(id).type === 'document' || /\.(md|rst)$/i.test(norm(byId.get(id).filePath)));
  if (hasDoc) groupsWithDocs.push(g);
  else undocumentedGroups.push(g);
}

// ---------- K. Dependency direction ----------
const dependencyDirection = [];
const seenPair = new Set();
for (const key of Object.keys(interGroupImports)) {
  const [a, b] = key.split('\u0000');
  const pairKey = [a, b].sort().join('\u0000');
  if (seenPair.has(pairKey)) continue;
  seenPair.add(pairKey);
  const ab = interGroupImports[a + '\u0000' + b] || 0;
  const ba = interGroupImports[b + '\u0000' + a] || 0;
  if (ab === 0 && ba === 0) continue;
  if (ab >= ba) dependencyDirection.push({ dependent: a, dependsOn: b, forward: ab, reverse: ba });
  else dependencyDirection.push({ dependent: b, dependsOn: a, forward: ba, reverse: ab });
}

// ---------- Output ----------
const out = {
  scriptCompleted: true,
  prefixSegs,
  directoryGroups,
  directoryGroupsLevel2,
  nodeTypeGroups,
  crossCategoryEdges: Object.entries(crossCategoryMap)
    .map(([k, count]) => {
      const [fromType, toType, edgeType] = k.split('\u0000');
      return { fromType, toType, edgeType, count };
    })
    .sort((a, b) => b.count - a.count),
  interGroupImports: Object.entries(interGroupImports)
    .map(([k, count]) => {
      const [from, to] = k.split('\u0000');
      return { from, to, count };
    })
    .sort((a, b) => b.count - a.count),
  intraGroupDensity: Object.fromEntries(
    Object.keys(directoryGroups).map((g) => {
      const internalEdges = intraCount[g] || 0;
      const totalEdges = groupTotalEdges[g] || 0;
      return [g, { internalEdges, totalEdges, density: totalEdges ? +(internalEdges / totalEdges).toFixed(3) : 0 }];
    })
  ),
  patternMatches,
  patternMatchesLevel2,
  deploymentTopology: { hasDockerfile, hasCompose, hasK8s, hasTerraform, hasCI, infraFiles },
  dataPipeline: {
    schemaFiles,
    migrationFiles,
    sqlFiles,
    dataModelFiles,
    sqlTableNodeCount: dataModelFiles.length,
    apiHandlerFiles: apiHandlerFiles.slice(0, 200),
    apiHandlerFileCount: apiHandlerFiles.length,
  },
  docCoverage: {
    groupsWithDocs: groupsWithDocs.length,
    totalGroups: Object.keys(directoryGroups).length,
    coverageRatio: Object.keys(directoryGroups).length
      ? +(groupsWithDocs.length / Object.keys(directoryGroups).length).toFixed(2)
      : 0,
    undocumentedGroups,
  },
  dependencyDirection,
  fileStats: {
    totalFileNodes: fileNodes.length,
    filesPerGroup: Object.fromEntries(Object.entries(directoryGroups).map(([g, ids]) => [g, ids.length])),
    filesPerGroupLevel2: Object.fromEntries(Object.entries(directoryGroupsLevel2).map(([g, ids]) => [g, ids.length])),
    nodeTypeCounts: Object.fromEntries(Object.entries(nodeTypeGroups).map(([t, ids]) => [t, ids.length])),
  },
  fileFanIn: Object.fromEntries(Object.entries(fanIn).sort((a, b) => b[1] - a[1]).slice(0, 40)),
  fileFanOut: Object.fromEntries(Object.entries(fanOut).sort((a, b) => b[1] - a[1]).slice(0, 40)),
};

try {
  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, JSON.stringify(out, null, 1));
} catch (e) {
  fail('failed to write output: ' + e.message);
}
console.log('scriptCompleted totalFileNodes=' + fileNodes.length);
process.exit(0);
