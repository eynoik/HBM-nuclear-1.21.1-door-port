#!/usr/bin/env python3
import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/hbmdoors'
IDS = [
    'round_airlock_door','sliding_seal_door','sliding_gate_door','secure_access_door','hatch',
    'fire_door','qe_sliding_door','qe_containment_door','water_door','large_vehicle_door',
    'transition_seal','blast_door','sliding_blast_door','vault_door','silo_hatch'
]

# Curated item renders are kept in one reproducible source archive. On CI they are
# unpacked into normal Minecraft item textures; main-branch bootstrap commits them.
icon_zip = ROOT / 'tools/item_icons.zip'
icon_dir = ASSETS / 'textures/item'
icon_dir.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(icon_zip) as zf:
    expected = {f'{door}.png' for door in IDS}
    present = set(zf.namelist())
    missing = expected - present
    if missing:
        raise SystemExit('Missing icons in item_icons.zip: ' + ', '.join(sorted(missing)))
    for name in sorted(expected):
        target = icon_dir / name
        target.write_bytes(zf.read(name))

item_dir = ASSETS / 'models/item'
item_dir.mkdir(parents=True, exist_ok=True)
for door in IDS:
    model = {
        'parent': 'minecraft:item/generated',
        'textures': {'layer0': f'hbmdoors:item/{door}'}
    }
    (item_dir / f'{door}.json').write_text(json.dumps(model, indent=2) + '\n', encoding='utf-8')

missing = []
for door in IDS:
    for rel in (
        f'assets/hbmdoors/models/item/{door}.json',
        f'assets/hbmdoors/textures/item/{door}.png',
        f'assets/hbmdoors/blockstates/{door}.json',
        f'data/hbmdoors/recipe/{door}.json',
    ):
        if not (RES / rel).is_file():
            missing.append(rel)
if missing:
    raise SystemExit('Missing static door resources:\n' + '\n'.join(missing))
print(f'validated static resources and item icons for {len(IDS)} doors')
