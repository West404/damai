#!/usr/bin/env node
// Understand-Anything tour-builder graph topology analysis
const fs = require('fs');

const inputPath = process.argv[2];
const outputPath = process.argv[3];
if (!inputPath || !outputPath) {
  console.error('Usage: node ua-tour-analyze.js <input.json> <output.json>');
  process.exit(1);
}

let raw;
try {
  raw = JSON.parse(fs.readFileSync(inputPath, 'utf8'));
} catch (e) {
  console.error('Failed to read/parse input: ' + e.message);
  process.exit(1);
}

// Filter to file-level nodes: keep all types except function/class
const nodes = (raw.nodes || []).filter(n => n.type !== 'function' && n.type !== 'class');
const nodeIds = new Set(nodes.map(n => n.id));
const edges = (raw.edges || []).filter(e => nodeIds.has(e.source) && nodeIds.has(e.target));
const layers = raw.layers || [];

const nodeById = new Map(nodes.map(n => [n.id, n]));

// --- A & B: Fan-in / Fan-out ---
const fanIn = new Map();
const fanOut = new Map();
for (const n of nodes) { fanIn.set(n.id, 0); fanOut.set(n.id, 0); }
for (const e of edges) {
  fanOut.set(e.source, (fanOut.get(e.source) || 0) + 1);
  fanIn.set(e.target, (fanIn.get(e.target) || 0) + 1);
}

const fanInRanking = [...fanIn.entries()]
  .map(([id, count]) => ({ id, fanIn: count, name: (nodeById.get(id) || {}).name || id }))
  .sort((a, b) => b.fanIn - a.fanIn)
  .slice(0, 20);

const fanOutRanking = [...fanOut.entries()]
  .map(([id, count]) => ({ id, fanOut: count, name: (nodeById.get(id) || {}).name || id }))
  .sort((a, b) => b.fanOut - a.fanOut)
  .slice(0, 20);

// --- C: Entry point candidates ---
const ENTRY_NAMES = new Set([
  'index.ts','index.js','main.ts','main.js','app.ts','app.js','server.ts','server.js',
  'mod.rs','main.go','main.py','main.rs','manage.py','app.py','wsgi.py','asgi.py','run.py',
  '__main__.py','application.java','main.java','program.cs','config.ru','index.php',
  'app.swift','application.kt','main.cpp','main.c'
]);

const sortedFanOutDesc = [...fanOut.entries()].sort((a, b) => b[1] - a[1]);
const top10FanOutCutoff = sortedFanOutDesc[Math.min(Math.floor(nodes.length * 0.1), sortedFanOutDesc.length - 1)]?.[1] ?? Infinity;
const sortedFanInAsc = [...fanIn.entries()].sort((a, b) => a[1] - b[1]);
const bottom25FanInCutoff = sortedFanInAsc[Math.min(Math.floor(nodes.length * 0.25), sortedFanInAsc.length - 1)]?.[1] ?? -Infinity;

const entryCandidates = [];
for (const n of nodes) {
  const baseName = (n.name || '').toLowerCase();
  const fp = (n.filePath || '').replace(/\\/g, '/');
  const depth = fp.split('/').filter(Boolean).length;
  let score = 0;
  if (n.type === 'document') {
    if (baseName === 'readme.md' && depth <= 1) score += 5;
    else if (baseName.endsWith('.md') && depth <= 1) score += 2;
  } else {
    if (ENTRY_NAMES.has(baseName) || baseName.endsWith('application.java')) score += 3;
    if (depth <= 2) score += 1;
    if ((fanOut.get(n.id) || 0) >= top10FanOutCutoff) score += 1;
    if ((fanIn.get(n.id) || 0) <= bottom25FanInCutoff) score += 1;
  }
  if (score > 0) entryCandidates.push({ id: n.id, score, name: n.name, summary: n.summary || '' });
}
entryCandidates.sort((a, b) => b.score - a.score);
const topEntryCandidates = entryCandidates.slice(0, 5);

// --- D: BFS from top code entry point (skip documents) ---
const adjacency = new Map();
for (const e of edges) {
  if (e.type === 'imports' || e.type === 'calls') {
    if (!adjacency.has(e.source)) adjacency.set(e.source, []);
    adjacency.get(e.source).push(e.target);
  }
}

const topCodeEntry = entryCandidates.find(c => (nodeById.get(c.id) || {}).type !== 'document');
const bfsTraversal = { startNode: null, order: [], depthMap: {}, byDepth: {} };
if (topCodeEntry) {
  bfsTraversal.startNode = topCodeEntry.id;
  const visited = new Set([topCodeEntry.id]);
  const queue = [[topCodeEntry.id, 0]];
  bfsTraversal.depthMap[topCodeEntry.id] = 0;
  bfsTraversal.byDepth['0'] = [topCodeEntry.id];
  while (queue.length > 0) {
    const [cur, depth] = queue.shift();
    bfsTraversal.order.push(cur);
    for (const next of (adjacency.get(cur) || [])) {
      if (!visited.has(next)) {
        visited.add(next);
        const nd = depth + 1;
        bfsTraversal.depthMap[next] = nd;
        if (!bfsTraversal.byDepth[String(nd)]) bfsTraversal.byDepth[String(nd)] = [];
        bfsTraversal.byDepth[String(nd)].push(next);
        queue.push([next, nd]);
      }
    }
  }
}

// --- E: Non-code file inventory ---
const nonCodeFiles = { documentation: [], infrastructure: [], data: [], config: [] };
for (const n of nodes) {
  const rec = { id: n.id, name: n.name, type: n.type, summary: n.summary || '' };
  if (n.type === 'document') nonCodeFiles.documentation.push(rec);
  else if (['service', 'pipeline', 'resource'].includes(n.type)) nonCodeFiles.infrastructure.push(rec);
  else if (['table', 'schema', 'endpoint'].includes(n.type)) nonCodeFiles.data.push(rec);
  else if (n.type === 'config') nonCodeFiles.config.push(rec);
}

// --- F: Tightly coupled clusters ---
const undirected = new Map(); // id -> Set of neighbors
const pairCount = new Map(); // "a|b" -> count
for (const e of edges) {
  const [a, b] = [e.source, e.target].sort();
  const key = a + '|' + b;
  pairCount.set(key, (pairCount.get(key) || 0) + 1);
  if (!undirected.has(e.source)) undirected.set(e.source, new Set());
  if (!undirected.has(e.target)) undirected.set(e.target, new Set());
  undirected.get(e.source).add(e.target);
  undirected.get(e.target).add(e.source);
}

// bidirectional pairs
const bidir = [];
const edgeSet = new Set(edges.map(e => e.source + '->' + e.target));
const seenPairs = new Set();
for (const e of edges) {
  const rev = e.target + '->' + e.source;
  const key = [e.source, e.target].sort().join('|');
  if (edgeSet.has(rev) && !seenPairs.has(key)) {
    seenPairs.add(key);
    bidir.push([e.source, e.target]);
  }
}

const clusters = [];
const inCluster = new Set();
for (const [a, b] of bidir) {
  if (inCluster.has(a) || inCluster.has(b)) continue;
  const cluster = new Set([a, b]);
  let grew = true;
  while (grew && cluster.size < 5) {
    grew = false;
    for (const n of nodes) {
      if (cluster.has(n.id) || inCluster.has(n.id)) continue;
      let conn = 0;
      for (const m of cluster) {
        const k1 = [n.id, m].sort().join('|');
        if (pairCount.has(k1)) conn++;
      }
      if (conn >= 2 && cluster.size < 5) { cluster.add(n.id); grew = true; }
    }
  }
  let edgeCount = 0;
  for (const m of cluster) for (const k of cluster) {
    if (m !== k) {
      const key = [m, k].sort().join('|');
      edgeCount += pairCount.get(key) || 0;
    }
  }
  edgeCount = edgeCount / 2;
  clusters.push({ nodes: [...cluster], edgeCount });
  for (const m of cluster) inCluster.add(m);
}
clusters.sort((a, b) => b.edgeCount - a.edgeCount);
const topClusters = clusters.slice(0, 10);

// --- G: Layers ---
const layersOut = { count: layers.length, list: layers.map(l => ({ id: l.id, name: l.name, description: l.description })) };

// --- H: Node summary index ---
const nodeSummaryIndex = {};
for (const n of nodes) {
  nodeSummaryIndex[n.id] = { name: n.name, type: n.type, summary: n.summary || '' };
}

const results = {
  scriptCompleted: true,
  entryPointCandidates: topEntryCandidates,
  fanInRanking,
  fanOutRanking,
  bfsTraversal,
  nonCodeFiles,
  clusters: topClusters,
  layers: layersOut,
  nodeSummaryIndex,
  totalNodes: nodes.length,
  totalEdges: edges.length
};

try {
  fs.writeFileSync(outputPath, JSON.stringify(results, null, 2));
} catch (e) {
  console.error('Failed to write output: ' + e.message);
  process.exit(1);
}
console.log('OK nodes=' + nodes.length + ' edges=' + edges.length + ' clusters=' + topClusters.length);
process.exit(0);
