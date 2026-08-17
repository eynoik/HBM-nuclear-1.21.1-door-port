# Provenance / preserved HBM assets

This port preserves assets from `mlbv/Hbm-s-Nuclear-Tech-GIT-Custom-1.12.2`, pinned to commit `900ca58f1b08e915903913e469e719f5fccd1134`.

The original HBM OBJ/DAE/PNG/OGG files are copied without conversion or re-export and SHA-256 verified during bootstrap. The Round Airlock proof hashes remain:

| Asset | SHA-256 |
|---|---|
| `round_airlock_door.obj` | `ace3886efb882a811ec640e86ceb58e97e9941b91ec0c8c27eb7038d54c1cb9d` |
| `round_airlock_door.png` | `b853201eecca40999e4a2b46d1d9e7db787758e7d2271d88c13c86f3330df099` |
| `garage_move.ogg` | `5f65a51e6a39d35d7fe9a8b86c1429856a1babd35a150dfc3f4f310550bf9d60` |
| `garage_stop.ogg` | `f22e0a4bae143b926894002d33cf4416be4cc3d02b7e7f0762e72f4868ac9f01` |

Run `python tools/verify_preserved_assets.py` to verify preserved files and OBJ group topology.

The upstream HBM tree includes GPLv3 licensing. `LICENSE` is copied from the pinned upstream commit during bootstrap.
