# -*- coding: utf-8 -*-
"""Turn the captured screencast frames into the demo animation shipped in demo/.

    python tools/make_video.py

Reads preview/out/frames/*.jpg (written by tools/capture_video.mjs) and writes:
    demo/launcher-demo.gif   640x360, for GitHub and chat clients
    demo/launcher-demo.webp  960x540, smaller and sharper than the GIF
"""
import glob
import json
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FRAME_DIR = os.path.join(ROOT, "preview", "out", "frames")
DEMO_DIR = os.path.join(ROOT, "demo")

FPS = 12
GIF_SIZE = (720, 405)
WEBP_SIZE = (960, 540)
MAX_SECONDS = 30


def load_frames():
    paths = sorted(glob.glob(os.path.join(FRAME_DIR, "*.jpg")))
    if not paths:
        raise SystemExit("no frames found - run `node tools/capture_video.mjs` first")
    with open(os.path.join(FRAME_DIR, "index.json"), "r", encoding="utf-8") as handle:
        meta = json.load(handle)
    stamps = meta.get("frames") or []
    return paths, stamps


def resample(paths, stamps):
    """Resamples the variable-rate capture onto a constant FPS timeline."""
    if len(stamps) != len(paths) or not stamps:
        return paths
    start, end = stamps[0], min(stamps[-1], stamps[0] + MAX_SECONDS)
    count = max(1, int((end - start) * FPS))
    sequence = []
    cursor = 0
    for index in range(count):
        target = start + index / float(FPS)
        while cursor + 1 < len(stamps) and stamps[cursor + 1] <= target:
            cursor += 1
        sequence.append(paths[cursor])
    return sequence


def build(sequence, size, out_path, **save_kwargs):
    images = []
    for path in sequence:
        with Image.open(path) as frame:
            images.append(frame.convert("RGB").resize(size, Image.LANCZOS))
    images[0].save(
        out_path,
        save_all=True,
        append_images=images[1:],
        duration=int(1000 / FPS),
        loop=0,
        **save_kwargs,
    )
    return out_path, len(images)


def main():
    os.makedirs(DEMO_DIR, exist_ok=True)
    paths, stamps = load_frames()
    sequence = resample(paths, stamps)
    print("using %d of %d captured frames" % (len(sequence), len(paths)))

    gif_path = os.path.join(DEMO_DIR, "launcher-demo.gif")
    build(
        sequence,
        GIF_SIZE,
        gif_path,
        optimize=True,
        disposal=2,
    )
    print("wrote %s (%.1f MB)" % (gif_path, os.path.getsize(gif_path) / 1e6))

    webp_path = os.path.join(DEMO_DIR, "launcher-demo.webp")
    build(sequence, WEBP_SIZE, webp_path, quality=82, method=6)
    print("wrote %s (%.1f MB)" % (webp_path, os.path.getsize(webp_path) / 1e6))

    # Still frames used by README.md / DELIVERY.md, taken straight from the recording.
    still_dir = os.path.join(ROOT, "preview", "out", "stills")
    for name in ("home", "drawer"):
        source = os.path.join(still_dir, name + ".png")
        if os.path.exists(source):
            target = os.path.join(DEMO_DIR, name + ".png")
            with Image.open(source) as image:
                image.convert("RGB").save(target)
            print("wrote %s" % target)


if __name__ == "__main__":
    main()
