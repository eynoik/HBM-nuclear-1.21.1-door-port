# Port status

## 0.3.2-alpha.1
- Restored dedicated 32x32 inventory renders for all 15 doors; the temporary `block_steel` placeholder is gone.
- Added a dedicated HBM Doors creative tab containing all ported doors.
- Fixed static Collada node transforms to match HBM 1.12: static 4x4 matrices are multiplied directly instead of being decomposed after transposition. This targets the displaced Transition Seal center and shifted/reversed-looking Silo Hatch pieces.
- Kept the 0.3.1 triangle-to-quad compatibility fix for modern Minecraft entity buffers.
- Restored Sliding Blast Door movement/end sounds using the same `qe_sliding_opening`, `qe_sliding_opened` and `qe_sliding_shut` samples used by HBM 1.12.
- Blast Door connected-group behavior from 0.3.1 is retained.
- Vault Door render and animation path are unchanged.

## 0.3.1-alpha.1
- Fixed the magenta/black inventory failure by temporarily moving item models onto a valid atlas sprite (`block_steel`).
- Fixed Collada triangle rendering for Transition Seal, Sliding Blast Door and Silo Hatch without changing the original DAE files or animation keyframes.
- Restored original HBM connected Blast Door behavior: horizontally adjacent segments open and close as one connected group.
- Connected Blast Door redstone is evaluated across the whole group so one powered segment keeps the group open.
- Vault Door render and animation path are unchanged.

## 0.3.0-alpha.1
- Battle Towers dependency/workspace removed entirely; standalone repository only.
- 10 OBJ DoorDecl doors retained from 0.2.x.
- Added Transition Seal with original Collada model/animation.
- Added Blast Door with original procedural renderer math.
- Added Sliding Blast Door with original Collada model/animation.
- Added Vault Door with original pull-out/slide/roll math.
- Added Silo Hatch with original Collada model/animation.
- Added standalone vanilla recipes and item models for all 15 doors.
- Special-door source is validated through this repository's own NeoForge 1.21.1 CI; Battle Towers is not used as a build workspace.
