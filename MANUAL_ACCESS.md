# HBM 1.12.2 empty-hand door access

Source of truth: `mlbv/Hbm-s-Nuclear-Tech-GIT-Custom-1.12.2` commit `900ca58f1b08e915903913e469e719f5fccd1134`.

The pinned fork does **not** make all of the ported doors directly usable with an empty hand.

## Empty hand: allowed

- `sliding_blast_door` — the normal Sliding Blast Door. Its `TileEntitySlidingBlastDoor#canAccess(EntityPlayer)` explicitly returns true while the door is not locked.

## Empty hand: denied by the original access path

These route activation through `TileEntityLockableBase#canAccess`, whose implementation in the pinned fork does not grant empty-hand access:

- `round_airlock_door`
- `sliding_seal_door`
- `sliding_gate_door`
- `secure_access_door`
- `hatch`
- `fire_door`
- `qe_sliding_door`
- `qe_containment_door`
- `water_door`
- `large_vehicle_door`
- `transition_seal`
- `blast_door`
- `vault_door`
- `silo_hatch`

The original HBM access-controlled doors could instead be operated through the lock/key/pin/lockpick system (and, where implemented, redstone/control-panel behavior). The standalone door port does not yet include those HBM access items, so 0.3.5 restores the empty-hand restriction while keeping the existing redstone path.

The special Sliding Blast Door keypad/key variants from HBM are separate variants and are not represented by the port's current normal `sliding_blast_door` block.
