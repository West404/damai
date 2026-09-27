import json, math, os, sys
from collections import defaultdict

ROOT = "E:/WorkSpace/damai"
UA = os.path.join(ROOT, ".ua")
BI = 4
sem = json.load(open(f"{UA}/tmp/ua-b4-semantic.json", encoding="utf-8"))
bi = json.load(open(f"{UA}/intermediate/batch-input-{BI}.json", encoding="utf-8"))

batch_files = [f["path"] for f in bi["batchFiles"]]
imp = bi["batchImportData"]
neighbor = bi.get("neighborMap", {})

nodes = list(sem["nodes"])
edges = list(sem["edges"])

# --- append one imports edge per batchImportData entry ---
expected_imports = sum(len(v) for v in imp.values())
imports_edges = []
for f in batch_files:
    for tgt in imp.get(f, []):
        if tgt == f:
            print("WARN self import", f)
            continue
        imports_edges.append({
            "source": f"file:{f}",
            "target": f"file:{tgt}",
            "type": "imports",
            "direction": "forward",
            "weight": 0.7,
        })
edges.extend(imports_edges)
print("expected imports:", expected_imports, "emitted:", len(imports_edges))

# --- validation ---
ids = [n["id"] for n in nodes]
dups = {i for i in ids if ids.count(i) > 1}
assert not dups, f"duplicate node ids: {dups}"

for n in nodes:
    for k in ("id", "type", "name", "summary", "tags", "complexity"):
        assert n.get(k), f"node missing {k}: {n.get('id')}"
    assert isinstance(n["tags"], list) and 3 <= len(n["tags"]) <= 5, f"bad tags: {n['id']} {n['tags']}"
    assert n["complexity"] in ("simple", "moderate", "complex"), n["id"]
    assert n["id"].split(":")[0] in ("file", "function", "class", "config", "document", "service",
                                     "table", "endpoint", "pipeline", "schema", "resource"), n["id"]

# every batch file has a file-level node
file_ids = {n["id"] for n in nodes if n["type"] == "file"}
missing = [f for f in batch_files if f"file:{f}" not in file_ids]
assert not missing, f"missing file-level nodes: {missing}"

idset = set(ids)
allowed_ext = set()
for v in imp.values():
    allowed_ext.update(v)
for k, lst in neighbor.items():
    for nb in lst:
        allowed_ext.add(nb["path"])
        for s in nb.get("symbols", []):
            for pref in ("file", "class", "function"):
                idset.add(f"{pref}:{nb['path']}:{s}")

bad = []
for e in edges:
    assert e["direction"] == "forward", e
    assert e["type"] in ("contains", "imports", "calls", "inherits", "implements", "exports",
                         "depends_on", "tested_by", "configures", "documents", "deploys",
                         "migrates", "triggers", "defines_schema", "serves", "provisions",
                         "routes", "related"), e
    if e["source"] == e["target"]:
        bad.append(("self", e))
    for side in ("source", "target"):
        v = e[side]
        if v in idset:
            continue
        if v.startswith("file:") and v[5:] in allowed_ext:
            continue
        bad.append((side, e))
assert not bad, f"invalid edges: {bad[:5]}"

by_type = defaultdict(int)
for e in edges:
    by_type[e["type"]] += 1
by_ntype = defaultdict(int)
for n in nodes:
    by_ntype[n["type"]] += 1
print("nodes:", len(nodes), dict(by_ntype))
print("edges:", len(edges), dict(by_type))

# --- partition ---
nodeCount, edgeCount = len(nodes), len(edges)
if nodeCount <= 60 and edgeCount <= 120:
    parts = 1
else:
    parts = math.ceil(max(nodeCount / 60, edgeCount / 120))
sorted_files = sorted(batch_files)
chunk = math.ceil(len(sorted_files) / parts)
groups = [set(sorted_files[i:i + chunk]) for i in range(0, len(sorted_files), chunk)]
print("parts:", parts, "group sizes:", [len(g) for g in groups])

node_by_id = {n["id"]: n for n in nodes}
edge_src_in = defaultdict(set)
for e in edges:
    edge_src_in[e["source"]].add(e["source"])

written = []
for k, g in enumerate(groups, start=1):
    pnodes = [n for n in nodes if n["filePath"] in g]
    pids = {n["id"] for n in pnodes}
    pedges = [e for e in edges if e["source"] in pids]
    out = {"nodes": pnodes, "edges": pedges}
    path = f"{UA}/intermediate/batch-{BI}-part-{k}.json" if parts > 1 else f"{UA}/intermediate/batch-{BI}.json"
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(out, fh, ensure_ascii=False, indent=2)
    written.append(path)
    print(f"part {k}: nodes={len(pnodes)} edges={len(pedges)} -> {path}")

# coverage check across parts
tot_n = sum(len(json.load(open(p, encoding="utf-8"))["nodes"]) for p in written)
tot_e = sum(len(json.load(open(p, encoding="utf-8"))["edges"]) for p in written)
print("written totals:", tot_n, tot_e, "vs", nodeCount, edgeCount)
assert tot_n == nodeCount and tot_e == edgeCount
print("OK")
