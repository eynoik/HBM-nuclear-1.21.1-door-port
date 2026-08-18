from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parent.parent
p = root / 'src/main/java/com/hbmdoorsport/HbmDoorsPort.java'
if p.exists():
    s = p.read_text()
    if 'uniformDoorProperties()' not in s:
        s = s.replace('"round_airlock_door", RoundAirlockDoorBlock::new, roundAirlockProperties());', '"round_airlock_door", RoundAirlockDoorBlock::new, uniformDoorProperties());')
        s = s.replace('new LegacyDoorBlock(type, p), legacyDoorProperties(type))', 'new LegacyDoorBlock(type, p), uniformDoorProperties())')
        s = s.replace('new SpecialDoorBlock(type, p), heavyDoorProperties(type))', 'new SpecialDoorBlock(type, p), uniformDoorProperties())')
        pattern = re.compile(r'\n    /\*\* Exact block hardness / blast resistance values from the pinned HBM 1\.12\.2 fork\. \*/.*?\n    private static Supplier<SoundEvent> sound', re.S)
        replacement = '''
    /** One gameplay-oriented durability profile shared by every ported HBM door. */
    private static BlockBehaviour.Properties uniformDoorProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(60F, 1_500F)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .dynamicShape();
    }

    private static Supplier<SoundEvent> sound'''
        s, count = pattern.subn(replacement, s)
        if count != 1:
            raise RuntimeError(f'Expected one durability section, replaced {count}')
        p.write_text(s)

        gp = root / 'gradle.properties'
        g = gp.read_text().replace('mod_version=0.3.7-alpha.1', 'mod_version=0.3.8-alpha.1')
        gp.write_text(g)

        status = root / 'PORT_STATUS.md'
        old = status.read_text()
        header = '# Port status\n\n## 0.3.8-alpha.1\n- All 15 ported doors now use identical gameplay durability: hardness 60 and explosion resistance 1500.\n- Correct-tool requirement and pickaxe mining remain enabled.\n- Removes the extreme per-door legacy hardness spread.\n- No animation, sound, access, render or multiblock changes.\n\n'
        if old.startswith('# Port status\n'):
            old = old[len('# Port status\n'):].lstrip('\n')
        status.write_text(header + old)

        for rel in ['tools/sitecustomize.py', '.github/workflows/normalize-durability-0.3.8.yml', 'DURABILITY_0.3.8_TRIGGER']:
            (root / rel).unlink(missing_ok=True)

        subprocess.run(['git','config','user.name','github-actions[bot]'], cwd=root, check=True)
        subprocess.run(['git','config','user.email','41898282+github-actions[bot]@users.noreply.github.com'], cwd=root, check=True)
        subprocess.run(['git','add','-A'], cwd=root, check=True)
        subprocess.run(['git','commit','-m','Normalize all HBM door durability to 60/1500'], cwd=root, check=True)
        subprocess.run(['git','push'], cwd=root, check=True)
