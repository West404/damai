#!/usr/bin/env node
'use strict';

const fs = require('fs');

function main() {
  const inPath = process.argv[2];
  const outPath = process.argv[3];
  if (!inPath || !outPath) {
    process.stderr.write('usage: ua-tour-analyze.js <input.json> <output.json>\n');
    process.exit(1);
  }

  const raw = JSON.parse(fs.readFileSync(inPath, 'utf8'));
  const nodes = Array.isArray(raw.nodes) ? raw.nodes : [];
  const edges = Array.isArray(raw.edges) ? raw.edges : [];
  const layers = Array.isArray(raw.layers) ? raw.layers : [];

  const nodeById = new Map();
  for (const n of nodes) {
    nodeById.set(n.id, {
      id: n.id,
      name: n.name || n.id,
      type: n.type || 'file',
      filePath: n.filePath || '',
      summary: n.summary || ''
    });
  }

  // Only count edges whose endpoints are both present in the node set.
  const known = edges.filter((e) => nodeById.has(e.source) && nodeById.has(e.target));

  const fanIn = new Map();
  const fanOut = new Map();
  const outAdj = new Map();
  const inAdj = new Map();
  const undirectedNeighbors = new Map();

  for (const id of nodeById.keys()) {
    fanIn.set(id, 0);
    fanOut.set(id, 0);
    outAdj.set(id, new Set());
    inAdj.set(id, new Set());
    undirectedNeighbors.set(id, new Set());
  }

  for (const e of known) {
    fanIn.set(e.target, fanIn.get(e.target) + 1);
    fanOut.set(e.source, fanOut.get(e.source) + 1);
    outAdj.get(e.source).add(e.target);
    inAdj.get(e.target).add(e.source);
    undirectedNeighbors.get(e.source).add(e.target);
    undirectedNeighbors.get(e.target).add(e.source);
  }

  const desc = (map) =>
    [...map.entries()]
      .map(([id, v]) => ({ id, value: v }))
      .sort((a, b) => b.value - a.value || a.id.localeCompare(b.id));

  const fanInSorted = desc(fanIn);
  const fanOutSorted = desc(fanOut);

  const fanInRanking = fanInSorted.slice(0, 20).map((r) => ({
    id: r.id,
    fanIn: r.value,
    name: nodeById.get(r.id).name,
    type: nodeById.get(r.id).type,
    filePath: nodeById.get(r.id).filePath,
    summary: nodeById.get(r.id).summary
  }));

  const fanOutRanking = fanOutSorted.slice(0, 20).map((r) => ({
    id: r.id,
    fanOut: r.value,
    name: nodeById.get(r.id).name,
    type: nodeById.get(r.id).type,
    filePath: nodeById.get(r.id).filePath,
    summary: nodeById.get(r.id).summary
  }));

  // ---------- C. Entry point candidates ----------
  const ENTRY_NAMES = [
    'index.ts', 'index.js', 'main.ts', 'main.js', 'app.ts', 'app.js',
    'server.ts', 'server.js', 'mod.rs', 'main.go', 'main.py', 'main.rs',
    'manage.py', 'app.py', 'wsgi.py', 'asgi.py', 'run.py', '__main__.py',
    'Application.java', 'Main.java', 'Program.cs', 'config.ru', 'index.php',
    'App.swift', 'Application.kt', 'main.cpp', 'main.c'
  ];

  const nonZeroFanOut = fanOutSorted.filter((r) => r.value > 0);
  const top10pctThreshold = nonZeroFanOut.length
    ? nonZeroFanOut[Math.max(0, Math.ceil(nonZeroFanOut.length * 0.1) - 1)].value
    : Infinity;
  const nonZeroFanIn = fanInSorted.filter((r) => r.value > 0);
  const bottom25Threshold = nonZeroFanIn.length
    ? nonZeroFanIn[Math.min(nonZeroFanIn.length - 1, Math.floor(nonZeroFanIn.length * 0.75))].value
    : 0;

  const entryCandidates = [];
  for (const n of nodes) {
    let score = 0;
    const base = (n.name || '').split('/').pop();
    const depth = (n.filePath || '').split('/').filter(Boolean).length;
    if (n.type === 'document') {
      if (/^README\.md$/i.test(n.name || '')) score += 5;
      else if (/\.md$/i.test(n.name || '') && depth <= 1) score += 2;
    } else {
      // Framework-agnostic: exact names plus common framework suffixes
      // (e.g. Spring Boot `GatewayApplication.java`, `OrderApplication.java`,
      //  C# `Program.cs`, Python `settings/asgi.py`).
      if (
        ENTRY_NAMES.includes(base) ||
        /(Application|Bootstrap|Startup|Launcher|Boot)\.(java|kt)$/i.test(base) ||
        /^(Program|Startup)\.cs$/i.test(base)
      ) {
        score += 3;
      }
      if (depth <= 2) score += 1;
      if (fanOut.get(n.id) >= top10pctThreshold && fanOut.get(n.id) > 0) score += 1;
      if (fanIn.get(n.id) <= bottom25Threshold) score += 1;
    }
    if (score > 0) {
      entryCandidates.push({
        id: n.id,
        score,
        name: n.name,
        type: n.type,
        filePath: n.filePath,
        summary: n.summary
      });
    }
  }

  // ---------- D. BFS from entry point candidates ----------
  const BFSEdgeTypes = new Set(['imports', 'calls']);
  const bfsAdj = new Map();
  for (const id of nodeById.keys()) bfsAdj.set(id, []);
  for (const e of known) {
    if (BFSEdgeTypes.has(e.type)) bfsAdj.get(e.source).push(e.target);
  }

  const runBfs = (start) => {
    const depthMap = { [start]: 0 };
    const queue = [start];
    const order = [];
    while (queue.length) {
      const cur = queue.shift();
      order.push(cur);
      const d = depthMap[cur];
      for (const nxt of bfsAdj.get(cur) || []) {
        if (!(nxt in depthMap)) {
          depthMap[nxt] = d + 1;
          queue.push(nxt);
        }
      }
    }
    const byDepth = {};
    for (const id of order) {
      const d = String(depthMap[id]);
      if (!byDepth[d]) byDepth[d] = [];
      byDepth[d].push(id);
    }
    return { startNode: start, order, depthMap, byDepth, reach: order.length };
  };

  // Annotate each candidate with its own BFS reach so tie-breaking favours the
  // entry point that actually opens up the dependency graph.
  for (const c of entryCandidates) {
    if (c.type === 'document') {
      c.bfsReach = 0;
    } else {
      c.bfsReach = runBfs(c.id).reach;
    }
  }
  entryCandidates.sort(
    (a, b) =>
      b.score - a.score ||
      b.bfsReach - a.bfsReach ||
      a.filePath.localeCompare(b.filePath)
  );
  const topEntryCandidates = entryCandidates.slice(0, 25);

  const codeCandidates = topEntryCandidates.filter((c) => c.type !== 'document');
  const topCode = codeCandidates[0];
  const bfsTraversal = topCode
    ? runBfs(topCode.id)
    : { startNode: null, order: [], depthMap: {}, byDepth: {}, reach: 0 };
  const alternativeEntryTraversals = codeCandidates.slice(1, 9).map((c) => {
    const t = runBfs(c.id);
    return {
      id: c.id,
      filePath: c.filePath,
      reach: t.reach,
      byDepth: Object.fromEntries(
        Object.entries(t.byDepth).map(([d, ids]) => [d, ids.length])
      )
    };
  });

  // Widest traversal anywhere in the graph: the node that opens up the largest
  // portion of the dependency graph. Useful when bootstrap files are thin.
  let widest = null;
  for (const id of nodeById.keys()) {
    if (!(bfsAdj.get(id) || []).length) continue;
    const t = runBfs(id);
    if (!widest || t.reach > widest.reach) widest = t;
  }

  // ---------- E. Non-code inventory ----------
  const nonCodeFiles = { documentation: [], infrastructure: [], data: [], config: [] };
  for (const n of nodes) {
    const rec = { id: n.id, name: n.name, type: n.type, filePath: n.filePath, summary: n.summary };
    if (n.type === 'document') nonCodeFiles.documentation.push(rec);
    else if (n.type === 'service' || n.type === 'pipeline' || n.type === 'resource')
      nonCodeFiles.infrastructure.push(rec);
    else if (n.type === 'table' || n.type === 'schema' || n.type === 'endpoint')
      nonCodeFiles.data.push(rec);
    else if (n.type === 'config') nonCodeFiles.config.push(rec);
  }

  // ---------- F. Tightly coupled clusters ----------
  const pairKey = (a, b) => (a < b ? a + '\u0000' + b : b + '\u0000' + a);
  const directed = new Map(); // pairKey -> Set of "a>b" markers
  for (const e of known) {
    const k = pairKey(e.source, e.target);
    if (!directed.has(k)) directed.set(k, new Set());
    directed.get(k).add(e.source + '>' + e.target);
  }

  const seedPairs = [];
  for (const [k, marks] of directed.entries()) {
    const [a, b] = k.split('\u0000');
    if (marks.has(a + '>' + b) && marks.has(b + '>' + a)) seedPairs.push([a, b]);
  }

  const clusters = [];
  const usedPairs = new Set();
  for (const [a, b] of seedPairs) {
    const members = new Set([a, b]);
    // expand: nodes connected to 2+ existing members
    let changed = true;
    while (changed && members.size < 5) {
      changed = false;
      const scores = new Map();
      for (const m of members) {
        for (const nb of undirectedNeighbors.get(m) || []) {
          if (members.has(nb)) continue;
          scores.set(nb, (scores.get(nb) || 0) + 1);
        }
      }
      let best = null;
      for (const [nb, s] of scores.entries()) {
        if (s >= 2 && (best === null || s > scores.get(best))) best = nb;
      }
      if (best) {
        members.add(best);
        changed = true;
      }
    }
    const arr = [...members];
    const key = arr.slice().sort().join('|');
    if (usedPairs.has(key)) continue;
    usedPairs.add(key);
    let edgeCount = 0;
    for (let i = 0; i < arr.length; i++) {
      for (let j = i + 1; j < arr.length; j++) {
        const k = pairKey(arr[i], arr[j]);
        const marks = directed.get(k);
        if (marks) edgeCount += marks.size;
      }
    }
    clusters.push({ nodes: arr, edgeCount });
  }
  clusters.sort((x, y) => y.edgeCount - x.edgeCount || y.nodes.length - x.nodes.length);
  const topClusters = clusters.slice(0, 12).map((c) => ({
    nodes: c.nodes,
    nodeNames: c.nodes.map((id) => nodeById.get(id).name),
    edgeCount: c.edgeCount
  }));

  // ---------- G. Layers ----------
  const layerInfo = { count: layers.length, list: layers };

  // ---------- H. Node summary index ----------
  const nodeSummaryIndex = {};
  for (const n of nodes) {
    nodeSummaryIndex[n.id] = { name: n.name, type: n.type, filePath: n.filePath, summary: n.summary };
  }

  const out = {
    scriptCompleted: true,
    entryPointCandidates: topEntryCandidates,
    fanInRanking,
    fanOutRanking,
    bfsTraversal,
    nonCodeFiles,
    clusters: topClusters,
    layers: layerInfo,
    nodeSummaryIndex,
    totalNodes: nodes.length,
    totalEdges: edges.length,
    edgesConsidered: known.length,
    alternativeEntryTraversals,
    widestTraversal: widest
      ? {
          startNode: widest.startNode,
          reach: widest.reach,
          order: widest.order,
          byDepth: widest.byDepth
        }
      : null
  };

  fs.writeFileSync(outPath, JSON.stringify(out, null, 2));
  process.exit(0);
}

try {
  main();
} catch (err) {
  process.stderr.write('FATAL: ' + (err && err.stack ? err.stack : String(err)) + '\n');
  process.exit(1);
}
