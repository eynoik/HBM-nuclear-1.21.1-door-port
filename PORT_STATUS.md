# Port status

## 0.3.4-alpha.1
- Reworks client movement-loop cleanup so a loop is valid only while the exact door BlockEntity is still installed at its world position.
- Explicitly stops and removes stale loop instances from `DoorSoundController` instead of trusting `BlockEntity#isRemoved()` alone.
- Adds the same live-world identity check inside both generic-door and Round Airlock tickable sound instances as a second safety net.
- Targets lingering loop audio after destroying QE Containment Door, Large Vehicle Door, Round Airlock, Secure Access Door and Fire Door while they are moving.
- Keeps the server stop-sound cleanup from 0.3.3 as an additional fallback.
- Every successful `main` build publishes automatically as a GitHub Pre-release with the compiled JAR attached.

## 0.3.3-alpha.1
- Stops HBM door sound samples for nearby clients when an HBM door is broken, preventing positional audio from lingering after the multiblock is removed.
- Restores Transition Seal closing movement audio using the original HBM transition sample.
- Replaces the blank QE Containment Door and Sliding Blast Door inventory sprites with explicit 32x32 icons.
- Adds dedicated break/hit particle sprite bindings for all 15 ported doors.
- Fixes the NeoForge 1.21.1 SoundEvent lookup used by the sound cleanup path.
- Alpha builds from `main` are published automatically as GitHub Pre-releases with the compiled JAR attached.

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
