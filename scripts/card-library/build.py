"""Fill scripts/card-library/template.html with the card data.

Usage: python3 scripts/card-library/build.py CARDS_JSON MATCHES OUT_HTML
(scripts/card-library.sh runs it). Adds to each card of CARDS_JSON (written by
CardLibraryReport) its art from textures/gui/card/<id>.png, embedded so the
page is one file, and whether it is on origin/master yet; reads the open
queue from docs/CARD_QUEUE.md (notes from queue-vi.json when present).
"""
import base64
import json
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ART = 'src/main/resources/assets/royalarena/textures/gui/card'
CARD_JSON = 'src/main/resources/data/royalarena/cards'


def git(*args):
    return subprocess.run(['git', *args], capture_output=True, text=True).stdout.strip()


def master_ids():
    names = git('ls-tree', '--name-only', 'origin/master', CARD_JSON + '/').split('\n')
    return {os.path.basename(n)[:-5] for n in names if n.endswith('.json')}


def art(card_id):
    path = os.path.join(ART, card_id + '.png')
    if not os.path.exists(path):
        return None
    with open(path, 'rb') as f:
        return 'data:image/png;base64,' + base64.b64encode(f.read()).decode()


def queue():
    with open(os.path.join(HERE, 'queue-vi.json'), encoding='utf-8') as f:
        notes = json.load(f)
    out = []
    for line in open('docs/CARD_QUEUE.md', encoding='utf-8'):
        m = re.match(r'- \[ \] (.+?) \(', line)
        if not m:
            continue
        name = m.group(1)
        note = notes.get(name)
        if note is None:
            rest = line.split('):', 1)[-1].strip()
            note = ('Làm tay. ' if '(by hand)' in line else '') + rest
        out.append({'name': name, 'note': note})
    return out


def main():
    cards_json, matches, out = sys.argv[1], sys.argv[2], sys.argv[3]
    with open(cards_json, encoding='utf-8') as f:
        cards = json.load(f)
    on_master = master_ids()
    for c in cards:
        c['master'] = c['id'] in on_master
        c['art'] = art(c['id'])
    with open(os.path.join(HERE, 'template.html'), encoding='utf-8') as f:
        page = f.read()
    for key, value in {
        '{{CARDS}}': json.dumps(cards, ensure_ascii=False),
        '{{QUEUE}}': json.dumps(queue(), ensure_ascii=False),
        '{{MATCHES}}': matches,
        '{{MASTER_HASH}}': git('rev-parse', '--short', 'origin/master'),
        '{{BRANCH_HASH}}': git('rev-parse', '--short', 'HEAD'),
    }.items():
        page = page.replace(key, value)
    with open(out, 'w', encoding='utf-8') as f:
        f.write(page)
    with_art = sum(1 for c in cards if c['art'])
    new = [c['name'] for c in cards if not c['master']]
    print(f'{len(cards)} cards ({with_art} with art), {len(new)} not on master -> {out}')


main()
