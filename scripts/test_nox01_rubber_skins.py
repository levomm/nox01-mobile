"""Regression: all NOX01 themes keep the same tactile, matte rubber material."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/app/buddy/assistant"


def read(name):
    return (JAVA / name).read_text(encoding="utf-8")


def dark_color_set(theme, surface):
    match = re.search(
        r'case "' + re.escape(surface) + r'":\s*set\(([^)]*)\);',
        theme,
    )
    assert match is not None, f"missing dark theme {surface}"
    colors = [int(part.strip(), 16) for part in match.group(1).split(",")]
    assert len(colors) == 5
    return [tuple((arg >> shift) & 255 for shift in (16, 8, 0)) for arg in colors]


class RubberSkinsRegression(unittest.TestCase):
    def test_every_theme_raises_cards_by_default_and_migrates_flat_bug(self):
        prefs = read("Prefs.java")
        depth = prefs.split("static String cardDepth(Context c)", 1)[1].split("static void setCardDepth", 1)[0]
        self.assertIn('getString("card_depth", "raised")', depth)
        self.assertIn("rubber_2026_migrated", depth)
        self.assertIn('.putString("card_depth", "raised")', depth)

    def test_switching_preset_never_changes_physical_depth(self):
        setup = read("SetupActivity.java")
        selector = setup.split("private View themePresets()", 1)[1].split("// ----------------------------------------------------------- appearance", 1)[0]
        self.assertIn("Prefs.setSurface(this, surface)", selector)
        self.assertIn("Prefs.setAccent(this, color)", selector)
        self.assertNotIn("Prefs.setCardDepth(", selector, "changing paint must not flatten the rubber")

    def test_all_five_skins_use_muted_charcoal_surfaces(self):
        theme = read("Theme.java").split("if (dark)", 1)[1].split("} else {", 1)[0]
        for key in ("neu", "terminal", "alert", "steel", "mono"):
            bg, card, card2, nav, code = dark_color_set(theme, key)
            for surface in (bg, card, card2, nav):
                self.assertLessEqual(max(surface) - min(surface), 18, (key, surface))
            self.assertGreater(sum(card), sum(bg) + 18, key)
            self.assertLess(max(bg), 52, key)
        self.assertNotIn('Ui.TEXT = "terminal".equals(surface)', read("Theme.java"))

    def test_surface_highlights_are_soft_rubber_not_chrome(self):
        ui = read("Ui.java")
        raise_block = ui.split("static Drawable neuSurface(", 1)[1].split("static Drawable neuInsetSurface(", 1)[0]
        match = re.search(r'blend\(CARD, Color.WHITE, DARK \? ([0-9.]+)f', raise_block)
        self.assertIsNotNone(match, "soft rim highlight must be palette-derived")
        self.assertLessEqual(float(match.group(1)), 0.10)
        self.assertIn("LayerDrawable", raise_block)
        self.assertIn("GradientDrawable", raise_block)
        press = ui.split("static void elasticPress(", 1)[1].split("static ", 1)[0]
        self.assertIn("OvershootInterpolator", press)
        self.assertIn("ACTION_DOWN", press)
        fab = ui.split("static Drawable neuAccentSurface(", 1)[1].split("static void elasticPress(", 1)[0]
        self.assertNotIn("Color.WHITE, 0.28f", fab)

    def test_theme_is_only_colour_not_material(self):
        ui = read("Ui.java")
        self.assertIn('"raised".equals(Prefs.cardDepth(c))', ui)
        main = read("MainActivity.java")
        self.assertIn("neuSurface(this, 22)", main)
        self.assertIn("elasticPress(r)", main)
        self.assertIn("neuSurface(a, 34)", ui)
        self.assertIn("elasticPress(it)", ui)

    def test_new_upgrade_version(self):
        build = read("BuildInfo.java")
        self.assertIn('VERSION = "2.3.16"', build)
        self.assertRegex(build, r"VERSION_CODE\s*=\s*215\s*;")


if __name__ == "__main__":
    unittest.main(verbosity=2)
