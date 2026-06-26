#!/usr/bin/env python3
"""Generate Android launcher icons from assets/branding/icon-full.png (no zoom or crop)."""

from __future__ import annotations

import math
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src" / "main" / "res"
BRANDING = ROOT / "assets" / "branding"
FULL = BRANDING / "icon-full.png"
FOREGROUND = BRANDING / "icon-foreground.png"
FASTLANE_ICON = ROOT / "fastlane" / "metadata" / "android" / "en-US" / "images" / "icon.png"

LAUNCHER_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

FOREGROUND_SIZES = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}


def rgb_to_hex(rgb: tuple[int, int, int]) -> str:
    return f"#{rgb[0]:02x}{rgb[1]:02x}{rgb[2]:02x}"


def sample_background_color(image: Image.Image) -> tuple[int, int, int]:
    rgb = image.convert("RGB")
    w, h = rgb.size
    corners = [(2, 2), (w - 3, 2), (2, h - 3), (w - 3, h - 3)]
    pixels = [rgb.getpixel(point) for point in corners]
    return tuple(sum(channel) // len(pixels) for channel in zip(*pixels))


def color_distance(a: tuple[int, int, int], b: tuple[int, int, int]) -> float:
    return math.sqrt(sum((x - y) ** 2 for x, y in zip(a, b)))


def foreground_is_usable(image: Image.Image) -> bool:
    if image.mode not in ("RGBA", "LA"):
        return False
    alpha = image.getchannel("A")
    extrema = alpha.getextrema()
    if extrema[0] >= 250:
        return False
    rgb = image.convert("RGB")
    pixels = list(rgb.getdata())
    if max(pixels, key=pixels.count) == (0, 0, 0) and extrema[1] < 20:
        return False
    return True


def extract_foreground(full: Image.Image, bg: tuple[int, int, int], threshold: float = 42.0) -> Image.Image:
    rgba = full.convert("RGBA")
    pixels = rgba.load()
    width, height = rgba.size
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if color_distance((r, g, b), bg) <= threshold:
                pixels[x, y] = (r, g, b, 0)
    return rgba


def load_foreground(full: Image.Image, bg: tuple[int, int, int]) -> Image.Image:
    if FOREGROUND.exists():
        candidate = Image.open(FOREGROUND)
        if foreground_is_usable(candidate):
            return candidate.convert("RGBA")
    return extract_foreground(full, bg)


def save_resized(image: Image.Image, size: int, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    resized = image.resize((size, size), Image.Resampling.LANCZOS)
    resized.save(path, format="PNG", optimize=True)


def write_background_color(hex_color: str) -> None:
    path = RES / "values" / "ic_launcher_background.xml"
    path.write_text(
        "\n".join(
            [
                '<?xml version="1.0" encoding="utf-8"?>',
                "<resources>",
                f'    <color name="ic_launcher_background">{hex_color}</color>',
                "</resources>",
                "",
            ]
        ),
        encoding="utf-8",
    )


def write_adaptive_icon_xml() -> None:
    path = RES / "mipmap-anydpi-v26" / "ic_launcher.xml"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        "\n".join(
            [
                '<?xml version="1.0" encoding="utf-8"?>',
                '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">',
                '    <background android:drawable="@color/ic_launcher_background" />',
                '    <foreground android:drawable="@mipmap/ic_launcher_foreground" />',
                '    <monochrome android:drawable="@mipmap/ic_launcher_foreground" />',
                "</adaptive-icon>",
                "",
            ]
        ),
        encoding="utf-8",
    )


def main() -> int:
    if not FULL.exists():
        print(f"Place a 1024×1024 PNG at {FULL}", file=sys.stderr)
        return 1

    source = Image.open(FULL).convert("RGB")
    width, height = source.size
    if width != height:
        print(f"Expected square icon, got {width}×{height}", file=sys.stderr)
        return 1

    bg = sample_background_color(source)
    bg_hex = rgb_to_hex(bg)
    foreground = load_foreground(source, bg)

    print(f"Source: {FULL.relative_to(ROOT)} ({width}×{height}, used as-is)")

    for folder, size in LAUNCHER_SIZES.items():
        out = RES / folder / "ic_launcher.png"
        save_resized(source, size, out)
        print(f"  {out.relative_to(ROOT)} ({size}px)")

    for folder, size in FOREGROUND_SIZES.items():
        out = RES / folder / "ic_launcher_foreground.png"
        save_resized(foreground, size, out)
        print(f"  {out.relative_to(ROOT)} ({size}px)")

    FASTLANE_ICON.parent.mkdir(parents=True, exist_ok=True)
    save_resized(source, 512, FASTLANE_ICON)
    print(f"  {FASTLANE_ICON.relative_to(ROOT)} (512px)")

    write_background_color(bg_hex)
    write_adaptive_icon_xml()
    print(f"  values/ic_launcher_background.xml -> {bg_hex}")
    print("Done.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
