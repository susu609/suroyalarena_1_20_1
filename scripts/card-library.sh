#!/bin/sh
# Build the card library page (every card, its art, stats, win rate, and
# whether it is on master yet) into one self-contained HTML file.
# Usage: scripts/card-library.sh [matches=4000] [out=build/card-library/card-library.html]
# (also writes card-config.html next to it; BEFORE=old-cards.json shows the old numbers)
# Card art comes from textures/gui/card/<id>.png: add or replace a PNG there,
# run this again, and republish the page (docs/AUTOPILOT.md, "Card library").
set -e
cd "$(dirname "$0")/.."
N=${1:-4000}
OUT=${2:-build/card-library/card-library.html}
mkdir -p "$(dirname "$OUT")"
git fetch -q origin master 2>/dev/null || echo "warning: could not fetch master; using the last known origin/master"
[ -d build/pure ] || scripts/pure-test.sh >/dev/null
java -cp build/pure net.susu609.royalarena.CardLibraryReport "$N" build/card-library/cards.json
python3 scripts/card-library/build.py build/card-library/cards.json "$N" "$OUT"
# The card config page (edit stats, live cost, saved for Claude to apply).
python3 scripts/card-library/config.py build/card-library/cards.json "$N" "$(dirname "$OUT")/card-config.html" ${BEFORE:+"$BEFORE"}
# The tribes page (scripts/tribes.sh measures what each step is worth).
java -cp build/pure net.susu609.royalarena.TribeReport 400 build/card-library/tribes.json >/dev/null
python3 scripts/card-library/tribes.py build/card-library/cards.json build/card-library/tribes.json "$(dirname "$OUT")/tribes.html"
