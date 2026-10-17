#!/bin/sh
# Bot-versus-bot balance report from the classes scripts/pure-test.sh built.
# Usage: scripts/balance.sh [matches=4000] [out=build/balance.md]
# 4000 matches take about 40 s and give each card about +/-2%.
set -e
cd "$(dirname "$0")/.."
N=${1:-4000}
OUT=${2:-build/balance.md}
[ -d build/pure ] || scripts/pure-test.sh >/dev/null
java -cp build/pure net.susu609.royalarena.BalanceReport "$N" "$OUT" >/dev/null 2>&1
grep "^|" "$OUT" | sed -n '3,$p' | awk -F'|' '{printf "%-18s %4s el %5s %s\n", $2, $3, $5, $8}'
tail -1 "$OUT"
