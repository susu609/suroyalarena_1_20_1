#!/bin/sh
# What each tribe step is worth (docs/TRIBES.md): decks around 4 and 6 cards
# of a tribe against plain bot decks, with and without the tribe bonuses.
# Usage: scripts/tribes.sh [matches per row=400] [json out, optional]
set -e
cd "$(dirname "$0")/.."
[ -d build/pure ] || scripts/pure-test.sh >/dev/null
java -cp build/pure net.susu609.royalarena.TribeReport "${1:-400}" ${2:+"$2"} 2>/dev/null
