#!/usr/bin/env python3
"""Generate Android launcher icons from Polli branding assets."""

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


def foreground_has_alpha(image: Image.Image) -> bool:
    if image.mode not in ("RGBA", "LA"):
        return False
    lo, hi = image.getchannel("A").getextrema()
    return lo < 250 and hi > 0


def key_out_background(
    image: Image.Image,
    bg: tuple[int, int, int],
    threshold: float = 40.0,
) -> Image.Image:
    rgba = image.convert("RGBA")
    pixels = rgba.load()
    width, height = rgba.size
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if color_distance((r, g, b), bg) <= threshold:
                pixels[x, y] = (r, g, b, 0)
    return rgba


def has_visible_content(image: Image.Image) -> bool:
    alpha = image.convert("RGBA").getchannel("A")
    return alpha.getextrema()[1] > 10


def extract_foreground(full: Image.Image, bg: tuple[int, int, int], threshold: float = 42.0) -> Image.Image:
    return key_out_background(full, bg, threshold)


def load_foreground(full: Image.Image, bg: tuple[int, int, int]) -> Image.Image:
    if FOREGROUND.exists():
        candidate = Image.open(FOREGROUND)
        if foreground_has_alpha(candidate):
            print(f"Using transparent foreground asset: {FOREGROUND}")
            return candidate.convert("RGBA")

        fg_bg = sample_background_color(candidate)
        keyed = key_out_background(candidate, fg_bg)
        if has_visible_content(keyed):
            print(f"Using foreground asset (removed {rgb_to_hex(fg_bg)} background): {FOREGROUND}")
            return keyed

        print(
            f"Warning: {FOREGROUND} could not be converted to a foreground layer; "
            "extracting from icon-full.png instead.",
            file=sys.stderr,
        )

    print("Extracting foreground layer from icon-full.png")
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


def main() -> int:
    if not FULL.exists():
        print(f"Missing {FULL}", file=sys.stderr)
        print("Add a 1024x1024 PNG with background at that path.", file=sys.stderr)
        return 1

    full = Image.open(FULL).convert("RGB")
    bg = sample_background_color(full)
    bg_hex = rgb_to_hex(bg)
    foreground = load_foreground(full, bg)

    print(f"Background color: {bg_hex}")

    for folder, size in LAUNCHER_SIZES.items():
        out = RES / folder / "ic_launcher.png"
        save_resized(full, size, out)
        print(f"  {out.relative_to(ROOT)} ({size}px)")

    for folder, size in FOREGROUND_SIZES.items():
        out = RES / folder / "ic_launcher_foreground.png"
        save_resized(foreground, size, out)
        print(f"  {out.relative_to(ROOT)} ({size}px)")

    FASTLANE_ICON.parent.mkdir(parents=True, exist_ok=True)
    save_resized(full, 512, FASTLANE_ICON)
    print(f"  {FASTLANE_ICON.relative_to(ROOT)} (512px)")

    write_background_color(bg_hex)
    print(f"  values/ic_launcher_background.xml -> {bg_hex}")
    print("Done.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
