# -*- coding: utf-8 -*-
"""Icon bbox per card with colour-specific masks + button icon colours."""
import numpy as np
from PIL import Image

SRC = r"D:\23178\DP-Launcher\_ref\reference.png"
im = Image.open(SRC).convert("RGB")
a = np.asarray(im).astype(np.int16)


def bbox(mask, x0, y0, label):
    ys, xs = np.where(mask)
    if not len(ys):
        print("   %-16s none" % label)
        return
    print("   %-16s x %d..%d (w=%d)  y %d..%d (h=%d)  cx=%.1f cy=%.1f" % (
        label, x0 + xs.min(), x0 + xs.max(), xs.max() - xs.min() + 1,
        y0 + ys.min(), y0 + ys.max(), ys.max() - ys.min() + 1,
        x0 + (xs.min() + xs.max()) / 2.0, y0 + (ys.min() + ys.max()) / 2.0))


# --- netflix: red logo on black ---
sub = a[150:370, 134:360]
bbox((sub[:, :, 0] > 110) & (sub[:, :, 1] < 80) & (sub[:, :, 2] < 90), 134, 150, "netflix-rect")

# --- youtube: red rounded rect (and white triangle inside) ---
sub = a[150:370, 400:626]
bbox((sub[:, :, 0] > 150) & (sub[:, :, 1] < 90) & (sub[:, :, 2] < 90), 400, 150, "youtube-red")
bbox(sub.min(axis=2) > 190, 400, 150, "youtube-white")

# --- play: bright/white triangle (multi colour gradient) ---
sub = a[150:370, 666:892]
bg = np.array([0x82, 0xDD, 0x7E])
d = np.abs(sub - bg).max(axis=2)
bbox(d > 45, 666, 150, "play-triangle")

# --- chrome: colourful circle ---
sub = a[150:370, 932:1158]
bg = np.array([0x8A, 0x48, 0xD0])
d = np.abs(sub - bg).max(axis=2)
bbox(d > 45, 932, 150, "chrome-circle")

print("\n== button icon stroke colours (bright pixels) ==")
for name, (x0, x1) in {"keystone": (130, 310), "miracast": (340, 520), "signal": (550, 730),
                       "myapps": (760, 940), "settings": (975, 1155)}.items():
    sub = a[528:600, x0:x1].reshape(-1, 3)
    bright = sub[sub.min(axis=1) > 150]
    if len(bright):
        m = bright.mean(axis=0)
        print("   %-9s n=%5d mean #%02X%02X%02X  max #%02X%02X%02X" % (
            name, len(bright), int(m[0]), int(m[1]), int(m[2]),
            int(bright[:, 0].max()), int(bright[:, 1].max()), int(bright[:, 2].max())))

print("\n== status bar icon bboxes ==")
sub = a[55:95, 660:770]
bbox(sub.min(axis=2) > 150, 660, 55, "cast+mouse area")
sub2 = a[50:95, 660:720]
bbox(sub2.min(axis=2) > 130, 660, 50, "icon1")
sub3 = a[50:95, 715:765]
bbox(sub3.min(axis=2) > 130, 715, 50, "wifi")
