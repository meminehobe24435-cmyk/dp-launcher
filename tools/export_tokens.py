# -*- coding: utf-8 -*-
"""Generate preview/tokens.css and preview/tokens.json from DesignSpec.kt.

DesignSpec.kt is the single source of truth for the launcher geometry: the Android code reads it
through UiScale, and this script mirrors it into CSS custom properties so the HTML preview (used
for the pixel comparison and the demo video) can never drift away from the app.
"""
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPEC = os.path.join(ROOT, "app", "src", "main", "java", "com", "dp", "launcher", "ui", "DesignSpec.kt")
OUT_CSS = os.path.join(ROOT, "preview", "tokens.css")

DECL = re.compile(r"^\s*const val ([A-Z][A-Z0-9_]*)\s*(?::\s*\w+)?\s*=\s*(.+?)\s*$")

values = {}
order = []

with open(SPEC, "r", encoding="utf-8") as handle:
    for line in handle:
        line = line.split("//")[0].rstrip()
        match = DECL.match(line)
        if not match:
            continue
        name, expr = match.group(1), match.group(2)
        expr = expr.rstrip(",")
        # Kotlin-isms that have no meaning in Python.
        expr = expr.replace(".toInt()", "").replace(".toFloat()", "")
        expr = re.sub(r"(\d)[fF]\b", r"\1", expr)
        try:
            value = eval(expr, {"__builtins__": {}}, dict(values))
        except Exception as error:  # pragma: no cover - build-time diagnostic
            raise SystemExit("cannot evaluate %s = %s (%s)" % (name, expr, error))
        values[name] = value
        order.append(name)

missing = [n for n in ("REFERENCE_WIDTH", "REFERENCE_HEIGHT", "CARD_WIDTH") if n not in values]
if missing:
    raise SystemExit("DesignSpec.kt is missing %s" % missing)


def css_name(name):
    return "--ds-" + name.lower().replace("_", "-")


def is_unitless(name):
    return name.endswith(("_SCALE", "_ALPHA", "_RATIO", "_COLUMNS"))


def css_value(name, value):
    if isinstance(value, int) and value > 0xFFFFFF:
        # Kotlin stores colours as ARGB ints; CSS wants rgba().
        alpha = ((value >> 24) & 0xFF) / 255.0
        return "rgba(%d, %d, %d, %.3f)" % (
            (value >> 16) & 0xFF,
            (value >> 8) & 0xFF,
            value & 0xFF,
            alpha,
        )
    if is_unitless(name):
        return "%g" % value
    if isinstance(value, float) and value.is_integer():
        value = int(value)
    return "%spx" % value


lines = [
    "/*",
    " * GENERATED FILE - do not edit by hand.",
    " * Source: app/src/main/java/com/dp/launcher/ui/DesignSpec.kt",
    " * Regenerate with: python tools/export_tokens.py",
    " */",
    ":root {",
]
for name in order:
    lines.append("  %s: %s;" % (css_name(name), css_value(name, values[name])))
lines.append("}")
lines.append("")

with open(OUT_CSS, "w", encoding="utf-8", newline="\n") as handle:
    handle.write("\n".join(lines))

print("wrote %s (%d tokens)" % (OUT_CSS, len(order)))
