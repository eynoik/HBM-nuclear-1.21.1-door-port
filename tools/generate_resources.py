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

# HBM 1.12 itself used its steel block sprite for these inventory items.
# Keep the item texture on the normal block atlas instead of incorrectly pointing
# item/generated at textures/models/*, which produces the magenta/black missing sprite.
item_dir = ASSETS / 'models/item'
item_dir.mkdir(parents=True, exist_ok=True)
for door in IDS:
    model = {
        'parent': 'minecraft:item/generated',
        'textures': {'layer0': 'hbmdoors:block/block_steel'}
    }
    (item_dir / f'{door}.json').write_text(json.dumps(model, indent=2) + '\n', encoding='utf-8')

missing = []
for door in IDS:
    for rel in (
        f'assets/hbmdoors/models/item/{door}.json',
        f'assets/hbmdoors/blockstates/{door}.json',
        f'data/hbmdoors/recipe/{door}.json',
    ):
        if not (RES / rel).is_file():
            missing.append(rel)
if not (ASSETS / 'textures/block/block_steel.png').is_file():
    missing.append('assets/hbmdoors/textures/block/block_steel.png')
if missing:
    raise SystemExit('Missing static door resources:\n' + '\n'.join(missing))
print(f'validated static resources and item models for {len(IDS)} doors')
