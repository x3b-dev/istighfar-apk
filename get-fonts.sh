#!/usr/bin/env bash
# Downloads the open-source (OFL) Arabic fonts into the app assets.
# Aref Ruqaa -> big Arabic phrases | Tajawal -> UI text
set -u
DIR="$(cd "$(dirname "$0")" && pwd)/app/src/main/assets/fonts"
BASE="https://raw.githubusercontent.com/google/fonts/main/ofl"
mkdir -p "$DIR"
get() {
  if [ -s "$DIR/$2" ]; then echo "ok (exists): $2"; return; fi
  if curl -fsSL "$BASE/$1/$2" -o "$DIR/$2.part"; then
    mv "$DIR/$2.part" "$DIR/$2"; echo "downloaded: $2"
  else
    echo "WARN: could not download $2 (a system font will be used)"
  fi
}
get arefruqaa ArefRuqaa-Bold.ttf
get arefruqaa ArefRuqaa-Regular.ttf
get tajawal Tajawal-Medium.ttf
get tajawal Tajawal-Bold.ttf
get tajawal Tajawal-ExtraBold.ttf
exit 0
