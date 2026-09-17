# -*- coding: utf-8 -*-
"""Measure the same edges on the reference screenshot and on the rendered preview.

Both images are analysed with identical detection code, so any systematic blur bias cancels out
and the printed deltas are the real geometry differences to fix.
"""
import os
import sys

import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REFERENCE = os.path.join(ROOT, "_ref", "reference.png")
RENDER = os.path.join(ROOT, "preview", "out", "render.png")
BG = np.array([2, 0, 251], dtype=np.float64)


def load(path):
    image = Image.open(path).convert("RGB")
    return np.asarray(image).astype(np.float64)


def edge_along(array, fixed, start, end, axis, background=BG):
    """Returns the coordinate where the pixel crosses half way between background and UI."""
    values = []
    for t in range(start, end):
        pixel = array[t, fixed] if axis == "y" else array[fixed, t]
        values.append((t, float(np.abs(pixel - background).max())))
    peak = max(v for _, v in values)
    if peak < 40:
        return None
    half = peak / 2.0
    for index in range(1, len(values)):
        previous, current = values[index - 1][1], values[index][1]
        if (previous < half <= current) or (previous >= half > current):
            t0, t1 = values[index - 1][0], values[index][0]
            span = current - previous
            if span == 0:
                return float(t0)
            return t0 + (half - previous) / span * (t1 - t0)
    return None


def text_box(array, x0, y0, x1, y1, threshold=150):
    region = array[y0:y1, x0:x1]
    mask = region.min(axis=2) > threshold
    ys, xs = np.where(mask)
    if len(ys) == 0:
        return None
    return (x0 + int(xs.min()), x0 + int(xs.max()), y0 + int(ys.min()), y0 + int(ys.max()))


def last_visible_row(array, column, start, end, tolerance=12):
    result = None
    for y in range(start, end):
        if float(np.abs(array[y, column] - BG).max()) > tolerance:
            result = y
    return result


def measure(array, label):
    data = {}
    data["card1.left"] = edge_along(array, 300, 100, 200, "x")
    data["card1.right"] = edge_along(array, 300, 340, 400, "x")
    data["card2.left"] = edge_along(array, 300, 375, 430, "x")
    data["card4.right"] = edge_along(array, 300, 1130, 1200, "x")
    data["card1.bottom"] = edge_along(array, 250, 415, 445, "y")
    data["dockitem1.left"] = edge_along(array, 560, 100, 160, "x")
    data["dockitem1.right"] = edge_along(array, 560, 300, 345, "x")
    data["dockitem1.top"] = edge_along(array, 220, 500, 535, "y")
    data["dockitem1.bottom"] = edge_along(array, 220, 620, 660, "y")
    data["dockitem5.left"] = edge_along(array, 620, 925, 990, "x")
    data["dockitem5.right"] = edge_along(array, 620, 1130, 1200, "x")
    data["reflect.end.card1"] = last_visible_row(array, 250, 440, 520, 12)
    data["reflect.end.card3"] = last_visible_row(array, 720, 440, 520, 12)
    data["text.NETFLIX"] = text_box(array, 130, 360, 370, 420)
    data["text.Keystone"] = text_box(array, 130, 600, 310, 645)
    data["text.Settings"] = text_box(array, 975, 600, 1150, 650)
    data["text.status_time"] = text_box(array, 760, 55, 880, 95)
    data["text.status_date"] = text_box(array, 880, 55, 1180, 95)
    return data


def main():
    reference = measure(load(REFERENCE), "reference")
    render = measure(load(RENDER), "render")

    print("%-20s %-26s %-26s %s" % ("metric", "reference", "render", "delta"))
    print("-" * 96)
    for key in reference:
        left = reference[key]
        right = render[key]
        if isinstance(left, tuple) and isinstance(right, tuple):
            delta = tuple(r - l for l, r in zip(left, right))
            print("%-20s %-26s %-26s %s" % (key, left, right, delta))
        elif isinstance(left, float) and isinstance(right, float):
            print("%-20s %-26.1f %-26.1f %+.1f" % (key, left, right, right - left))
        else:
            print("%-20s %-26s %-26s" % (key, left, right))
    return 0


if __name__ == "__main__":
    sys.exit(main())
