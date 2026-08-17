#!/usr/bin/env python3
import json
from pathlib import Path
R=Path(__file__).resolve().parents[1]/'src/main/resources'
A=R/'assets/hbmdoors'; D=R/'data/hbmdoors/recipe'
ids=['round_airlock_door','sliding_seal_door','sliding_gate_door','secure_access_door','hatch','fire_door','qe_sliding_door','qe_containment_door','water_door','large_vehicle_door','transition_seal','blast_door','sliding_blast_door','vault_door','silo_hatch']
en=['Round Airlock Door','Sliding Seal Door','Sliding Gate Door','Secure Access Door','Hatch','Fire Door','QE Sliding Door','QE Containment Door','Water Door','Large Vehicle Door','Transition Seal','Blast Door','Sliding Blast Door','Vault Door','Silo Hatch']
pl=['Okrągłe drzwi grodziowe','Przesuwne drzwi uszczelniające','Przesuwna brama','Drzwi bezpiecznego dostępu','Właz','Drzwi przeciwpożarowe','Przesuwne drzwi QE','Drzwi osłony QE','Drzwi wodoszczelne','Duże drzwi pojazdowe','Śluza przejściowa','Drzwi przeciwwybuchowe','Przesuwne drzwi przeciwwybuchowe','Drzwi skarbca','Właz silosu']
tex={
'round_airlock_door':'models/doors/round_airlock_door','sliding_seal_door':'models/doors/sliding_seal_door','sliding_gate_door':'models/doors/sliding_gate_door','secure_access_door':'models/doors/secure_access_door','hatch':'models/doors/hatch','fire_door':'models/doors/fire_door','qe_sliding_door':'models/doors/qe_sliding_door','qe_containment_door':'models/doors/qe_containment','water_door':'models/doors/water_door','large_vehicle_door':'models/doors/large_vehicle_door','transition_seal':'block/block_steel','blast_door':'models/doors/blast/blast_door_block','sliding_blast_door':'models/doors/slidingblast/sliding_blast_door','vault_door':'models/doors/vault/vault_frame','silo_hatch':'models/doors/hatchtexture'}

def write(p,o): p.parent.mkdir(parents=True,exist_ok=True); p.write_text(json.dumps(o,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
write(A/'models/block/empty.json',{'parent':'minecraft:block/block'})
for i in ids:
 write(A/f'blockstates/{i}.json',{'variants':{'':{'model':'hbmdoors:block/empty'}}})
 write(A/f'models/item/{i}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'hbmdoors:'+tex[i]}})
write(A/'lang/en_us.json',{f'block.hbmdoors.{i}':n for i,n in zip(ids,en)})
write(A/'lang/pl_pl.json',{f'block.hbmdoors.{i}':n for i,n in zip(ids,pl)})
sounds={
'round_airlock.move':'block/doors/garage_move','round_airlock.stop':'block/doors/garage_stop','sliding_seal.move':'block/doors/doormove2','sliding_seal.stop':'block/doors/metal_stop1','wgh.move':'block/doors/door_wgh_start','wgh.stop':'block/doors/door_wgh_stop','wgh_big.move':'block/doors/door_wgh_big_start','wgh_big.stop':'block/doors/door_wgh_big_stop','alarm6':'block/doors/alarm6','qe.opening':'block/doors/doorslide_opening1','qe.opened':'block/doors/doorslide_opened1','qe.shut':'block/doors/doorshut_1','hatch.open':'block/doors/hatch_open1','door.lever':'block/doors/lever1','special.transition_seal':'block/doors/transition_seal_open','special.reactor_start':'block/reactorstart','special.reactor_stop':'block/reactorstop','special.vault_scrape':'block/vaultscrapenew','special.vault_thud':'block/vaultthudnew','special.silo_open':'block/doors/siloopen','special.silo_close':'block/doors/siloclose'}
write(A/'sounds.json',{k:{'sounds':[{'name':'hbmdoors:'+v,'stream':False}]} for k,v in sounds.items()})
recipes={
'round_airlock_door':(['IPI','BRB','IPI'],{'I':'minecraft:iron_block','P':'minecraft:piston','B':'minecraft:iron_bars','R':'minecraft:redstone'}),
'sliding_seal_door':(['III','PRP','III'],{'I':'minecraft:iron_ingot','P':'minecraft:piston','R':'minecraft:redstone'}),
'sliding_gate_door':(['IBI','PRP','IBI'],{'I':'minecraft:iron_ingot','B':'minecraft:iron_bars','P':'minecraft:piston','R':'minecraft:redstone'}),
'secure_access_door':(['IPI','IRI','IPI'],{'I':'minecraft:iron_block','P':'minecraft:piston','R':'minecraft:redstone'}),
'hatch':(['III','PRP','III'],{'I':'minecraft:iron_ingot','P':'minecraft:piston','R':'minecraft:redstone'}),
'fire_door':(['IRI','IPI','IRI'],{'I':'minecraft:iron_ingot','P':'minecraft:piston','R':'minecraft:redstone'}),
'qe_sliding_door':(['ICI','PRP','ICI'],{'I':'minecraft:iron_ingot','C':'minecraft:copper_ingot','P':'minecraft:piston','R':'minecraft:redstone'}),
'qe_containment_door':(['ICI','PRP','IBI'],{'I':'minecraft:iron_block','C':'minecraft:copper_block','P':'minecraft:piston','R':'minecraft:redstone','B':'minecraft:iron_bars'}),
'water_door':(['ICI','PRP','ICI'],{'I':'minecraft:iron_ingot','C':'minecraft:copper_ingot','P':'minecraft:piston','R':'minecraft:redstone'}),
'large_vehicle_door':(['IPI','BRB','IPI'],{'I':'minecraft:iron_block','P':'minecraft:piston','B':'minecraft:iron_bars','R':'minecraft:redstone'}),
'transition_seal':(['OIO','PRP','OIO'],{'O':'minecraft:obsidian','I':'minecraft:iron_block','P':'minecraft:piston','R':'minecraft:redstone_block'}),
'blast_door':(['III','OPO','IRI'],{'I':'minecraft:iron_block','O':'minecraft:obsidian','P':'minecraft:piston','R':'minecraft:redstone_block'}),
'sliding_blast_door':(['IGI','PRP','IGI'],{'I':'minecraft:iron_block','G':'minecraft:glass','P':'minecraft:piston','R':'minecraft:redstone_block'}),
'vault_door':(['ONO','PRP','ONO'],{'O':'minecraft:obsidian','N':'minecraft:netherite_ingot','P':'minecraft:piston','R':'minecraft:redstone_block'}),
'silo_hatch':(['IPI','SRS','IPI'],{'I':'minecraft:iron_block','P':'minecraft:piston','S':'minecraft:slime_block','R':'minecraft:redstone_block'})}
for i,(pat,key) in recipes.items():
 write(D/f'{i}.json',{'type':'minecraft:crafting_shaped','category':'redstone' if i in {'transition_seal','blast_door','sliding_blast_door','vault_door','silo_hatch'} else 'building','pattern':pat,'key':{k:{'item':v} for k,v in key.items()},'result':{'id':'hbmdoors:'+i,'count':1}})
print('generated resources for',len(ids),'doors')
