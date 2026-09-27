import json, glob, os, collections

UA = r"E:/WorkSpace/damai/.ua"
inp = json.load(open(UA + "/tmp/ua-file-analyzer-input-2.json", encoding="utf-8"))
files = [b["path"] for b in inp["batchFiles"]]
imp = inp["batchImportData"]

parts = sorted(glob.glob(UA + "/intermediate/batch-2-part-*.json"))
single = UA + "/intermediate/batch-2.json"
paths = parts if parts else ([single] if os.path.exists(single) else [])

VALID_EDGE_TYPES = {"contains", "imports", "calls", "inherits", "implements", "exports", "depends_on",
                    "tested_by", "configures", "documents", "deploys", "migrates", "triggers",
                    "defines_schema", "serves", "provisions", "routes", "related"}
VALID_NODE_TYPES = {"file", "function", "class", "config", "document", "service", "table", "endpoint",
                    "pipeline", "schema", "resource"}

all_nodes, all_edges = {}, []
errors = []
for f in paths:
    d = json.load(open(f, encoding="utf-8"))
    print(os.path.basename(f), "nodes=", len(d["nodes"]), "edges=", len(d["edges"]),
          "ok<=60/120:", len(d["nodes"]) <= 60 and len(d["edges"]) <= 120)
    for n in d["nodes"]:
        if n["id"] in all_nodes:
            errors.append("dup node " + n["id"])
        all_nodes[n["id"]] = n
        for req in ("id", "type", "name", "summary", "tags", "complexity"):
            if not n.get(req):
                errors.append("missing " + req + " on " + n["id"])
        if n["type"] not in VALID_NODE_TYPES:
            errors.append("bad node type " + n["type"])
        if n["type"] in {"file", "config", "document", "service", "pipeline", "schema", "resource"} and not n.get("filePath"):
            errors.append("missing filePath " + n["id"])
    all_edges += d["edges"]

print("total nodes", len(all_nodes), "total edges", len(all_edges))

# coverage
file_nodes = {n["filePath"]: n["id"] for n in all_nodes.values() if n["type"] == "file"}
missing = [p for p in files if p not in file_nodes]
print("batchFiles", len(files), "file-level nodes", len(file_nodes), "missing", missing)
extra = [p for p in file_nodes if p not in files]
print("extra file nodes", extra)

# import parity
imp_edges = [e for e in all_edges if e["type"] == "imports"]
expected = sum(len(v) for v in imp.values())
print("import edges", len(imp_edges), "expected", expected, "match", len(imp_edges) == expected)
got = collections.Counter(e["source"] for e in imp_edges)
for p, targets in imp.items():
    if got.get("file:" + p, 0) != len(targets):
        errors.append(f"import mismatch {p}: {got.get('file:' + p, 0)} != {len(targets)}")
print("per-file import parity:", "OK" if not any("import mismatch" in e for e in errors) else "FAIL")

# edge validity
nb = json.load(open(UA + "/intermediate/batch-input-2.json", encoding="utf-8"))["neighborMap"]
known_files = set(files) | {p for v in imp.values() for p in v} | {n["path"] for v in nb.values() for n in v}
syms = collections.defaultdict(set)
for v in nb.values():
    for n in v:
        syms[n["path"]].update(n["symbols"])

def resolvable(nid):
    if nid in all_nodes:
        return True
    for pref in ("file:", "config:", "document:", "service:", "pipeline:", "schema:", "resource:", "table:", "endpoint:"):
        if nid.startswith(pref):
            return nid[len(pref):].split(":")[0] in known_files
    for pref in ("function:", "class:"):
        if nid.startswith(pref):
            rest = nid[len(pref):]
            for p in known_files:
                if rest.startswith(p + ":"):
                    return rest[len(p) + 1:] in syms.get(p, set())
    return False

bad = [e for e in all_edges if not resolvable(e["target"]) or not resolvable(e["source"])]
print("unresolvable edges", len(bad))
for e in bad[:10]:
    print("   ", e)

selfs = [e for e in all_edges if e["source"] == e["target"]]
print("self edges", len(selfs))
badtype = [e for e in all_edges if e["type"] not in VALID_EDGE_TYPES]
print("bad edge types", badtype)
badw = [(e["type"], e["weight"]) for e in all_edges if e["weight"] not in (0.5, 0.6, 0.7, 0.8, 0.9, 1.0)]
print("bad weights", badw)
nokeys = [e for e in all_edges if not all(k in e for k in ("source", "target", "type", "direction", "weight"))]
print("edges missing keys", len(nokeys))
print("edge type counts", collections.Counter(e["type"] for e in all_edges))
print("node type counts", collections.Counter(n["type"] for n in all_nodes.values()))
print("ERRORS:", errors if errors else "none")
