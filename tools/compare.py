# -*- coding: utf-8 -*-
"""Render the HTML preview with headless Chrome and compare it with the reference screenshot.

Usage:
    python tools/compare.py [--state reference|drawer]

Outputs (in preview/out/):
    render.png      - the preview rendered at exactly 1280x720
    side_by_side.png
    diff.png        - per-pixel difference heat map
and prints a similarity report for the whole screen and for the measured regions.
"""
import argparse
import os
import subprocess
import sys
import tempfile

import numpy as np
from PIL import Image, ImageChops, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PREVIEW = os.path.join(ROOT, "preview", "index.html")
REFERENCE = os.path.join(ROOT, "_ref", "reference.png")
OUT_DIR = os.path.join(ROOT, "preview", "out")
CHROME_CANDIDATES = [
    r"C:\Program Files\Google\Chrome\Application\chrome.exe",
    r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    r"C:\Program Files\Microsoft\Edge\Application\msedge.exe",
]

# Regions of interest measured from the reference design, in reference pixels.
REGIONS = {
    "status bar": (560, 40, 1280, 120),
    "card row": (110, 130, 1175, 480),
    "dock row": (100, 495, 1180, 670),
    "wallpaper": (0, 600, 120, 700),
}

# The reference screenshot carries the reviewer's annotation overlay (the red note and its
# purple frame) over the top of the screen. Everything below that line is product UI, so the
# "clean" score is the number that actually measures the replica.
ANNOTATION_BOTTOM = 146


def find_chrome():
    for candidate in CHROME_CANDIDATES:
        if os.path.exists(candidate):
            return candidate
    raise SystemExit("no Chrome/Edge binary found; set one in CHROME_CANDIDATES")


def render(state, width=1280, height=720):
    url = "file:///" + PREVIEW.replace("\\", "/")
    if state != "reference":
        url += "?state=" + state
    profile = tempfile.mkdtemp(prefix="chrome-preview-")
    output = os.path.join(OUT_DIR, "render.png")
    command = [
        find_chrome(),
        "--headless=new",
        "--disable-gpu",
        "--hide-scrollbars",
        "--force-device-scale-factor=1",
        "--window-size=%d,%d" % (width, height),
        "--screenshot=" + output,
        "--user-data-dir=" + profile,
        "--virtual-time-budget=2500",
        url,
    ]
    subprocess.run(command, check=True, capture_output=True)
    with Image.open(output) as image:
        return image.convert("RGB").copy()


def similarity(a, b):
    """Returns (mean absolute error, percentage of pixels that are visually identical)."""
    diff = np.abs(a.astype(np.int16) - b.astype(np.int16))
    mae = float(diff.mean())
    close = (diff.max(axis=2) <= 24).mean() * 100.0
    return mae, float(close)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--state", default="reference")
    args = parser.parse_args()

    os.makedirs(OUT_DIR, exist_ok=True)
    reference = Image.open(REFERENCE).convert("RGB")
    render_image = render(args.state)

    if render_image.size != reference.size:
        render_image = render_image.resize(reference.size, Image.LANCZOS)

    reference_array = np.asarray(reference)
    render_array = np.asarray(render_image)

    mae, close = similarity(render_array, reference_array)
    print("overall  MAE=%5.2f  pixels within tolerance=%5.1f%%" % (mae, close))

    clean_reference = reference_array[ANNOTATION_BOTTOM:, :]
    clean_render = render_array[ANNOTATION_BOTTOM:, :]
    clean_mae, clean_close = similarity(clean_render, clean_reference)
    print(
        "clean (below the reviewer's annotation, y>=%d)  MAE=%5.2f  within tolerance=%5.1f%%"
        % (ANNOTATION_BOTTOM, clean_mae, clean_close)
    )

    for name, (x0, y0, x1, y1) in REGIONS.items():
        region_reference = reference_array[y0:y1, x0:x1]
        region_render = render_array[y0:y1, x0:x1]
        region_mae, region_close = similarity(region_render, region_reference)
        print("  %-12s MAE=%5.2f  within tolerance=%5.1f%%" % (name, region_mae, region_close))

    side_by_side = Image.new("RGB", (reference.width * 2 + 20, reference.height), (20, 20, 20))
    side_by_side.paste(reference, (0, 0))
    side_by_side.paste(render_image, (reference.width + 20, 0))
    side_by_side.save(os.path.join(OUT_DIR, "side_by_side.png"))

    diff_image = ImageChops.difference(reference, render_image)
    amplified = diff_image.point(lambda value: min(255, value * 4))
    draw = ImageDraw.Draw(amplified)
    for name, (x0, y0, x1, y1) in REGIONS.items():
        draw.rectangle([x0, y0, x1 - 1, y1 - 1], outline=(0, 255, 0))
    amplified.save(os.path.join(OUT_DIR, "diff.png"))
    print("wrote %s, side_by_side.png, diff.png" % os.path.join(OUT_DIR, "render.png"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
