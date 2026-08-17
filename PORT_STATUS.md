# Port status

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

Runtime validation still matters for visual orientation, Collada matrix parity and the very large Transition Seal multiblock.
