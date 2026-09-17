# -*- coding: utf-8 -*-
"""Patch means, edge profiles and crops for visual inspection."""
import os
import numpy as np
from PIL import Image

SRC = r"D:\23178\DP-Launcher\_ref\reference.png"
OUT = r"D:\23178\DP-Launcher\_ref"
im = Image.open(SRC).convert("RGB")
a = np.asarray(im).astype(np.float64)


def mean(x0, y0, x1, y1):
    m = a[y0:y1, x0:x1].reshape(-1, 3).mean(axis=0)
    return "#%02X%02X%02X" % tuple(int(round(v)) for v in m)


print("== patch means ==")
print("   wallpaper          ", mean(20, 300, 90, 600))
print("   card1 netflix      ", mean(160, 180, 330, 340))
print("   card2 youtube      ", mean(410, 180, 600, 340))
print("   card3 play         ", mean(670, 180, 870, 260))
print("   card4 chrome       ", mean(930, 180, 1135, 260))
print("   btn fill keystone  ", mean(140, 540, 300, 600))
print("   btn fill miracast  ", mean(350, 540, 510, 600))
print("   btn fill settings  ", mean(980, 560, 1140, 620))

print("\n== vertical profile x=720 (card3) y=395..485 ==")
for y in range(395, 486, 2):
    v = a[y, 720]
    print("   y=%3d  #%02X%02X%02X" % (y, int(v[0]), int(v[1]), int(v[2])))

print("\n== vertical profile x=250 (card1) y=395..485 ==")
for y in range(395, 486, 2):
    v = a[y, 250]
    print("   y=%3d  #%02X%02X%02X" % (y, int(v[0]), int(v[1]), int(v[2])))

print("\n== vertical profile x=220 (keystone btn) y=505..530 ==")
for y in range(505, 531):
    v = a[y, 220]
    print("   y=%3d  #%02X%02X%02X" % (y, int(v[0]), int(v[1]), int(v[2])))

print("\n== vertical profile x=1060 (settings focused) y=495..530 ==")
for y in range(495, 531):
    v = a[y, 1060]
    print("   y=%3d  #%02X%02X%02X" % (y, int(v[0]), int(v[1]), int(v[2])))

print("\n== vertical profile x=960/1160 (settings left/right edge) y=560 ==")
v = a[560, 940:990]
print("   x 940..989:", " ".join("#%02X%02X%02X" % (int(p[0]), int(p[1]), int(p[2])) for p in v[::3]))

crops = {
    "c_status.png": (560, 40, 1280, 150, 3),
    "c_card1.png": (110, 130, 380, 480, 2),
    "c_card2.png": (380, 130, 650, 480, 2),
    "c_card3.png": (645, 130, 905, 480, 2),
    "c_card4.png": (900, 130, 1175, 480, 2),
    "c_bottom.png": (100, 495, 700, 670, 2),
    "c_settings.png": (930, 490, 1200, 675, 3),
    "c_topedge.png": (100, 100, 700, 210, 3),
}
os.makedirs(OUT, exist_ok=True)
for name, (x0, y0, x1, y1, sc) in crops.items():
    c = im.crop((x0, y0, x1, y1))
    c = c.resize((c.width * sc, c.height * sc), Image.LANCZOS)
    c.save(os.path.join(OUT, name))
    print("wrote", name, c.size)
