import json, os, sys

base = "E:/WorkSpace/damai/.ua/intermediate"
inp = json.load(open("E:/WorkSpace/damai/.ua/tmp/ua-file-analyzer-input-12.json", encoding="utf-8"))
batch_files = [f["path"] for f in inp["batchFiles"]]
imp = inp["batchImportData"]
expected_imports = sum(len(v) for v in imp.values())

parts = ["batch-12-part-1.json", "batch-12-part-2.json", "batch-12-part-3.json"]
nodes, edges, ids = [], [], []
for p in parts:
    path = os.path.join(base, p)
    d = json.load(open(path, encoding="utf-8"))
    print(p, "nodes=", len(d["nodes"]), "edges=", len(d["edges"]))
    nodes += d["nodes"]
    edges += d["edges"]
    ids += [n["id"] for n in d["nodes"]]

print("TOTAL nodes=", len(nodes), "edges=", len(edges))
dup = set(i for i in ids if ids.count(i) > 1)
print("duplicate ids:", dup)

# every batch file has a file-level node
missing = [f for f in batch_files if f"file:{f}" not in ids]
print("batchFiles without file node:", missing)

# imports edge count per file
imp_edges = {}
for e in edges:
    if e["type"] == "imports":
        src = e["source"]
        imp_edges[src] = imp_edges.get(src, 0) + 1
bad = []
for f, targets in imp.items():
    got = imp_edges.get(f"file:{f}", 0)
    if got != len(targets):
        bad.append((f, len(targets), got))
print("imports edges total:", sum(imp_edges.values()), "expected:", expected_imports)
print("per-file import mismatches:", bad)

# edge field validation
valid_types = {"contains","imports","calls","inherits","implements","exports","depends_on","tested_by",
               "configures","documents","deploys","migrates","triggers","defines_schema","serves",
               "provisions","routes","related"}
weights = {"contains":1.0,"imports":0.7,"calls":0.8,"inherits":0.9,"implements":0.9,"exports":0.8,
           "depends_on":0.6,"tested_by":0.5,"configures":0.6,"documents":0.5,"deploys":0.7,
           "migrates":0.7,"triggers":0.6,"defines_schema":0.8,"serves":0.7,"provisions":0.7,
           "routes":0.6,"related":0.5}
id_set = set(ids)
errors = []
neighbor_refs = set()
nm = json.load(open("E:/WorkSpace/damai/.ua/intermediate/batch-input-12.json", encoding="utf-8"))["neighborMap"]
for f, ns in nm.items():
    for n in ns:
        neighbor_refs.add("file:" + n["path"])
        for s in n["symbols"]:
            neighbor_refs.add("function:" + n["path"] + ":" + s)
            neighbor_refs.add("class:" + n["path"] + ":" + s)
for f, targets in imp.items():
    for t in targets:
        neighbor_refs.add("file:" + t)

self_edges = 0
for e in edges:
    if e["type"] not in valid_types: errors.append(("bad type", e))
    if e.get("direction") != "forward": errors.append(("bad dir", e))
    if abs(e.get("weight", -1) - weights.get(e["type"], -2)) > 1e-9: errors.append(("bad weight", e))
    if e["source"] == e["target"]: self_edges += 1
    for k in ("source","target"):
        v = e[k]
        if v not in id_set and v not in neighbor_refs:
            errors.append(("unresolved " + k, v))
print("edge validation errors:", errors[:20], "count:", len(errors))
print("self edges:", self_edges)

# node field validation
for n in nodes:
    for k in ("id","type","name","summary","tags","complexity"):
        if not n.get(k): print("MISSING FIELD", k, n.get("id"))
    if n["type"] in ("file","config","document","service","pipeline","schema","resource","table") and not n.get("filePath"):
        print("MISSING filePath", n["id"])
    if n["type"] in ("function","class") and not n.get("lineRange"): print("MISSING lineRange", n["id"])
    if not (3 <= len(n["tags"]) <= 5): print("TAG COUNT", n["id"], n["tags"])
