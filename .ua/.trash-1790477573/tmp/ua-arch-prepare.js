const fs = require('fs');
const path = require('path');

const graphPath = process.argv[2];
const outPath = process.argv[3];

const g = JSON.parse(fs.readFileSync(graphPath, 'utf8'));
const FILE_LEVEL = new Set(['file', 'config', 'document', 'service', 'pipeline', 'table', 'schema', 'resource', 'endpoint']);

const fileNodes = g.nodes.filter((n) => FILE_LEVEL.has(n.type));
const fileNodeIds = new Set(fileNodes.map((n) => n.id));

const importEdges = g.edges.filter((e) => e.type === 'imports' && fileNodeIds.has(e.source) && fileNodeIds.has(e.target));
const allEdges = g.edges.filter((e) => e.type !== 'imports' && fileNodeIds.has(e.source) && fileNodeIds.has(e.target));

const payload = {
  fileNodes: fileNodes.map((n) => ({
    id: n.id,
    type: n.type,
    name: n.name,
    filePath: n.filePath || '',
    summary: n.summary || '',
    tags: n.tags || [],
  })),
  importEdges: importEdges.map((e) => ({ source: e.source, target: e.target, type: e.type })),
  allEdges: allEdges.map((e) => ({ source: e.source, target: e.target, type: e.type })),
};

fs.writeFileSync(outPath, JSON.stringify(payload, null, 1));
console.log('fileNodes', payload.fileNodes.length, 'importEdges', payload.importEdges.length, 'allEdges', payload.allEdges.length);
