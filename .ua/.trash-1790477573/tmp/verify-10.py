import json, os, sys
from collections import Counter

base = r"E:/WorkSpace/damai/.ua"
inp = json.load(open(os.path.join(base, "intermediate", "batch-input-10.json"), encoding="utf-8"))
batch_files = [f["path"] for f in inp["batchFiles"]]
import_data = inp["batchImportData"]

parts = []
for k in (1, 2, 3):
    p = os.path.join(base, "intermediate", f"batch-10-part-{k}.json")
    d = json.load(open(p, encoding="utf-8"))
    parts.append((p, d))

all_nodes, all_edges = [], []
for p, d in parts:
    print(f"{os.path.basename(p)}: nodes={len(d['nodes'])} edges={len(d['edges'])}")
    all_nodes += d["nodes"]
    all_edges += d["edges"]

print(f"TOTAL nodes={len(all_nodes)} edges={len(all_edges)}")

ids = [n["id"] for n in all_nodes]
dup = [i for i, c in Counter(ids).items() if c > 1]
print("duplicate node ids:", dup)

# every batch file has a file-level node
file_node_paths = {n["filePath"] for n in all_nodes if n["type"] == "file"}
missing = [f for f in batch_files if f not in file_node_paths]
print("batch files missing file node:", missing)

# required node fields
for n in all_nodes:
    assert n.get("id") and n.get("type") and n.get("name") and n.get("summary") and n.get("tags") and n.get("complexity"), n
    assert 3 <= len(n["tags"]) <= 5, ("tags count", n["id"], n["tags"])
    if n["type"] in ("file", "config", "document", "service", "pipeline", "schema", "resource"):
        assert n.get("filePath"), n["id"]
    if n["type"] in ("function", "class"):
        assert "lineRange" in n, n["id"]

# imports edges 1:1
expected = sum(len(v) for v in import_data.values())
got = [e for e in all_edges if e["type"] == "imports"]
print(f"imports edges expected={expected} got={len(got)}")
missing_imp = []
for f, targets in import_data.items():
    emitted = {(e["source"], e["target"]) for e in got if e["source"] == f"file:{f}"}
    for t in targets:
        if (f"file:{f}", f"file:{t}") not in emitted:
            missing_imp.append((f, t))
print("missing import edges:", missing_imp)

# edge validity
node_ids = set(ids)
neighbor_targets = set()
for f, lst in inp.get("neighborMap", {}).items():
    neighbor_targets.add(f"file:{f}")
    for nb in lst:
        neighbor_targets.add(f"file:{nb['path']}")
        for s in nb.get("symbols", []):
            neighbor_targets.add(f"function:{nb['path']}:{s}")
            neighbor_targets.add(f"class:{nb['path']}:{s}")
bad = []
for e in all_edges:
    assert e["direction"] == "forward", e
    if e["source"] == e["target"]:
        bad.append(("self", e))
    for side in ("source", "target"):
        v = e[side]
        if v not in node_ids and v not in neighbor_targets:
            bad.append((side, e))
print("edges with unresolved endpoint:", bad)
print("edge types:", Counter(e["type"] for e in all_edges))
