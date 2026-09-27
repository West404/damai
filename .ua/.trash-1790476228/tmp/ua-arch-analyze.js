// Phase 1: structural analysis for architecture-analyzer
const fs = require('fs');

const inputPath = process.argv[2];
const outputPath = process.argv[3];

let graph;
try {
  graph = JSON.parse(fs.readFileSync(inputPath, 'utf8'));
} catch (e) {
  console.error('Failed to read/parse graph: ' + e.message);
  process.exit(1);
}

const FILE_TYPES = ['file', 'config', 'document', 'service', 'pipeline', 'table', 'schema', 'resource', 'endpoint'];
const fileTypeSet = {};
FILE_TYPES.forEach(t => fileTypeSet[t] = true);

const allNodes = graph.nodes || [];
const allEdges = graph.edges || [];

const fileNodes = allNodes.filter(n => fileTypeSet[n.type]);
const fileIdSet = {};
fileNodes.forEach(n => fileIdSet[n.id] = n);

// file-level edges only (both endpoints are file-level nodes)
const fileEdges = allEdges.filter(e => fileIdSet[e.source] && fileIdSet[e.target]);
const importEdges = fileEdges.filter(e => e.type === 'imports');

// A. Directory grouping: top-level directory (first segment) since no common prefix
function groupOf(p) {
  const idx = p.indexOf('/');
  return idx === -1 ? '(root)' : p.substring(0, idx);
}
const directoryGroups = {};
fileNodes.forEach(n => {
  const g = groupOf(n.filePath || n.id);
  if (!directoryGroups[g]) directoryGroups[g] = [];
  directoryGroups[g].push(n.id);
});
const groupById = {};
Object.keys(directoryGroups).forEach(g => directoryGroups[g].forEach(id => groupById[id] = g));

// B. Node type grouping
const nodeTypeGroups = {};
fileNodes.forEach(n => {
  if (!nodeTypeGroups[n.type]) nodeTypeGroups[n.type] = [];
  nodeTypeGroups[n.type].push(n.id);
});

// C. fan-in / fan-out
const fanOut = {}, fanIn = {};
importEdges.forEach(e => {
  fanOut[e.source] = (fanOut[e.source] || 0) + 1;
  fanIn[e.target] = (fanIn[e.target] || 0) + 1;
});

// D. cross-category edges
const crossCat = {};
fileEdges.forEach(e => {
  const st = fileIdSet[e.source].type, tt = fileIdSet[e.target].type;
  if (st !== tt || e.type !== 'imports') {
    const key = st + '|' + tt + '|' + e.type;
    crossCat[key] = (crossCat[key] || 0) + 1;
  }
});
const crossCategoryEdges = Object.keys(crossCat).map(k => {
  const parts = k.split('|');
  return { fromType: parts[0], toType: parts[1], edgeType: parts[2], count: crossCat[k] };
}).sort((a, b) => b.count - a.count);

// E. inter-group imports
const interGroup = {};
importEdges.forEach(e => {
  const gs = groupById[e.source], gt = groupById[e.target];
  if (gs && gt && gs !== gt) {
    const key = gs + '|' + gt;
    interGroup[key] = (interGroup[key] || 0) + 1;
  }
});
const interGroupImports = Object.keys(interGroup).map(k => {
  const parts = k.split('|');
  return { from: parts[0], to: parts[1], count: interGroup[k] };
}).sort((a, b) => b.count - a.count);

// F. intra-group density
const intraGroupDensity = {};
Object.keys(directoryGroups).forEach(g => { intraGroupDensity[g] = { internalEdges: 0, totalEdges: 0, density: 0 }; });
importEdges.forEach(e => {
  const gs = groupById[e.source], gt = groupById[e.target];
  if (!gs || !gt) return;
  intraGroupDensity[gs].totalEdges++;
  if (gs !== gt) intraGroupDensity[gt].totalEdges++;
  else intraGroupDensity[gs].internalEdges++;
});
Object.keys(intraGroupDensity).forEach(g => {
  const d = intraGroupDensity[g];
  d.density = d.totalEdges === 0 ? 0 : Math.round((d.internalEdges / d.totalEdges) * 100) / 100;
});

// G. pattern matching on group names
const DIR_PATTERNS = [
  [/routes|controllers|endpoints|handlers|^api$/, 'api'],
  [/services|service|^core$|^lib$|domain|logic|server/, 'service'],
  [/models|^db$|persistence|repository|entities|migrations/, 'data'],
  [/components|views|pages|^ui$|layouts|screens/, 'ui'],
  [/middleware|plugins|interceptors|guards/, 'middleware'],
  [/utils|helpers|common|shared|tools/, 'utility'],
  [/^config$|constants|^env$|settings/, 'config'],
  [/__tests__|^tests?$|spec/, 'test'],
  [/types|interfaces|schemas|contracts|dtos/, 'types'],
  [/^vue3$|^src$|public/, 'ui'],
  [/docs|documentation|wiki/, 'documentation'],
  [/deploy|infra|docker/, 'infrastructure'],
  [/\.github|\.gitlab|\.circleci/, 'ci-cd'],
  [/sql|database|schema/, 'data'],
  [/client/, 'types'],
  [/framework/, 'service']
];
const patternMatches = {};
Object.keys(directoryGroups).forEach(g => {
  const low = g.toLowerCase();
  for (const [re, label] of DIR_PATTERNS) {
    if (re.test(low)) { patternMatches[g] = label; break; }
  }
  if (!patternMatches[g]) patternMatches[g] = 'unknown';
});

// H. deployment topology
const names = fileNodes.map(n => (n.filePath || '').toLowerCase());
const infraFiles = fileNodes.filter(n => {
  const p = (n.filePath || '').toLowerCase();
  return p.indexOf('dockerfile') !== -1 || p.indexOf('docker-compose') !== -1 || p.indexOf('.github/workflows') !== -1 || p.indexOf('jenkinsfile') !== -1 || p.indexOf('.gitlab-ci') !== -1 || /\.tf$/.test(p) || p.indexOf('k8s') !== -1;
}).map(n => n.filePath);
const deploymentTopology = {
  hasDockerfile: names.some(p => p.indexOf('dockerfile') !== -1),
  hasCompose: names.some(p => p.indexOf('docker-compose') !== -1),
  hasK8s: names.some(p => p.indexOf('k8s') !== -1 || p.indexOf('kubernetes') !== -1),
  hasTerraform: names.some(p => /\.tf$/.test(p)),
  hasCI: names.some(p => p.indexOf('.github/workflows') !== -1 || p.indexOf('.gitlab-ci') !== -1),
  infraFiles: infraFiles
};

// I. data pipeline
const dataPipeline = {
  schemaFiles: fileNodes.filter(n => n.type === 'schema' || /\.sql$/.test(n.filePath || '')).map(n => n.filePath),
  migrationFiles: fileNodes.filter(n => /migrations?\//.test(n.filePath || '')).map(n => n.filePath),
  dataModelFiles: fileNodes.filter(n => n.type === 'table').map(n => n.filePath),
  apiHandlerFiles: fileNodes.filter(n => n.type === 'endpoint').map(n => n.filePath)
};

// J. doc coverage
const docGroups = new Set();
fileNodes.filter(n => n.type === 'document' || /\.md$/.test(n.filePath || '')).forEach(n => docGroups.add(groupOf(n.filePath)));
const totalGroups = Object.keys(directoryGroups).length;
const docCoverage = {
  groupsWithDocs: docGroups.size,
  totalGroups: totalGroups,
  coverageRatio: Math.round((docGroups.size / totalGroups) * 100) / 100,
  undocumentedGroups: Object.keys(directoryGroups).filter(g => !docGroups.has(g))
};

// K. dependency direction
const dependencyDirection = [];
const pairCounts = {};
interGroupImports.forEach(r => { pairCounts[r.from + '|' + r.to] = r.count; });
const seen = {};
interGroupImports.forEach(r => {
  const rev = pairCounts[r.to + '|' + r.from] || 0;
  const key = [r.from, r.to].sort().join('|');
  if (seen[key]) return;
  seen[key] = true;
  if (r.count >= rev) dependencyDirection.push({ dependent: r.from, dependsOn: r.to, count: r.count, reverse: rev });
  else dependencyDirection.push({ dependent: r.to, dependsOn: r.from, count: rev, reverse: r.count });
});

const filesPerGroup = {};
Object.keys(directoryGroups).forEach(g => filesPerGroup[g] = directoryGroups[g].length);
const nodeTypeCounts = {};
Object.keys(nodeTypeGroups).forEach(t => nodeTypeCounts[t] = nodeTypeGroups[t].length);

// also emit per-node info for phase 2 (path, type, summary, tags, group)
const nodeInfo = fileNodes.map(n => ({
  id: n.id, type: n.type, name: n.name, filePath: n.filePath,
  group: groupOf(n.filePath || n.id),
  summary: (n.summary || '').substring(0, 200),
  tags: n.tags || []
}));

const results = {
  scriptCompleted: true,
  directoryGroups: directoryGroups,
  nodeTypeGroups: Object.keys(nodeTypeGroups).reduce((acc, t) => { acc[t] = nodeTypeGroups[t].length; return acc; }, {}),
  crossCategoryEdges: crossCategoryEdges.slice(0, 60),
  interGroupImports: interGroupImports.slice(0, 80),
  intraGroupDensity: intraGroupDensity,
  patternMatches: patternMatches,
  deploymentTopology: deploymentTopology,
  dataPipeline: {
    schemaFiles: dataPipeline.schemaFiles.slice(0, 50),
    migrationFiles: dataPipeline.migrationFiles.slice(0, 20),
    dataModelFileCount: dataPipeline.dataModelFiles.length,
    apiHandlerFileCount: dataPipeline.apiHandlerFiles.length
  },
  docCoverage: docCoverage,
  dependencyDirection: dependencyDirection.slice(0, 60),
  fileStats: { totalFileNodes: fileNodes.length, filesPerGroup: filesPerGroup, nodeTypeCounts: nodeTypeCounts },
  fileFanIn: Object.keys(fanIn).sort((a, b) => fanIn[b] - fanIn[a]).slice(0, 30).reduce((acc, k) => { acc[k] = fanIn[k]; return acc; }, {}),
  fileFanOut: Object.keys(fanOut).sort((a, b) => fanOut[b] - fanOut[a]).slice(0, 30).reduce((acc, k) => { acc[k] = fanOut[k]; return acc; }, {}),
  nodeInfo: nodeInfo
};

fs.writeFileSync(outputPath, JSON.stringify(results, null, 1));
console.log('OK totalFileNodes=' + fileNodes.length + ' groups=' + totalGroups + ' edges=' + fileEdges.length);
