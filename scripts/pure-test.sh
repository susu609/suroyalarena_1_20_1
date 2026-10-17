#!/bin/sh
# Compile the pure-Java sources (no Minecraft/Forge imports) and the tests,
# then run DomainSmokeTest. Works where gradlew cannot reach Forge's Maven.
# Usage: scripts/pure-test.sh            (classes land in build/pure)
set -e
cd "$(dirname "$0")/.."
OUT=build/pure
rm -rf "$OUT" && mkdir -p "$OUT"
FILES=$(grep -L -E "import (net\.minecraft|net\.minecraftforge|com\.mojang|org\.joml|net\.susu609\.modlib|org\.spongepowered)" $(find src/main/java -name "*.java") \
    | grep -v -E "/(config|network|server|client/(screen|render|state|input|model)|mixin|event|entity/ArenaEntityTypes|command)/|RoyalArena.java")
javac -nowarn -d "$OUT" $FILES $(find src/test/java -name "*.java")
java -cp "$OUT" net.susu609.royalarena.DomainSmokeTest
