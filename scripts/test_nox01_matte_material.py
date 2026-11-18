"""Matte-rubber UI regression: light blending must never flood dark panels."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/app/buddy/assistant"

def read(name):
    return (JAVA / name).read_text(encoding="utf-8")

class MatteMaterialTests(unittest.TestCase):
    def test_blend_uses_normal_start_to_end_interpolation(self):
        theme = read("Theme.java")
        implementation = theme.split("static int blend(int a, int b, float t)", 1)[1].split("static int darken", 1)[0]
        for channel in ("red", "green", "blue"):
            self.assertIn(
                f"Color.{channel}(a) + (Color.{channel}(b) - Color.{channel}(a)) * t",
                implementation, "blend(a, white, 0.065) must mean ONLY 6.5% white"
            )

    def test_dark_cards_have_no_white_wash(self):
        ui = read("Ui.java")
        raised = ui.split("static Drawable neuSurface(", 1)[1].split("static Drawable neuInsetSurface(", 1)[0]
        inset = ui.split("static Drawable neuInsetSurface(", 1)[1].split("static Drawable neuAccentSurface(", 1)[0]
        for body in (raised, inset):
            self.assertIn("LayerDrawable", body)
            self.assertNotIn("0xFFFFFFFF", body)
            for expression in re.findall(r"Theme.blend\((?:CARD|CARD2), Color.WHITE, DARK \? ([0-9.]+)f", body):
                self.assertLess(float(expression), 0.09)
        selected = ui.split("static LinearLayout segmented(", 1)[2].split("static View radio(", 1)[0]
        self.assertIn("neuSurface(c, 11)", selected)
        nav = ui.split("static LinearLayout bottomNav(", 1)[1]
        self.assertIn("neuInsetSurface(a, 28)", nav)

    def test_user_chat_color_keeps_dark_accent_subtle(self):
        theme = read("Theme.java")
        self.assertIn("Ui.USER = blend(Ui.BG, accent, 0.32f)", theme)
        self.assertIn("Ui.USER = blend(Ui.CARD, accent, 0.16f)", theme)

    def test_version_217(self):
        build = read("BuildInfo.java")
        self.assertIn('VERSION = "2.3.17"', build)
        self.assertRegex(build, r"VERSION_CODE\s*=\s*216\s*;")

if __name__ == "__main__":
    unittest.main(verbosity=2)
