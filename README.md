# HBM Nuclear Door Port — NeoForge 1.21.1

Standalone port of the animated HBM Nuclear Tech doors. This repository is intentionally separate from Battle Towers.

## Ported doors

### DoorDecl / OBJ family
- Round Airlock Door
- Sliding Seal Door
- Sliding Gate Door
- Secure Access Door
- Hatch
- Fire Door
- QE Sliding Door
- QE Containment Door
- Water Door
- Large Vehicle Door

### Bespoke legacy renderers
- Transition Seal — original `seal.dae` keyframes
- Blast Door — original procedural 5-second segmented lift
- Sliding Blast Door — original `door0.dae` keyframes
- Vault Door — original pull-out / roll-away transform math
- Silo Hatch — original `hatch.dae` keyframes

All doors support right-click toggling and redstone. Vanilla-only standalone recipes and item icons are included.

## Preservation rule
The original HBM OBJ/DAE/PNG/OGG assets are copied unchanged. The 1.21.1 code replaces only the old GL11/TESR transport layer with NeoForge BlockEntity renderers.

## Build
Java 21, Minecraft 1.21.1, NeoForge 21.1.x.

```bash
./gradlew build
```

## License / attribution
See `LICENSE` and `NOTICE.md`. Original HBM Nuclear Tech assets/code remain subject to their upstream license and attribution requirements.
