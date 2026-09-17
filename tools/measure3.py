# -*- coding: utf-8 -*-
"""Final precise geometry: card rect/radius, button rect, status bar, reflection."""
import numpy as np
from PIL import Image

SRC = r"D:\23178\DP-Launcher\_ref\reference.png"
im = Image.open(SRC).convert("RGB")
a = np.asarray(im).astype(np.int16)
H, W, _ = a.shape
BG = np.array([2, 0, 251], dtype=np.int16)


def px(x, y):
    v = a[y, x]
    return "#%02X%02X%02X" % (int(v[0]), int(v[1]), int(v[2]))


print("== card1 top edge, x=250, y=100..170 ==")
for y in range(100, 171, 2):
    print("   y=%3d %s" % (y, px(250, y)))

print("\n== card1 bottom edge, x=250, y=415..440 ==")
for y in range(415, 441):
    print("   y=%3d %s" % (y, px(250, y)))

print("\n== keystone button bottom, x=220, y=625..655 ==")
for y in range(625, 656):
    print("   y=%3d %s" % (y, px(220, y)))

print("\n== card1 left edge, y=300, x=112..140 ==")
for x in range(112, 141):
    print("   x=%3d %s" % (x, px(x, 300)))

print("\n== card1 right edge, y=300, x=360..380 ==")
for x in range(360, 381):
    print("   x=%3d %s" % (x, px(x, 300)))


def near_black(x, y):
    v = a[y, x]
    return v.max() < 90


print("\n== corner radius sweep (card1 top-left corner) ==")
for y in range(120, 165, 3):
    row = [x for x in range(112, 200) if near_black(x, y)]
    print("   y=%3d first dark x=%s" % (y, row[0] if row else "-"))

print("\n== mirrored label bbox in reflection (card1) ==")
sub = a[431:500, 130:370]
m = (sub.min(axis=2) > 90)
ys, xs = np.where(m)
if len(ys):
    print("   x %d..%d  y %d..%d  (h=%d)" % (130 + xs.min(), 130 + xs.max(),
                                             431 + ys.min(), 431 + ys.max(),
                                             ys.max() - ys.min() + 1))
else:
    print("   none")

print("\n== reflection full extent card1 x=250 (dist>12) ==")
for y in range(425, 500):
    d = int(np.abs(a[y, 250] - BG).max())
    if d > 12:
        last = y
print("   last row with visible reflection:", last)

print("\n== reflection full extent card3 (green) x=720 ==")
last = None
for y in range(425, 510):
    d = int(np.abs(a[y, 720] - BG).max())
    if d > 12:
        last = y
print("   last row:", last)

print("\n== status bar: bright (near-white) pixels ==")
sub = a[45:110, 600:1275]
m = (sub.min(axis=2) > 170)
ys, xs = np.where(m)
if len(ys):
    print("   all: x %d..%d y %d..%d" % (600 + xs.min(), 600 + xs.max(), 45 + ys.min(), 45 + ys.max()))
colsum = m.sum(axis=0)
groups, run = [], None
for i, v in enumerate(colsum):
    if v > 0:
        run = [i, i] if run is None else [run[0], i]
    elif run and i - run[1] > 8:
        groups.append(run); run = None
if run:
    groups.append(run)
for g in groups:
    band = m[:, g[0]:g[1] + 1]
    rows = np.where(band.sum(axis=1) > 0)[0]
    print("   item x %4d..%4d (w=%3d)  y %3d..%3d (h=%2d)" % (
        600 + g[0], 600 + g[1], g[1] - g[0] + 1, 45 + rows[0], 45 + rows[-1], rows[-1] - rows[0] + 1))

print("\n== button label bboxes (white text under icons) ==")
for name, (x0, x1) in {"keystone": (130, 310), "miracast": (340, 520), "signal": (550, 730),
                       "myapps": (760, 940), "settings": (975, 1150)}.items():
    sub = a[595:660, x0:x1]
    m = (sub.min(axis=2) > 150)
    ys, xs = np.where(m)
    if len(ys):
        print("   %-9s x %d..%d (w=%d) y %d..%d (h=%d)" % (name, x0 + xs.min(), x0 + xs.max(),
                                                            xs.max() - xs.min() + 1,
                                                            595 + ys.min(), 595 + ys.max(),
                                                            ys.max() - ys.min() + 1))

print("\n== button icon bboxes (light strokes above label) ==")
for name, (x0, x1) in {"keystone": (130, 310), "miracast": (340, 520), "signal": (550, 730),
                       "myapps": (760, 940), "settings": (975, 1150)}.items():
    sub = a[530:600, x0:x1]
    m = (sub.min(axis=2) > 120)
    ys, xs = np.where(m)
    if len(ys):
        print("   %-9s x %d..%d (w=%d) y %d..%d (h=%d)" % (name, x0 + xs.min(), x0 + xs.max(),
                                                            xs.max() - xs.min() + 1,
                                                            530 + ys.min(), 530 + ys.max(),
                                                            ys.max() - ys.min() + 1))
