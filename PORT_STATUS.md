# Port status

## 0.3.1-alpha.1
- Fixed all 15 inventory icons by moving them back onto a valid item/block atlas sprite (`block_steel`), matching the legacy HBM item-model approach instead of pointing at raw model textures.
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
