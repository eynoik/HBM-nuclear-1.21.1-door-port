#!/usr/bin/env python3

from __future__ import annotations

import hashlib
import shutil
import sys
from pathlib import Path

# Pinned source asset map from mlbv/Hbm-s-Nuclear-Tech-GIT-Custom-1.12.2
# commit 900ca58f1b08e915903913e469e719f5fccd1134.
# Destination paths are under assets/hbmdoors.
ASSETS = [
    ('src/main/resources/assets/hbm/models/anim/door0.dae', 'models/anim/door0.dae', '8a50dc9f352aa9e21835ddb21108c534da468603ffc6e1b27a6c1020d7dd0ca1'),
    ('src/main/resources/assets/hbm/models/anim/hatch.dae', 'models/anim/hatch.dae', 'e83367b0321e7152c1fbc42bf60cd4be840cfcef82f3924d43fb14ecc92b38d4'),
    ('src/main/resources/assets/hbm/models/blast_door_base.obj', 'models/blast_door_base.obj', 'a48b09fcdc77381ec33c717ce7f5981e48ae657f24ca2e2b1da782aeba8af815'),
    ('src/main/resources/assets/hbm/models/blast_door_block.obj', 'models/blast_door_block.obj', 'eb7bcacd2893f4352b652e08dce3809f9ad62f4910b45354fd226833dc501634'),
    ('src/main/resources/assets/hbm/models/blast_door_slider.obj', 'models/blast_door_slider.obj', 'c9657b1701a393991862ddb25edb727ea8e2712cd86b4522eec493707423f64e'),
    ('src/main/resources/assets/hbm/models/blast_door_tooth.obj', 'models/blast_door_tooth.obj', '8d0d4ac027eda90f2c813172f05a569738b77067ef1ae09d34c1e52ba469041b'),
    ('src/main/resources/assets/hbm/models/doors/fire_door.obj', 'models/doors/fire_door.obj', 'ab909197dc07ce491b51267029f42452c65d1e3f22999071081456372ded3f01'),
    ('src/main/resources/assets/hbm/models/doors/hatch.obj', 'models/doors/hatch.obj', '5c196102f6e45f12208d1ebcfac50e42d8b4a5f9ba5ff6b9dc950e8e1e40fdf5'),
    ('src/main/resources/assets/hbm/models/doors/large_vehicle_door.obj', 'models/doors/large_vehicle_door.obj', '1ecd7fb11813fced5ac882d8be5619e405e0de0670a17fb6488de53f95e8d422'),
    ('src/main/resources/assets/hbm/models/doors/qe_containment.obj', 'models/doors/qe_containment.obj', '6419f8c3918f5418e5786558608d94e02efcc805cf9d0dd3cd30a69776e26f63'),
    ('src/main/resources/assets/hbm/models/doors/qe_sliding_door.obj', 'models/doors/qe_sliding_door.obj', 'f7e9b6c67661bd248059917d430a74094dafd93ab89d62ecebd63d9371fadf57'),
    ('src/main/resources/assets/hbm/models/doors/round_airlock_door.obj', 'models/doors/round_airlock_door.obj', 'ace3886efb882a811ec640e86ceb58e97e9941b91ec0c8c27eb7038d54c1cb9d'),
    ('src/main/resources/assets/hbm/models/doors/seal.dae', 'models/doors/seal.dae', 'ce22d2afdca1ed0319cbf58ef0548dd16c24e62fc7c5dea687652587fe9efafe'),
    ('src/main/resources/assets/hbm/models/doors/secure_access_door.obj', 'models/doors/secure_access_door.obj', 'aae88022fda02acc4bec8b599cdc18c81dfcc462ad2a287bee13285efb8f412d'),
    ('src/main/resources/assets/hbm/models/doors/sliding_seal_door.obj', 'models/doors/sliding_seal_door.obj', 'f9a59eb767f92978e301c0d7d09c8bffed914aa8c848a040fff649c6f016f4f7'),
    ('src/main/resources/assets/hbm/models/doors/water_door.obj', 'models/doors/water_door.obj', 'a4ebc984f951e21b1672421bfca8acace19f78962d045da4bad664f4404ea711'),
    ('src/main/resources/assets/hbm/models/vault_cog.obj', 'models/vault_cog.obj', '26899a37217abd4c85093ce6ab34298910239e3159b9c0a4cd9f6df9a3876c28'),
    ('src/main/resources/assets/hbm/models/vault_frame.obj', 'models/vault_frame.obj', 'ca62336607422501b5e6032608c98ca7246fb8bdff0f5577ac2fbe80bbe06d18'),
    ('src/main/resources/assets/hbm/models/vault_label.obj', 'models/vault_label.obj', '3db0ea41032732683252a2f06f5e7e25b571cd9a4cd7648c187c8f3c88412ac9'),
    ('src/main/resources/assets/hbm/models/vault_teeth.obj', 'models/vault_teeth.obj', 'f1486ec266b4498e4ba72b40f5ac1f5621ede4df58b00abbb97e8cc2beef5135'),
    ('src/main/resources/assets/hbm/sounds/block/doors/alarm6.ogg', 'sounds/block/doors/alarm6.ogg', 'ca41480375c98e01ef3c876f104b316076f165c5d81d8f78b40f393358de9511'),
    ('src/main/resources/assets/hbm/sounds/block/doors/door_wgh_big_start.ogg', 'sounds/block/doors/door_wgh_big_start.ogg', 'b1e9f6c2a044dfd73894f9c0ffd79cc981bece80a10812cc21a6c51e48aaa9b2'),
    ('src/main/resources/assets/hbm/sounds/block/doors/door_wgh_big_stop.ogg', 'sounds/block/doors/door_wgh_big_stop.ogg', '625e9229623a8bae933855b3350d87df0a524ff463d0ed5afebf837e8538fb42'),
    ('src/main/resources/assets/hbm/sounds/block/doors/door_wgh_start.ogg', 'sounds/block/doors/door_wgh_start.ogg', '14e281f01aea16edfb58afc43a9259fa8ff4509347594938889de718aef945ce'),
    ('src/main/resources/assets/hbm/sounds/block/doors/door_wgh_stop.ogg', 'sounds/block/doors/door_wgh_stop.ogg', '188a215c5db60f3f5919c1e8dbb6457e7f04e9b819d2082d815637c2141ee066'),
    ('src/main/resources/assets/hbm/sounds/block/doors/doormove2.ogg', 'sounds/block/doors/doormove2.ogg', '4525c97a751f6802fc3a78c6df61479b57baf99ba0935f2e069eedb2c0e19cd0'),
    ('src/main/resources/assets/hbm/sounds/block/doors/doorshut_1.ogg', 'sounds/block/doors/doorshut_1.ogg', 'deae59bb30b07a8130086c253757474d39ddd4330a18a26955a12740c19d99ca'),
    ('src/main/resources/assets/hbm/sounds/block/doors/doorslide_opened1.ogg', 'sounds/block/doors/doorslide_opened1.ogg', '149d156aed0fa1bc48b3a30ba9bd3b07f7202b634f4f2dd0985ac395eccf893b'),
    ('src/main/resources/assets/hbm/sounds/block/doors/doorslide_opening1.ogg', 'sounds/block/doors/doorslide_opening1.ogg', '116b45cd71a86305094513e1a1ce908a6a15c460600109add2d5869b4144a4a1'),
    ('src/main/resources/assets/hbm/sounds/block/doors/garage_move.ogg', 'sounds/block/doors/garage_move.ogg', '5f65a51e6a39d35d7fe9a8b86c1429856a1babd35a150dfc3f4f310550bf9d60'),
    ('src/main/resources/assets/hbm/sounds/block/doors/garage_stop.ogg', 'sounds/block/doors/garage_stop.ogg', 'f22e0a4bae143b926894002d33cf4416be4cc3d02b7e7f0762e72f4868ac9f01'),
    ('src/main/resources/assets/hbm/sounds/block/doors/hatch_open1.ogg', 'sounds/block/doors/hatch_open1.ogg', 'cc4f29983b5e90265791845d942336ae8df2edb0f8cc25d6c59296f2a3065a26'),
    ('src/main/resources/assets/hbm/sounds/block/doors/lever1.ogg', 'sounds/block/doors/lever1.ogg', '431518c831f38992ab768dcd292ed0cb0e5e6923bbbda533481d93093f233d7b'),
    ('src/main/resources/assets/hbm/sounds/block/doors/metal_stop1.ogg', 'sounds/block/doors/metal_stop1.ogg', '3a6336ad86bde1b81a8334487d9ffc97fdc54d3bc15fca93f07ebfc4c12954cf'),
    ('src/main/resources/assets/hbm/sounds/block/doors/siloclose.ogg', 'sounds/block/doors/siloclose.ogg', 'd02570a86fbc6f10c48bd53be5df98eb637a1dd7edd13e2c40e26d4f1c228142'),
    ('src/main/resources/assets/hbm/sounds/block/doors/siloopen.ogg', 'sounds/block/doors/siloopen.ogg', '0d9817955d7368368d708ba21043578a7f5907aae5dbe932c6b3e9edb98a279d'),
    ('src/main/resources/assets/hbm/sounds/block/doors/transition_seal_open.ogg', 'sounds/block/doors/transition_seal_open.ogg', '7584a0f27eb25f31697eee5359c2ceba99841e694a5efa49f72f729c57baee83'),
    ('src/main/resources/assets/hbm/sounds/block/reactorstart.ogg', 'sounds/block/reactorstart.ogg', '8fb909aebb12e7043e7e8167435eb291faaae2f5a75f80598c828dbb09481ca5'),
    ('src/main/resources/assets/hbm/sounds/block/reactorstop.ogg', 'sounds/block/reactorstop.ogg', '839d975e0488006038881f24f2a0a0f3b188e0439041042fe2d3fc62daa32617'),
    ('src/main/resources/assets/hbm/sounds/block/vaultscrapenew.ogg', 'sounds/block/vaultscrapenew.ogg', '1a2001a29d703bdc96797e8bc2903bf718a31c12b4bd8976e27f96c7292cbffa'),
    ('src/main/resources/assets/hbm/sounds/block/vaultthudnew.ogg', 'sounds/block/vaultthudnew.ogg', 'f6ead97a83b62450ed491b6bfb90be03b6c59d9aee62376f2277b5d2262deb30'),
    ('src/main/resources/assets/hbm/textures/blocks/block_steel.png', 'textures/block/block_steel.png', '98c9a5ec4f708da2b4781cfc0619e98e1c8240e7be1706b29f7e8a5d9e80f3a9'),
    ('src/main/resources/assets/hbm/textures/models/doors/blast/blast_door_base.png', 'textures/models/doors/blast/blast_door_base.png', 'bfa25a252b42f15b8d1495c741e86994c73dcad72408e2933d6dba375e89276f'),
    ('src/main/resources/assets/hbm/textures/models/doors/blast/blast_door_block.png', 'textures/models/doors/blast/bllast_door_block.png', 'fa6f2158c57da986e64d5f0bc09c719aa4bde7db3007fc9c989571c56bb2c1b8'),
    ('src/main/resources/assets/hbm/textures/models/doors/blast/blast_door_slider.png', 'textures/models/doors/blast/blast_door_slider.png', 'cc411f75d6e9ae381560df9bfdb0c597c82b351fddc329ec601c658fae449068'),
    ('src/main/resources/assets/hbm/textures/models/doors/blast/blast_door_tooth.png', 'textures/models/doors/blast/blast_door_tooth.png', '960786f54e6829ccfa7e247094c1d4269c64aa5525ca143251582d092cda862d'),
    ('src/main/resources/assets/hbm/textures/models/doors/fire_door.png', 'textures/models/doors/fire_door.png', '8c1ad9944338c2bf55b7e7894ca4976ece9962f3fbb58bfd7f9fa3701d2a46da'),
    ('src/main/resources/assets/hbm/textures/models/doors/hatch.png', 'textures/models/doors/hatch.png', 'ff38340895b20f0cf95506734d506e267b463a305edd804b6964ccc672058cbf'),
    ('src/main/resources/assets/hbm/textures/models/doors/hatchtexture.png', 'textures/models/doors/hatchtexture.png', 'a271008fffc7dcca45e2aaec7d563ea6ada7f37d222ca33b489ca475e0e754e6'),
    ('src/main/resources/assets/hbm/textures/models/doors/large_vehicle_door.png', 'textures/models/doors/large_vehicle_door.png', 'a98ccd40754af123a36bda51485c0271cd4890f3c74ed412be3ef81dcb400336'),
    ('src/main/resources/assets/hbm/textures/models/doors/qe_containment.png', 'textures/models/doors/qe_containment.png', 'a390442b8ecd82c150327ded43645b9bb8e79b2604fb7ba1cbd8c38fd7fd5887'),
    ('src/main/resources/assets/hbm/textures/models/doors/qe_containment_decal.png', 'textures/models/doors/qe_containment_decal.png', '96d44f88d63ab3b6011cd1435828f97542adc58a3d07ef6814b4e495e20fe33b'),
    ('src/main/resources/assets/hbm/textures/models/doors/qe_sliding_door.png', 'textures/models/doors/qe_sliding_door.png', '2ce065328aa836123599ac02b29a101d2ac13a176dcfb551a6ad9cf48cbe80b9'),
    ('src/main/resources/assets/hbm/textures/models/doors/round_airlock_door.png', 'textures/models/doors/round_airlock_door.png', 'b853201eecca40999e4a2b46d1d9e7db787758e7d2271d88c13c86f3330df099'),
    ('src/main/resources/assets/hbm/textures/models/doors/secure_access_door.png', 'textures/models/doors/secure_access_door.png', '23bdaf6959a34136cac9149b704a6583c2a9bd2bc796a57009187f7d5d84538b'),
    ('src/main/resources/assets/hbm/textures/models/doors/sliding_gate_door.png', 'textures/models/doors/sliding_gate_door.png', '654937d118b4f3e82d02385d57a62959e405ee49e08e2fa86b81af7c649a1eb6'),
    ('src/main/resources/assets/hbm/textures/models/doors/sliding_seal_door.png', 'textures/models/doors/sliding_seal_door.png', '07eb150325674b1af74de522f887764047f801f80ae166afbc1d9ce78b5f8fa2'),
    ('src/main/resources/assets/hbm/textures/models/doors/slidingblast/sliding_blast_door.png', 'textures/models/doors/slidingblast/sliding_blast_door.png', 'b5fc69fbe5f0663c0c68fc53e45dd80ff80099d6633cbdfa9d6459a7d6893b11'),
    ('src/main/resources/assets/hbm/textures/models/doors/transition_seal.png', 'textures/models/doors/transition_seal.png', '948a13b18e38d101cfab3f977f140efe9243befab68ff4f5e395db952f71ace1'),
    ('src/main/resources/assets/hbm/textures/models/doors/vault/vault_cog_1.png', 'textures/models/doors/vault/vault_cog_1.png', 'ada3bfea8b5522d15b2097859ef0094d4e6ef3e913f092c352519f1a5dbabf6c'),
    ('src/main/resources/assets/hbm/textures/models/doors/vault/vault_cog_2.png', 'textures/models/doors/vault/vault_cog_2.png', 'ae3b5fe1d65972f78e4b593bafb47767f6085f413466067b9752b2ab48e04132'),
    ('src/main/resources/assets/hbm/textures/models/doors/vault/vault_cog_3.png', 'textures/models/doors/vault/vault_cog_3.png', 'de085026f3e2c2f58c000620cba507fb970504744e8f120135a8ad071d01d03a'),
    ('src/main/resources/assets/hbm/textures/models/doors/vault/vault_cog_4.png', 'textures/models/doors/vault/vault_cog_4.png', '85130980595bab2575071524e35cc0ba92386f3309ebe34fa74c4317fda9a2cc'),
    ('src/main/resources/assets/hbm/textures/models/doors/vault/vault_frame.png', 'textures/models/doors/vault/vault_frame.png', 'd9b5591f8e919a67c44eb9945cacc51f75a0d72e84b3ddd91be665e9344e4d68'),
    ('src/main/resources/assets/hbm/textures/models/doors/vault/vault_frame_old.png', 'textures/models/doors/vault/vault_frame_old.png', 'f27d3c62d7364bc144aa70b76ddb04ccf44ccf1aa9ea21f5ecb0042bc2cd0732'),
    ('src/main/resources/assets/hbm/textures/models/doors/vault/vault_label_1.png', 'textures/models/doors/vault/vault_label_1.png', '505d3a68163fb59d8bb5ad38c5f00d2982a0089e9b89e398c63879044dfc872f'),
    ('src/main/resources/assets/hbm/textures/models/doors/vault/vault_label_2.png', 'textures/models/doors/vault/vault_label_2.png', '75803ed5dbf87c19a547776e80ab7694de152a479a9ee764d1fd599165a5dafb'),
    ('src/main/resources/assets/hbm/textures/models/doors/vault/vault_label_3.png', 'textures/models/doors/vault/vault_label_3.png', '253c948c3fa6c980dfb0f23d95b138a562744a3f42d44a6e35122f41e5dd8e2f'),
    ('src/main/resources/assets/hbm/textures/models/doors/vault/vault_label_4.png', 'textures/models/doors/vault/vault_label_4.png', '06e7e618ab8fdfab265072aeea3d4f50c9fea27dcce4acba4a0b0c8f5c3a31e6'),
    ('src/main/resources/assets/hbm/textures/models/doors/vault/vault_label_5.png', 'textures/models/doors/vault/vault_label_5.png', '52c0bce144a62f18303fe35536c9bffb1004d2726d29c7e7f500302fff5ec64c'),
    ('src/main/resources/assets/hbm/textures/models/doors/vault/vault_label_6.png', 'textures/models/doors/vault/vault_label_6.png', 'e2df2089d32db5e6d37600fdd9b5da6873b0b5d0d3847e2e612b3df523d92fe3'),
    ('src/main/resources/assets/hbm/textures/models/doors/vault/vault_label_7.png', 'textures/models/doors/vault/vault_label_7.png', 'ddd7c17594dbe91cc83ae20042d23a7114db6a8b5e7751423c988945bb235299'),
    ('src/main/resources/assets/hbm/textures/models/doors/vault/vault_label_8.png', 'textures/models/doors/vault/vault_label_8.png', '705be2e907a8f7c671971d7d1a4d7e2a8635275fbf7948745597b2216948df73'),
    ('src/main/resources/assets/hbm/textures/models/doors/water_door.png', 'textures/models/doors/water_door.png', '734dfaa4f61e6af0c50a717d6895df6567249ec1e779c009047973706b0f21ac'),
    ('src/main/resources/assets/hbm/textures/models/misc/universaldark.png', 'textures/models/misc/universaldark.png', 'beb22eb88381a4eaa1b5d769aa0512440df4deff828506d137a61b8dc1495ca1'),
]

def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main() -> int:
    if len(sys.argv) == 2 and sys.argv[1] == "--sources":
        for src_rel, _, _ in ASSETS:
            print(src_rel)
        return 0
    if len(sys.argv) != 2:
        print("usage: bootstrap_assets.py <upstream-hbm-root> | --sources", file=sys.stderr)
        return 2
    upstream = Path(sys.argv[1]).resolve()
    target = Path(__file__).resolve().parents[1] / "src/main/resources/assets/hbmdoors"
    copied = 0
    for src_rel, dst_rel, expected in ASSETS:
        src = upstream / src_rel
        dst = target / dst_rel
        if not src.is_file():
            raise FileNotFoundError(src)
        actual = sha256(src)
        if actual != expected:
            raise RuntimeError(f"upstream asset changed: {src_rel}\nexpected {expected}\nactual   {actual}")
        dst.parent.mkdir(parents=True, exist_ok=True)
        if not dst.exists() or sha256(dst) != expected:
            shutil.copyfile(src, dst)
            copied += 1
        if sha256(dst) != expected:
            raise RuntimeError(f"copy verification failed: {dst_rel}")
    print(f"HBM preserved assets ready: {len(ASSETS)} checked, {copied} copied")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
