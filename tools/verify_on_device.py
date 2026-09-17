# -*- coding: utf-8 -*-
"""End-to-end verification of the launcher on a real Android device or emulator.

    python tools/verify_on_device.py [--apk dist/DP-Launcher-v1.0.0.apk] [--serial emulator-5554]

What it does, and why each step exists:

1. installs the APK and launches it                     - does the app start at all?
2. fails on any AndroidRuntime exception in logcat      - the crash class the JVM tests miss
3. drives the D-pad through the reference state and into the app drawer, saving a screenshot for
   every step                                          - do the navigation rules work on device?
4. checks which view actually holds the focus via uiautomator, because a screenshot cannot tell
   the difference between "nothing focused" and "the right thing focused"
5. presses HOME after setting the app as the home activity - does the device accept it as a launcher?

The screenshots land in preview/out/android/.
"""
import argparse
import os
import re
import subprocess
import sys
import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(ROOT, "preview", "out", "android")
DEFAULT_APK = os.path.join(ROOT, "dist", "DP-Launcher-v1.0.0.apk")
PACKAGE = "com.dp.launcher"
ACTIVITY = PACKAGE + "/.LauncherActivity"

# Key codes of the D-pad as sent by `adb shell input keyevent`.
KEY = {
    "up": 19,
    "down": 20,
    "left": 21,
    "right": 22,
    "enter": 66,
    "back": 4,
    "home": 3,
    "menu": 82,
}


# Resolved by find_adb() so the script also works before PATH is set up.
ADB = "adb"


def adb(serial, *args, check=True):
    command = [ADB] + (["-s", serial] if serial else []) + [str(a) for a in args]
    result = subprocess.run(command, capture_output=True, text=True)
    if check and result.returncode != 0:
        raise SystemExit("adb %s failed:\n%s%s" % (" ".join(map(str, args)), result.stdout, result.stderr))
    return result.stdout


def resolve_adb():
    """Prefers the adb from ANDROID_SDK_ROOT; falls back to whatever is on PATH."""
    global ADB
    sdk = os.environ.get("ANDROID_SDK_ROOT") or os.environ.get("ANDROID_HOME")
    if sdk:
        candidate = os.path.join(sdk, "platform-tools", "adb.exe" if os.name == "nt" else "adb")
        if os.path.exists(candidate):
            ADB = candidate
            return
    ADB = "adb"


def first_device():
    output = subprocess.run([ADB, "devices"], capture_output=True, text=True).stdout
    for line in output.splitlines()[1:]:
        parts = line.split()
        if len(parts) >= 2 and parts[1] == "device":
            return parts[0]
    raise SystemExit("no device or emulator attached - start one, or pass --serial")


def focused_bounds(serial):
    """Returns the bounds of the focused view, or None."""
    adb(serial, "shell", "uiautomator", "dump", "/sdcard/_verify_ui.xml")
    xml = adb(serial, "shell", "cat", "/sdcard/_verify_ui.xml")
    match = re.search(r'focused="true"[^>]*bounds="(\[[^"]+)"', xml)
    return match.group(1) if match else None


def screenshot(serial, name):
    os.makedirs(OUT_DIR, exist_ok=True)
    remote = "/sdcard/_verify_%s.png" % name
    adb(serial, "shell", "screencap", "-p", remote)
    adb(serial, "pull", remote, os.path.join(OUT_DIR, name + ".png"))
    return os.path.join(OUT_DIR, name + ".png")


def press(serial, *names, delay=0.35):
    for name in names:
        adb(serial, "shell", "input", "keyevent", KEY[name])
        time.sleep(delay)


def crashes(serial):
    output = adb(serial, "logcat", "-d", "-s", "AndroidRuntime:E")
    # The log always starts with the three "beginning of ..." banner lines.
    lines = [line for line in output.splitlines() if line.strip() and "beginning of" not in line]
    return [line for line in lines if "AndroidRuntime" in line]


def main():
    global ADB
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", default=DEFAULT_APK)
    parser.add_argument("--serial", default=None)
    args = parser.parse_args()

    resolve_adb()
    serial = args.serial or first_device()
    print("device: %s (adb: %s)" % (serial, ADB))

    if not os.path.exists(args.apk):
        raise SystemExit("APK not found: %s - run `./gradlew :app:assembleDebug` first" % args.apk)

    print("-- install")
    print(adb(serial, "install", "-r", args.apk).strip())

    print("-- launch")
    adb(serial, "logcat", "-c")
    adb(serial, "shell", "am", "force-stop", PACKAGE)
    adb(serial, "shell", "am", "start", "-n", ACTIVITY)
    time.sleep(4)

    focused = focused_bounds(serial)
    print("   focused view: %s" % focused)
    screenshot(serial, "android_home_card")

    print("-- navigate: dock -> Settings (the state of the reference screenshot)")
    press(serial, "down", "right", "right", "right", "right", delay=0.4)
    time.sleep(1)
    settings_focus = focused_bounds(serial)
    print("   focused view: %s" % settings_focus)
    screenshot(serial, "android_home_settings")

    print("-- DOWN on the dock must open the installed-app list")
    press(serial, "down")
    time.sleep(2)
    drawer_focus = focused_bounds(serial)
    print("   focused view: %s" % drawer_focus)
    screenshot(serial, "android_drawer")

    print("-- UP on the first grid row must close it and restore the focus")
    press(serial, "up")
    time.sleep(2)
    restored = focused_bounds(serial)
    print("   focused view: %s" % restored)
    screenshot(serial, "android_home_after_up")

    print("-- MENU must open the app list as well")
    press(serial, "menu")
    time.sleep(2)
    menu_focus = focused_bounds(serial)
    print("   focused view: %s" % menu_focus)
    press(serial, "back")
    time.sleep(1)

    print("-- behave as the device launcher")
    adb(serial, "shell", "cmd", "package", "set-home-activity", ACTIVITY, check=False)
    press(serial, "home")
    time.sleep(3)
    resumed = adb(serial, "shell", "dumpsys", "activity", "activities")
    is_home = ACTIVITY.replace("/", "/") in resumed and "mResumedActivity" in resumed
    screenshot(serial, "android_as_home")

    problems = crashes(serial)
    print("\n-- result")
    checks = [
        ("app launched without a crash", not problems),
        ("first card focused on start", focused is not None),
        ("dock reachable with DOWN", settings_focus is not None),
        ("DOWN on the dock opens the app list", drawer_focus is not None),
        ("UP restores the focus", restored is not None),
        ("MENU opens the app list", menu_focus is not None),
        ("accepted as the home activity", is_home),
    ]
    for label, ok in checks:
        print("   [%s] %s" % ("PASS" if ok else "FAIL", label))
    if problems:
        print("\ncrash log:")
        for line in problems[:20]:
            print("   " + line)
    return 0 if all(ok for _, ok in checks) else 1


if __name__ == "__main__":
    sys.exit(main())
