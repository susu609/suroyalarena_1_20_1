"""Fill scripts/card-library/config-template.html: the card config page.

Usage: python3 scripts/card-library/config.py CARDS_JSON MATCHES OUT_HTML [BEFORE_JSON]
CARDS_JSON comes from CardLibraryReport; BEFORE_JSON (optional, same format)
shows each card's numbers before a change. Vanilla health, armour and the
family of each mob come from vanilla.json.
"""
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))


def main():
    cards_json, matches, out = sys.argv[1], sys.argv[2], sys.argv[3]
    cards = json.load(open(cards_json, encoding='utf-8'))
    before = {}
    if len(sys.argv) > 4:
        before = {c['id']: c for c in json.load(open(sys.argv[4], encoding='utf-8'))}
    vanilla = json.load(open(os.path.join(HERE, 'vanilla.json'), encoding='utf-8'))
    troops, spells = [], []
    for c in cards:
        if c['kind'] == 'spell':
            spells.append({k: c[k] for k in ('id', 'name', 'cost', 'raw', 'facts', 'win')})
            continue
        v = vanilla[c['id']]
        t = {k: c[k] for k in ('id', 'name', 'cost', 'count', 'hp', 'armor', 'dmg', 'every',
                               'range', 'speed', 'win', 'traitMod', 'generations', 'riderHp',
                               'poisonPerSecond', 'poisonTicks', 'selfDestruct')}
        t.update(family=v['family'], vanillaHp=v['hp'],
                 vanillaArmor=v['armor'] + v.get('worn', 0),
                 keep=v.get('keep', False), why=v.get('why', ''))
        b = before.get(c['id'])
        if b and any(b[k] != c[k] for k in ('count', 'hp', 'dmg', 'cost')):
            t['before'] = {k: b[k] for k in ('count', 'hp', 'dmg', 'cost')}
        troops.append(t)
    head = subprocess.run(['git', 'rev-parse', '--short', 'HEAD'],
                          capture_output=True, text=True).stdout.strip()
    page = open(os.path.join(HERE, 'config-template.html'), encoding='utf-8').read()
    for key, value in {'{{DATA}}': json.dumps(troops, ensure_ascii=False),
                       '{{SPELLS}}': json.dumps(spells, ensure_ascii=False),
                       '{{MATCHES}}': matches, '{{BRANCH_HASH}}': head}.items():
        page = page.replace(key, value)
    open(out, 'w', encoding='utf-8').write(page)
    print(f'{len(troops)} troops, {len(spells)} spells -> {out}')


main()
