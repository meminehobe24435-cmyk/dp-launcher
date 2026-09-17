# -*- coding: utf-8 -*-
"""Icon bounding boxes inside cards, clean background colours, caption strip colour."""
import numpy as np
from PIL import Image

SRC = r"D:\23178\DP-Launcher\_ref\reference.png"
im = Image.open(SRC).convert("RGB")
a = np.asarray(im).astype(np.int16)


def mean(x0, y0, x1, y1):
    m = a[y0:y1, x0:x1].reshape(-1, 3).mean(axis=0)
    return "#%02X%02X%02X" % tuple(int(round(v)) for v in m)


CARDS = {
    "netflix": (124, 370),
    "youtube": (390, 636),
    "play": (656, 902),
    "chrome": (922, 1168),
}

print("== clean card bg (band above icon) ==")
for k, (x0, x1) in CARDS.items():
    print("   %-8s %s" % (k, mean(x0 + 20, 150, x1 - 20, 175)))

print("\n== card bg band between icon and label ==")
for k, (x0, x1) in CARDS.items():
    print("   %-8s %s" % (k, mean(x0 + 20, 340, x1 - 20, 368)))

print("\n== icon bbox inside each card (y 140..372) ==")
for k, (x0, x1) in CARDS.items():
    sub = a[140:372, x0:x1].astype(np.float64)
    bg = np.median(sub[5:30].reshape(-1, 3), axis=0)
    d = np.abs(sub - bg).max(axis=2)
    m = d > 55
    ys, xs = np.where(m)
    if len(ys):
        print("   %-8s bg=%s icon x %d..%d (w=%d) y %d..%d (h=%d)  cx=%.1f cy=%.1f" % (
            k, "#%02X%02X%02X" % tuple(int(v) for v in bg),
            x0 + xs.min(), x0 + xs.max(), xs.max() - xs.min() + 1,
            140 + ys.min(), 140 + ys.max(), ys.max() - ys.min() + 1,
            x0 + (xs.min() + xs.max()) / 2.0, 140 + (ys.min() + ys.max()) / 2.0))

print("\n== caption strip of buttons: vertical colour profile x=150 ==")
for y in range(590, 650, 2):
    v = a[y, 150]
    print("   y=%3d #%02X%02X%02X" % (y, int(v[0]), int(v[1]), int(v[2])))

print("\n== button icon-area colour profile x=150 ==")
for y in range(514, 600, 4):
    v = a[y, 150]
    print("   y=%3d #%02X%02X%02X" % (y, int(v[0]), int(v[1]), int(v[2])))

print("\n== caption strip mean (keystone) ==")
print("   ", mean(140, 606, 175, 600 + 32))
print("\n== icon area mean (keystone) ==")
print("   ", mean(140, 530, 175, 590))

print("\n== focused settings: horizontal profile y=560 ==")
for x in range(940, 985, 2):
    v = a[560, x]
    print("   x=%3d #%02X%02X%02X" % (x, int(v[0]), int(v[1]), int(v[2])))
print("   ...")
for x in range(1130, 1190, 2):
    v = a[560, x]
    print("   x=%3d #%02X%02X%02X" % (x, int(v[0]), int(v[1]), int(v[2])))

print("\n== focused settings: horizontal profile y=505 (white ring) ==")
for x in range(950, 1180, 6):
    v = a[505, x]
    print("   x=%3d #%02X%02X%02X" % (x, int(v[0]), int(v[1]), int(v[2])))
