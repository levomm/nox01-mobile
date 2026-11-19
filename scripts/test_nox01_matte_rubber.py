"""Regression tests for matte rubber surfaces without white chrome/gloss.

The earlier Theme.blend(a, b, t) weighs *a* by t. A low t with
Color.WHITE as b made several tactile surfaces ~95% WHITE, even when
the author expected a 5% white highlight. This must never return.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/app/buddy/assistant"


def source(name):
    return (SRC / name).read_text(encoding="utf-8")


def region(ui, start, end):
    return ui.split(start, 1)[1].split(end, 1)[0]


class MatteRubberTests(unittest.TestCase):
    def test_raised_surface_cannot_use_white_gloss(self):
        ui = source("Ui.java")
        raised = region(ui, "static Drawable neuSurface(", "static Drawable neuInsetSurface(")
        self.assertNotIn("Color.WHITE", raised)
        self.assertIn("LayerDrawable", raised, "Keep the layered rubber depth")
        self.assertIn("GradientDrawable", raised)
        self.assertIn("Theme.blend(CARD, CARD2", raised)

    def test_pressed_surface_stays_dark(self):
        ui = source("Ui.java")
        inset = region(ui, "static Drawable neuInsetSurface(", "static Drawable neuAccentSurface(")
        self.assertNotIn("Color.WHITE", inset)
        self.assertIn("LayerDrawable", inset)
        self.assertIn("Theme.blend(BG,", inset, "Pressed state should use dark substrate")

    def test_accent_buttons_do_not_have_white_glow(self):
        ui = source("Ui.java")
        accent = region(ui, "static Drawable neuAccentSurface(", "static void elasticPress(")
        self.assertNotIn("Color.WHITE", accent)
        self.assertIn("LayerDrawable", accent)
        self.assertIn("Theme.darken(accent", accent)

    def test_selected_segment_is_dark_pressed_rubber_not_silver(self):
        ui = source("Ui.java")
        segment = region(ui, "static LinearLayout segmented(Context c, String[][] options, String selected, boolean monoLabels, Pick l,", "/** A radio button's look")
        self.assertIn("if (on) t.setBackground(neu(c) ? neuInsetSurface(c, 11)", segment)
        self.assertNotIn("if (on) t.setBackground(neu(c) ? neuSurface", segment)

    def test_other_screens_use_shared_material_and_bouncy_touch(self):
        ui = source("Ui.java")
        self.assertIn("bar.setBackground(neu(a) ? neuSurface(a, 34)", ui)
        self.assertIn("if (on) it.setBackground(neu(a) ? neuInsetSurface(a, 28)", ui)
        self.assertIn("OvershootInterpolator", ui)
        setup = source("SetupActivity.java")
        self.assertIn("neuSurface(this, 20)", setup)
        main = source("MainActivity.java")
        self.assertIn("neuSurface(this, 22)", main)

    def test_next_installable_version(self):
        build = source("BuildInfo.java")
        self.assertIn('VERSION = "2.3.17"', build)
        self.assertRegex(build, r"VERSION_CODE\s*=\s*216\s*;")


if __name__ == "__main__":
    unittest.main(verbosity=2)
