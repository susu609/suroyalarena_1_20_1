#!/bin/sh
# Assembles the stats site into a folder (default: _site): the page, its
# data and the card portraits. .github/workflows/stats-site.yml deploys that
# folder with GitHub Pages; it runs the same way by hand.
set -eu
out="${1:-_site}"
case "$out" in
    ""|"/"|".") echo "refusing to write the site to '$out'" >&2; exit 1 ;;
esac
here="$(cd "$(dirname "$0")" && pwd)"
repo="$(cd "$here/../.." && pwd)"
cards="$repo/src/main/resources/assets/royalarena/textures/gui/card"

rm -rf "$out"
mkdir -p "$out/data" "$out/cards"
sed 's|<html lang="vi">|<html lang="vi" data-portraits="cards/">|' \
    "$here/index.html" > "$out/index.html"
for f in stats.js stats.json history.jsonl; do
    if [ -f "$here/data/$f" ]; then cp "$here/data/$f" "$out/data/"; fi
done
for f in "$cards"/*.png; do
    if [ -f "$f" ]; then cp "$f" "$out/cards/"; fi
done
# Serve the files as they are (no Jekyll processing on GitHub Pages).
touch "$out/.nojekyll"
echo "Stats site written to $out"
