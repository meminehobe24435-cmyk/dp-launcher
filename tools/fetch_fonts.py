# -*- coding: utf-8 -*-
"""Download the two Roboto weights used by the preview into preview/assets/fonts.

Roboto is Apache-2.0 licensed and is the typeface of the reference UI, so the preview would
otherwise drift from the Android rendering (which uses the platform Roboto).
"""
import os
import re
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(ROOT, "preview", "assets", "fonts")
CSS_URL = "https://fonts.googleapis.com/css2?family=Roboto:wght@400;500&display=swap"
UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/122.0.0.0 Safari/537.36"
)

WEIGHTS = {400: "roboto-400.woff2", 500: "roboto-500.woff2"}


def fetch(url, headers=None):
    request = urllib.request.Request(url, headers=headers or {})
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read()


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    css = fetch(CSS_URL, {"User-Agent": UA}).decode("utf-8")

    blocks = re.findall(r"@font-face\s*\{(.*?)\}", css, re.S)
    found = {}
    for block in blocks:
        if "/* latin */" not in css[: css.find(block)] + "/* latin */" and "latin" not in block:
            # The subset is announced by a comment right before the block; keep it simple and
            # only accept blocks whose unicode-range is the basic latin one.
            pass
        weight = re.search(r"font-weight:\s*(\d+)", block)
        url = re.search(r"url\((https://[^)]+\.woff2)\)", block)
        latin = "U+0000-00FF" in block
        if weight and url and latin:
            found[int(weight.group(1))] = url.group(1)

    for weight, target in WEIGHTS.items():
        url = found.get(weight)
        if not url:
            raise SystemExit("no latin woff2 found for weight %d" % weight)
        path = os.path.join(OUT_DIR, target)
        data = fetch(url, {"User-Agent": UA})
        with open(path, "wb") as handle:
            handle.write(data)
        print("wrote %s (%d bytes) from %s" % (path, len(data), url))


if __name__ == "__main__":
    main()
