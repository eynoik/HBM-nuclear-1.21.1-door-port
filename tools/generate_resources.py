#!/usr/bin/env python3
import json
import shutil
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/hbmdoors'
IDS = [
    'round_airlock_door','sliding_seal_door','sliding_gate_door','secure_access_door','hatch',
    'fire_door','qe_sliding_door','qe_containment_door','water_door','large_vehicle_door',
    'transition_seal','blast_door','sliding_blast_door','vault_door','silo_hatch'
]
BROKEN_ICON_IDS = {'qe_containment_door', 'sliding_blast_door'}


def png_chunk(kind: bytes, data: bytes) -> bytes:
    return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)


def write_rgba_png(path: Path, pixels, width=32, height=32):
    raw = bytearray()
    for y in range(height):
        raw.append(0)  # PNG filter: none
        for x in range(width):
            raw.extend(pixels[y][x])
    data = b'\x89PNG\r\n\x1a\n'
    data += png_chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
    data += png_chunk(b'IDAT', zlib.compress(bytes(raw), 9))
    data += png_chunk(b'IEND', b'')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def blank():
    return [[(0, 0, 0, 0) for _ in range(32)] for _ in range(32)]


def rect(px, x0, y0, x1, y1, color):
    for y in range(max(0, y0), min(32, y1 + 1)):
        for x in range(max(0, x0), min(32, x1 + 1)):
            px[y][x] = color


def line(px, x0, y0, x1, y1, color):
    dx = abs(x1 - x0); sx = 1 if x0 < x1 else -1
    dy = -abs(y1 - y0); sy = 1 if y0 < y1 else -1
    err = dx + dy
    while True:
        if 0 <= x0 < 32 and 0 <= y0 < 32: px[y0][x0] = color
        if x0 == x1 and y0 == y1: break
        e2 = 2 * err
        if e2 >= dy: err += dy; x0 += sx
        if e2 <= dx: err += dx; y0 += sy


def make_qe_containment_icon(path: Path):
    # Explicit sprite instead of sampling the legacy UV atlas. The old sampling path can
    # land on an empty/near-black atlas region, which is why this item looked iconless.
    px = blank()
    dark = (22, 25, 27, 255); edge = (86, 94, 98, 255)
    steel = (48, 54, 57, 255); light = (108, 116, 118, 255)
    yellow = (196, 154, 34, 255); black = (18, 18, 17, 255)
    rect(px, 4, 2, 27, 29, dark)
    rect(px, 5, 3, 26, 28, edge)
    rect(px, 7, 5, 24, 26, steel)
    rect(px, 8, 6, 23, 8, light)
    rect(px, 15, 5, 16, 26, dark)
    rect(px, 8, 21, 23, 25, black)
    for x in range(8, 24, 6):
        for off in range(5):
            line(px, x + off, 25, x + 5 + off, 21, yellow)
    rect(px, 10, 11, 13, 17, (66, 73, 76, 255))
    rect(px, 18, 11, 21, 17, (66, 73, 76, 255))
    rect(px, 11, 13, 12, 15, (145, 153, 154, 255))
    rect(px, 19, 13, 20, 15, (145, 153, 154, 255))
    write_rgba_png(path, px)


def make_sliding_blast_icon(path: Path):
    # Wide, heavily segmented blast-door silhouette matching the actual sliding door better
    # than a random crop from sliding_blast_door.png.
    px = blank()
    dark = (18, 21, 23, 255); frame = (75, 82, 86, 255)
    steel = (45, 51, 55, 255); hi = (100, 108, 111, 255)
    warn = (183, 137, 31, 255)
    rect(px, 1, 6, 30, 25, dark)
    rect(px, 2, 7, 29, 24, frame)
    rect(px, 4, 9, 27, 22, steel)
    for x in (8, 14, 17, 23): rect(px, x, 9, x + 1, 22, dark)
    rect(px, 4, 10, 27, 11, hi)
    rect(px, 4, 20, 27, 21, (31, 35, 38, 255))
    rect(px, 14, 13, 17, 18, dark)
    rect(px, 15, 14, 16, 17, warn)
    # top/bottom bolts
    for x in (4, 9, 14, 19, 24, 27):
        px[8][x] = (156, 161, 162, 255)
        px[23][x] = (156, 161, 162, 255)
    write_rgba_png(path, px)


item_tex = ASSETS / 'textures/item'
item_model = ASSETS / 'models/item'
particle_tex = ASSETS / 'textures/block/door_particles'
block_model = ASSETS / 'models/block'
item_tex.mkdir(parents=True, exist_ok=True)
item_model.mkdir(parents=True, exist_ok=True)
particle_tex.mkdir(parents=True, exist_ok=True)
block_model.mkdir(parents=True, exist_ok=True)

# These two were still visually blank in-game. Replace them deterministically instead of
# depending on atlas-crop heuristics.
make_qe_containment_icon(item_tex / 'qe_containment_door.png')
make_sliding_blast_icon(item_tex / 'sliding_blast_door.png')

for door in IDS:
    icon = item_tex / f'{door}.png'
    if not icon.is_file():
        raise SystemExit(f'Missing dedicated item icon: {icon.relative_to(RES)}')

    # Make item models unambiguous. This also repairs stale JSON from the old block_steel era.
    model = {
        'parent': 'minecraft:item/generated',
        'textures': {'layer0': f'hbmdoors:item/{door}'}
    }
    (item_model / f'{door}.json').write_text(json.dumps(model, indent=2) + '\n', encoding='utf-8')

    # BER-rendered doors have essentially empty vanilla block models, so Minecraft otherwise
    # has no useful particle sprite for hit/break effects. Put the item's dedicated sprite on
    # the block atlas and advertise it as the model's particle texture.
    particle = particle_tex / f'{door}.png'
    shutil.copyfile(icon, particle)

    model_path = block_model / f'{door}.json'
    if model_path.is_file():
        try:
            block_data = json.loads(model_path.read_text(encoding='utf-8'))
        except json.JSONDecodeError:
            block_data = {}
    else:
        block_data = {}
    block_data.setdefault('parent', 'minecraft:block/block')
    block_data.setdefault('textures', {})['particle'] = f'hbmdoors:block/door_particles/{door}'
    model_path.write_text(json.dumps(block_data, indent=2) + '\n', encoding='utf-8')

missing = []
for door in IDS:
    for rel in (
        f'assets/hbmdoors/models/item/{door}.json',
        f'assets/hbmdoors/models/block/{door}.json',
        f'assets/hbmdoors/textures/item/{door}.png',
        f'assets/hbmdoors/textures/block/door_particles/{door}.png',
        f'assets/hbmdoors/blockstates/{door}.json',
        f'data/hbmdoors/recipe/{door}.json',
    ):
        if not (RES / rel).is_file():
            missing.append(rel)
if missing:
    raise SystemExit('Missing static door resources:\n' + '\n'.join(missing))

# Fast PNG sanity check without requiring Pillow on CI.
for door in IDS:
    for folder in ('textures/item', 'textures/block/door_particles'):
        path = ASSETS / folder / f'{door}.png'
        data = path.read_bytes()
        if len(data) < 24 or data[:8] != b'\x89PNG\r\n\x1a\n':
            raise SystemExit(f'Invalid PNG: {path.relative_to(RES)}')
        width, height = struct.unpack('>II', data[16:24])
        if width != 32 or height != 32:
            raise SystemExit(f'Expected 32x32 PNG, got {width}x{height}: {path.relative_to(RES)}')

print(f'validated item icons, item models and break particles for {len(IDS)} doors')
