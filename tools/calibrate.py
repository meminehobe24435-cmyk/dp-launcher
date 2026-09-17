# -*- coding: utf-8 -*-
"""Fit the launcher geometry against the reference screenshot.

The reference is a heavily re-encoded screenshot: edges are blurred over ~10 px, so reading a
single coordinate off it is unreliable. Instead this script renders the preview with candidate
values and keeps the ones that minimise the pixel difference with the reference inside the
region the parameter affects.

    python tools/calibrate.py stage1     # card width / gap
    python tools/calibrate.py stage2     # card row top / card height
    python tools/calibrate.py stage3     # dock / status bar
"""
import argparse
import itertools
import os
import subprocess
import sys
import tempfile

import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PREVIEW = os.path.join(ROOT, "preview", "index.html")
REFERENCE = os.path.join(ROOT, "_ref", "reference.png")
SCRATCH = os.path.join(ROOT, "preview", "out", "calibration")
CHROME_CANDIDATES = [
    r"C:\Program Files\Google\Chrome\Application\chrome.exe",
    r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
]

# Region the parameter influences, and the value it is compared in.
STAGES = {
    "stage1": {
        "params": {"cardW": [243, 245, 246, 248, 250], "cardGap": [12, 15, 17, 18.7, 21]},
        "region": (100, 150, 1180, 420),
    },
    "stage2": {
        "params": {"cardRowTop": [128, 131, 134, 137], "cardH": [290, 296, 302]},
        "region": (100, 150, 1180, 430),
    },
    "stage3": {
        "params": {"dockRowTop": [510, 513, 516, 519], "statusCenterY": [68, 71, 74]},
        "region": (100, 40, 1180, 670),
    },
}


def find_chrome():
    for candidate in CHROME_CANDIDATES:
        if os.path.exists(candidate):
            return candidate
    raise SystemExit("no Chrome/Edge binary found")


def render(query):
    os.makedirs(SCRATCH, exist_ok=True)
    handle, path = tempfile.mkstemp(suffix=".png", dir=SCRATCH)
    os.close(handle)
    url = "file:///" + PREVIEW.replace("\\", "/")
    if query:
        url += "?" + query
    profile = tempfile.mkdtemp(prefix="chrome-cal-")
    command = [
        find_chrome(),
        "--headless=new",
        "--disable-gpu",
        "--hide-scrollbars",
        "--force-device-scale-factor=1",
        "--window-size=1280,720",
        "--screenshot=" + path,
        "--user-data-dir=" + profile,
        "--virtual-time-budget=2000",
        url,
    ]
    subprocess.run(command, check=True, capture_output=True)
    with Image.open(path) as image:
        array = np.asarray(image.convert("RGB")).astype(np.int16).copy()
    os.remove(path)
    return array


def score(candidate, reference, region):
    x0, y0, x1, y1 = region
    difference = np.abs(candidate[y0:y1, x0:x1] - reference[y0:y1, x0:x1])
    return float(difference.mean())


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("stage", choices=sorted(STAGES))
    args = parser.parse_args()

    stage = STAGES[args.stage]
    reference = np.asarray(Image.open(REFERENCE).convert("RGB")).astype(np.int16)

    keys = list(stage["params"].keys())
    results = []
    for combination in itertools.product(*(stage["params"][key] for key in keys)):
        query = "&".join("%s=%s" % (key, value) for key, value in zip(keys, combination))
        candidate = render(query)
        value = score(candidate, reference, stage["region"])
        results.append((value, combination))
        print("  %-40s MAE=%6.2f" % (query, value))
        sys.stdout.flush()

    results.sort(key=lambda item: item[0])
    print("\nbest of %s:" % args.stage)
    for value, combination in results[:5]:
        print("  MAE=%6.2f  %s" % (value, dict(zip(keys, combination))))
    return 0


if __name__ == "__main__":
    sys.exit(main())
