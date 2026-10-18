"""Fill scripts/card-library/tribes-template.html: the tribes page.

Usage: python3 scripts/card-library/tribes.py CARDS_JSON TRIBES_JSON OUT_HTML
CARDS_JSON from CardLibraryReport, TRIBES_JSON from TribeReport
(scripts/tribes.sh N file). Card art is embedded as on the card library.
"""
import base64
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ART = 'src/main/resources/assets/royalarena/textures/gui/card'


def main():
    cards = json.load(open(sys.argv[1], encoding='utf-8'))
    tribes = json.load(open(sys.argv[2], encoding='utf-8'))
    slim = []
    for c in cards:
        path = os.path.join(ART, c['id'] + '.png')
        art = None
        if os.path.exists(path):
            art = 'data:image/png;base64,' + base64.b64encode(open(path, 'rb').read()).decode()
        slim.append({'id': c['id'], 'name': c['name'], 'cost': c['cost'], 'kind': c['kind'], 'art': art})
    head = subprocess.run(['git', 'rev-parse', '--short', 'HEAD'],
                          capture_output=True, text=True).stdout.strip()
    page = open(os.path.join(HERE, 'tribes-template.html'), encoding='utf-8').read()
    for key, value in {'{{DATA}}': json.dumps(tribes, ensure_ascii=False),
                       '{{CARDS}}': json.dumps(slim, ensure_ascii=False),
                       '{{MATCHES}}': str(tribes['matches']), '{{BRANCH_HASH}}': head}.items():
        page = page.replace(key, value)
    open(sys.argv[3], 'w', encoding='utf-8').write(page)
    print(f"{len(tribes['tribes'])} tribes -> {sys.argv[3]}")


main()
