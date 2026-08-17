#!/usr/bin/env python3
from __future__ import annotations
import hashlib
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
EXPECTED = {
    "src/main/resources/assets/hbmdoors/models/doors/round_airlock_door.obj": "ace3886efb882a811ec640e86ceb58e97e9941b91ec0c8c27eb7038d54c1cb9d",
    "src/main/resources/assets/hbmdoors/textures/models/doors/round_airlock_door.png": "b853201eecca40999e4a2b46d1d9e7db787758e7d2271d88c13c86f3330df099",
    "src/main/resources/assets/hbmdoors/sounds/block/doors/garage_move.ogg": "5f65a51e6a39d35d7fe9a8b86c1429856a1babd35a150dfc3f4f310550bf9d60",
    "src/main/resources/assets/hbmdoors/sounds/block/doors/garage_stop.ogg": "f22e0a4bae143b926894002d33cf4416be4cc3d02b7e7f0762e72f4868ac9f01",
}

def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()

failed = False
for rel, expected in EXPECTED.items():
    p = ROOT / rel
    actual = sha256(p)
    ok = actual == expected
    print(f"{'OK' if ok else 'FAIL'} {rel}\n  {actual}")
    failed |= not ok

obj = ROOT / "src/main/resources/assets/hbmdoors/models/doors/round_airlock_door.obj"
group = "default"
counts: dict[str, int] = {}
verts = []
for raw in obj.read_text(errors="strict").splitlines():
    line = raw.strip()
    if line.startswith("v "):
        _, x, y, z, *_ = line.split()
        verts.append((float(x), float(y), float(z)))
    elif line.startswith(("o ", "g ")):
        parts = line.split(maxsplit=1)
        group = parts[1] if len(parts) == 2 else "default"
        counts.setdefault(group, 0)
    elif line.startswith("f "):
        n = len(line.split()) - 1
        counts[group] = counts.get(group, 0) + max(0, n - 2)

expected_groups = {"frame": 396, "doorRight": 322, "doorLeft": 324}
print("OBJ triangle groups:", counts)
if any(counts.get(k) != v for k, v in expected_groups.items()):
    print("FAIL unexpected OBJ group/triangle counts")
    failed = True
else:
    print("OK original frame/doorRight/doorLeft group topology")

if verts:
    bounds = tuple((min(v[i] for v in verts), max(v[i] for v in verts)) for i in range(3))
    print("OBJ bounds x/y/z:", bounds)

for p in ROOT.glob("src/main/resources/**/*.json"):
    try:
        json.loads(p.read_text())
    except Exception as e:
        print(f"FAIL JSON {p.relative_to(ROOT)}: {e}")
        failed = True
print("OK JSON syntax" if not failed else "Verification had failures")
sys.exit(1 if failed else 0)
