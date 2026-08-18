#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/hbmdoors'
IDS = [
    'round_airlock_door','sliding_seal_door','sliding_gate_door','secure_access_door','hatch',
    'fire_door','qe_sliding_door','qe_containment_door','water_door','large_vehicle_door',
    'transition_seal','blast_door','sliding_blast_door','vault_door','silo_hatch'
]

# Every door has its own inventory render. Keep these as normal item textures instead
# of pointing item/generated at large model texture atlases (which Minecraft does not stitch).
item_dir = ASSETS / 'models/item'
item_dir.mkdir(parents=True, exist_ok=True)
for door in IDS:
    model = {
        'parent': 'minecraft:item/generated',
        'textures': {'layer0': f'hbmdoors:item/{door}'}
    }
    (item_dir / f'{door}.json').write_text(json.dumps(model, indent=2) + '\n', encoding='utf-8')

missing = []
bad_icons = []
for door in IDS:
    for rel in (
        f'assets/hbmdoors/models/item/{door}.json',
        f'assets/hbmdoors/textures/item/{door}.png',
        f'assets/hbmdoors/blockstates/{door}.json',
        f'data/hbmdoors/recipe/{door}.json',
    ):
        if not (RES / rel).is_file():
            missing.append(rel)

    icon = ASSETS / f'textures/item/{door}.png'
    if icon.is_file():
        raw = icon.read_bytes()
        if len(raw) < 24 or raw[:8] != b'\x89PNG\r\n\x1a\n':
            bad_icons.append(f'{door}: invalid PNG signature')
        else:
            width = int.from_bytes(raw[16:20], 'big')
            height = int.from_bytes(raw[20:24], 'big')
            if (width, height) != (32, 32):
                bad_icons.append(f'{door}: expected 32x32, got {width}x{height}')

if missing:
    raise SystemExit('Missing static door resources:\n' + '\n'.join(missing))
if bad_icons:
    raise SystemExit('Invalid door item icons:\n' + '\n'.join(bad_icons))
print(f'validated static resources and dedicated 32x32 item icons for {len(IDS)} doors')
