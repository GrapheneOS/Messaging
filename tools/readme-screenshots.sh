#!/usr/bin/env bash
# Renders the README screenshots from app/src/screenshotTest (Layoutlib, no device) and joins them
# into docs/screenshots.webp: the screens side by side with rounded corners on a coloured backdrop,
# so the light screens stand out on a light page. Needs ImageMagick 7 with WebP support.
set -euo pipefail
cd "$(dirname "$0")/.."

reference=app/src/screenshotTestDebug/reference
shots=$reference/com/android/messaging/screenshots/ReadmeScreenshotsKt
out=docs/screenshots.webp
radius=64
# Around and between the screens, at the rendered size; the joined image is then halved.
padding=72
# A deep navy that matches the screens' blue, so they stand out sharply.
backdrop='#0a305f'

rm -rf "$reference"
./gradlew :app:updateDebugScreenshotTest --rerun

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

screens=()
for screen in ConversationList Chat GroupChat NewChat; do
    shot=("$shots/${screen}"_*.png)
    read -r w h < <(magick identify -format '%w %h\n' "${shot[0]}")
    magick "${shot[0]}" \
        \( -size "${w}x${h}" xc:black -fill white \
            -draw "roundrectangle 0,0 $((w - 1)),$((h - 1)) $radius,$radius" -alpha off \) \
        -compose CopyOpacity -composite "$work/$screen.png"
    if ((${#screens[@]} > 0)); then
        screens+=(-size "${padding}x${h}" xc:none)
    fi
    screens+=("$work/$screen.png")
done

mkdir -p "$(dirname "$out")"
magick -background "$backdrop" "${screens[@]}" +append -alpha remove -alpha off \
    -bordercolor "$backdrop" -border "$padding" -resize 50% \
    -quality 90 -define webp:method=6 -define webp:use-sharp-yuv=true -strip "$out"
