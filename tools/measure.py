# -*- coding: utf-8 -*-
"""Precise edge / color / reflection measurements."""
import numpy as np
from PIL import Image

SRC = r"D:\23178\DP-Launcher\_ref\reference.png"
im = Image.open(SRC).convert("RGB")
a = np.asarray(im).astype(np.int16)
H, W, _ = a.shape
BG = np.array([2, 0, 251], dtype=np.int16)


def hexs(v):
    return "#%02X%02X%02X" % tuple(int(x) for x in v)


def prof(label, fixed, lo, hi, axis):
    print("\n== %s (axis=%s @%d) ==" % (label, axis, fixed))
    prev = None
    for t in range(lo, hi):
        v = a[t, fixed] if axis == "y" else a[fixed, t]
        d = int(np.abs(v - BG).max())
        state = "BG" if d < 25 else "UI"
        if state != prev:
            print("   %s starts at %4d  %s  dist=%3d" % (state, t, hexs(v), d))
            prev = state
        elif t in (lo, hi - 1):
            print("   %s ...    at %4d  %s  dist=%3d" % (state, t, hexs(v), d))


def textbox(y0, y1, x0, x1, thr=140, label=""):
    sub = a[y0:y1, x0:x1]
    m = (sub.min(axis=2) > thr)
    ys, xs = np.where(m)
    if len(ys) == 0:
        print("   %s: no bright pixels" % label)
        return
    print("   %s text bbox x %d..%d (w=%d)  y %d..%d (h=%d)" % (
        label, x0 + xs.min(), x0 + xs.max(), xs.max() - xs.min() + 1,
        y0 + ys.min(), y0 + ys.max(), ys.max() - ys.min() + 1))


prof("card1 vertical through centre", 250, 140, 500, "y")
prof("gap between card1/card2 vertical", 379, 140, 460, "y")
prof("card1 horizontal mid", 285, 100, 1270, "x")
prof("bottom row horizontal mid", 560, 90, 1270, "x")
prof("keystone button vertical", 220, 495, 700, "y")
prof("settings focused vertical", 1060, 495, 700, "y")

print("\n== colours ==")
for name, (x, y) in {
    "wallpaper": (30, 400),
    "card1 netflix": (250, 200),
    "card2 youtube": (500, 200),
    "card3 play": (720, 180),
    "card4 chrome": (1030, 180),
    "card label white": (240, 388),
    "btn keystone fill": (160, 540),
    "btn keystone border(top)": (220, 522),
    "btn settings fill": (1000, 555),
    "btn settings border": (1060, 521),
    "status text": (1200, 90),
}.items():
    print("   %-26s %s" % (name, hexs(a[y, x])))

print("\n== text bounding boxes ==")
textbox(360, 420, 130, 370, label="NETFLIX")
textbox(360, 420, 390, 630, label="YouTube")
textbox(360, 420, 655, 890, label="GooglePlay")
textbox(360, 420, 910, 1150, label="chrome")
textbox(600, 660, 130, 310, label="Keystone")
textbox(600, 660, 965, 1155, label="Settings")
textbox(70, 140, 1050, 1270, label="time+date")
