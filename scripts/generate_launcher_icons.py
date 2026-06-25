#!/usr/bin/env python3
"""Generate Android launcher icons from a single full-bleed Polli logo PNG."""

from __future__ import annotations

import math
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src" / "main" / "res"
BRANDING = ROOT / "assets" / "branding"
FULL = BRANDING / "icon-full.png"
FASTLANE_ICON = ROOT / "fastlane" / "metadata" / "android" / "en-US" / "images" / "icon.png"

# Source art has generous padding; scale up so home-screen weight matches the old launcher.
ICON_CONTENT_SCALE = 1.72

LAUNCHER_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def rgb_to_hex(rgb: tuple[int, int, int]) -> str:
    return f"#{rgb[0]:02x}{rgb[1]:02x}{rgb[2]:02x}"


def sample_background_color(image: Image.Image) -> tuple[int, int, int]:
    rgb = image.convert("RGB")
    w, h = rgb.size
    corners = [(2, 2), (w - 3, 2), (2, h - 3), (w - 3, h - 3)]
    pixels = [rgb.getpixel(point) for point in corners]
    return tuple(sum(channel) // len(pixels) for channel in zip(*pixels))


def compose_launcher_image(source: Image.Image, scale: float) -> Image.Image:
    rgba = source.convert("RGBA")
    width, height = rgba.size
    scaled_size = max(1, int(round(width * scale)))
    scaled = rgba.resize((scaled_size, scaled_size), Image.Resampling.LANCZOS)
    bg = sample_background_color(source)
    canvas = Image.new("RGBA", (width, height), (*bg, 255))
    offset = ((width - scaled_size) // 2, (height - scaled_size) // 2)
    canvas.paste(scaled, offset, scaled)
    return canvas.convert("RGB")


def save_resized(image: Image.Image, size: int, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    resized = image.resize((size, size), Image.Resampling.LANCZOS)
    resized.save(path, format="PNG", optimize=True)


def main() -> int:
    if not FULL.exists():
        print(f"Missing {FULL}", file=sys.stderr)
        return 1

    source = Image.open(FULL)
    icon = compose_launcher_image(source, ICON_CONTENT_SCALE)
    bg_hex = rgb_to_hex(sample_background_color(icon))
    print(f"Content scale: {ICON_CONTENT_SCALE}x, background: {bg_hex}")

    for folder, size in LAUNCHER_SIZES.items():
        out = RES / folder / "ic_launcher.png"
        save_resized(icon, size, out)
        print(f"  {out.relative_to(ROOT)} ({size}px)")

    FASTLANE_ICON.parent.mkdir(parents=True, exist_ok=True)
    save_resized(icon, 512, FASTLANE_ICON)
    print(f"  {FASTLANE_ICON.relative_to(ROOT)} (512px)")
    print("Done.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
