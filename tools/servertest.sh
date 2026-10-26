#!/bin/bash
# Release-jar check on real dedicated servers (what players install), Minecraft 1.21.1:
#   tools/servertest.sh [neoforge] [fabric]
# Installs the server once into run/server-<loader>/ (NeoForge installer / Fabric launcher + Fabric API + Forge Config
# API Port), copies the built jar, starts the server and sends console commands: the mod's commands work, a zombie
# summoned at difficulty 0 keeps vanilla health, one at difficulty 250 has more health, a forced blight is a blight,
# the server difficulty can be set and read. Then checks the log for errors and stops the server.
# eula=true: the owner accepted the Minecraft EULA for local test servers.
cd "$(dirname "$0")/.."
LOADERS=${*:-"neoforge fabric"}
MC=1.21.1
NEOFORGE=21.1.201
FABRIC_LOADER=0.16.14
JAVA=${JAVA:-/opt/homebrew/opt/openjdk@21/bin/java}
FAILS=0
# Windows (Git Bash): "python3" is the Microsoft Store stub, the NeoForge server starts from win_args.txt, no pkill
PY=python3; python3 -c '' 2>/dev/null || PY=python
ARGS=unix_args.txt; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) ARGS=win_args.txt ;; esac
command -v pkill > /dev/null || pkill() { :; }

modrinth_file() { # <project> <loader>: URL of the newest file for MC 1.21.1
    curl -fsSL "https://api.modrinth.com/v2/project/$1/version?game_versions=%5B%22$MC%22%5D&loaders=%5B%22$2%22%5D" \
        | "$PY" -c 'import json,sys; print(json.load(sys.stdin)[0]["files"][0]["url"])'
}

for loader in $LOADERS; do
    dir=run/server-$loader
    mkdir -p "$dir/mods"
    case $loader in
        neoforge)
            if [ ! -f "$dir/libraries/net/neoforged/neoforge/$NEOFORGE/$ARGS" ]; then
                curl -fsSL -o "$dir/neoforge-installer.jar" "https://maven.neoforged.net/releases/net/neoforged/neoforge/$NEOFORGE/neoforge-$NEOFORGE-installer.jar"
                (cd "$dir" && "$JAVA" -jar neoforge-installer.jar --installServer > installer.log 2>&1)
            fi
            jar=$(ls neoforge/build/libs/scalinghealth-neoforge-*.jar | head -1)
            start=("$JAVA" -Xmx3G @libraries/net/neoforged/neoforge/$NEOFORGE/$ARGS nogui) ;;
        fabric)
            if [ ! -f "$dir/fabric-server-launch.jar" ]; then
                curl -fsSL -o "$dir/fabric-server-launch.jar" "https://meta.fabricmc.net/v2/versions/loader/$MC/$FABRIC_LOADER/1.0.1/server/jar"
                curl -fsSL -o "$dir/mods/fabric-api.jar" "$(modrinth_file fabric-api fabric)"
                curl -fsSL -o "$dir/mods/forgeconfigapiport.jar" "$(modrinth_file forge-config-api-port fabric)"
            fi
            jar=$(ls fabric/build/libs/scalinghealth-fabric-*.jar | head -1)
            start=("$JAVA" -Xmx3G -jar fabric-server-launch.jar nogui) ;;
    esac
    rm -f "$dir"/mods/scalinghealth-*.jar
    cp "$jar" "$dir/mods/"
    rm -rf "$dir/world"
    echo "eula=true" > "$dir/eula.txt"
    printf 'online-mode=false\nserver-port=25599\nlevel-type=minecraft\\:flat\nmax-tick-time=-1\ndifficulty=hard\n' > "$dir/server.properties"
    : > "$dir/console.in"; : > "$dir/console.log"
    (cd "$dir" && tail -f console.in | "${start[@]}" > console.log 2>&1) &
    pid=$!; disown
    for i in $(seq 1 300); do grep -q 'Done (' "$dir/console.log" && break; sleep 1; done
    if ! grep -q 'Done (' "$dir/console.log"; then echo "$loader: server did not start"; FAILS=$((FAILS+1)); kill $pid; pkill -f "tail -f console.in"; continue; fi
    cmd() { echo "$1" >> "$dir/console.in"; sleep "${2:-1}"; }
    health() { # summon at <difficulty> <blight>, print the zombie's health, remove it
        cmd "kill @e[type=minecraft:zombie]"
        cmd "sh_summon minecraft:zombie $1 $2" 2
        cmd "data get entity @e[type=minecraft:zombie,limit=1] Health"
        grep -o 'has the following entity data: [0-9.]*f' "$dir/console.log" | tail -1 | grep -o '[0-9.]*f$' | tr -d f
    }
    cmd "forceload add 0 0" 2
    cmd "time set midnight"
    cmd "gamerule doDaylightCycle false"
    h0=$(health 0 false)
    h250=$(health 250 false)
    cmd "kill @e[type=minecraft:zombie]"
    cmd "sh_summon minecraft:zombie 100 true" 2
    cmd "data get entity @e[type=minecraft:zombie,limit=1] ForgeCaps" 2
    blight=$(grep -c 'scalinghealth.*[Bb]light' "$dir/console.log")
    cmd "sh_difficulty set server 42"
    cmd "sh_difficulty get server" 2
    serverdiff=$(grep -cE '42(\.0)?' "$dir/console.log")
    cmd "stop" 10
    for i in $(seq 1 60); do kill -0 $pid 2>/dev/null || break; sleep 1; done
    kill $pid 2>/dev/null; pkill -f "tail -f console.in" 2>/dev/null; wait $pid 2>/dev/null
    errors=$(grep -E '/ERROR\]|ERROR\]|Exception|Caused by' "$dir/console.log" | grep -v 'No key layers' | grep -vc 'nightconfig/core/io/IoUtils')  # vanilla flat preset / Forge Config API Port class probe
    ok=1
    [[ "$h0" == "20.0" ]] || ok=0
    awk "BEGIN{exit !(${h250:-0} > 20)}" || ok=0
    [ "$blight" -gt 0 ] || ok=0
    [ "$serverdiff" -gt 0 ] || ok=0
    [ "$errors" = 0 ] || ok=0
    echo "$loader: zombie health at difficulty 0 = ${h0:-?}, at 250 = ${h250:-?}, blight data found: $blight, server difficulty set/read: $serverdiff, errors: $errors -> $([ $ok = 1 ] && echo PASS || echo FAIL)"
    [ $ok = 1 ] || FAILS=$((FAILS+1))
done
echo; [ $FAILS = 0 ] && echo "ALL PASS" || { echo "$FAILS FAILED"; exit 1; }
