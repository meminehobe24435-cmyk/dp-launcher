# -*- coding: utf-8 -*-
"""Sample exact colors / geometry from the reference launcher screenshot."""
import sys
from collections import Counter
from PIL import Image

SRC = r"D:\23178\DP-Launcher\_ref\reference.png"

im = Image.open(SRC).convert("RGB")
W, H = im.size
px = im.load()
print("size:", W, H, "ratio:", round(W / H, 4))

print("\n== top 14 colors ==")
c = Counter(im.getdata())
for col, n in c.most_common(14):
    print("  #%02X%02X%02X" % col, n)


def runs(y=None, x=None, tol=26, minlen=3):
    out = []
    if y is not None:
        n = W
        get = lambda i: px[i, y]
    else:
        n = H
        get = lambda i: px[x, i]
    start = 0
    base = get(0)
    for i in range(1, n):
        cur = get(i)
        if max(abs(cur[k] - base[k]) for k in range(3)) > tol:
            out.append((start, i - 1, i - start, "#%02X%02X%02X" % base))
            start = i
            base = cur
    out.append((start, n - 1, n - start, "#%02X%02X%02X" % base))
    return [r for r in out if r[2] >= minlen]


for y in (120, 200, 300, 355, 380, 440, 470, 520, 560, 590):
    print("\n== horizontal scan y=%d ==" % y)
    for r in runs(y=y):
        print("   x %4d..%4d  w=%4d  %s" % (r[0], r[1], r[2], r[3]))

for x in (60, 150, 400, 415, 425, 530, 700, 940, 1000, 1055):
    print("\n== vertical scan x=%d ==" % x)
    for r in runs(x=x):
        print("   y %4d..%4d  h=%4d  %s" % (r[0], r[1], r[2], r[3]))
