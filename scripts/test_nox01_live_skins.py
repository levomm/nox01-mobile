"""NOX01 2.3.15 live sprite, shared skin and chat contrast regressions."""
from pathlib import Path
import base64
import re
import struct
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/app/buddy/assistant"


def java(name):
    return (SRC / name).read_text(encoding="utf-8")


class Nox01LiveSkinsTests(unittest.TestCase):
    def test_working_uses_only_uploaded_white_eye_sprite(self):
        doom = java("DoomFaces.java")
        self.assertRegex(doom, r"WORKING\s*=\s*4\s*;")
        self.assertIn('nox01/doom_white_eyes.b64', doom)
        self.assertIn('state == WORKING', doom)
        data = base64.b64decode((ROOT / "app/src/main/assets/nox01/doom_white_eyes.b64").read_text().strip(), validate=False)
        self.assertTrue(data.startswith(b"\x89PNG\r\n\x1a\n"))
        self.assertEqual(struct.unpack(">II", data[16:24]), (64, 80))
        self.assertEqual(data[25], 6, "white-eye sprite must support RGBA transparency")

    def test_running_does_not_lose_white_eyes_during_normal_steps(self):
        bubble = java("Bubble.java")
        ticker = bubble.split("private final Runnable doomTick", 1)[1].split("private void setDoomFrame", 1)[0]
        self.assertIn("else if (doomRunning)", ticker)
        running = ticker.split("else if (doomRunning)", 1)[1].split("} else if", 1)[0]
        self.assertIn("DoomFaces.WORKING", running)
        self.assertNotIn("DoomFaces.ANNOYED", running)
        step = bubble.split("private void doomStep()", 1)[1].split("private boolean noteListening", 1)[0]
        self.assertNotIn("setDoomFrame(DoomFaces.ANNOYED)", step)

    def test_five_global_presets_and_persisted_visual_controls(self):
        theme = java("Theme.java")
        setup = java("SetupActivity.java")
        prefs = java("Prefs.java")
        ui = java("Ui.java")
        for key in ("neu", "terminal", "alert", "steel", "mono"):
            self.assertRegex(theme, rf'case "{key}"|\{{"{key}",')
            self.assertIn('"' + key + '"', setup)
        for func in ("cardDepth", "setCardDepth", "bubbleFrame", "setBubbleFrame"):
            self.assertIn(func + "(", prefs)
        self.assertIn("Theme.changed(this)", setup)
        self.assertIn("Prefs.cardDepth(c)", ui)
        self.assertIn("Theme.blend(CARD", ui)
        self.assertNotIn("rounded(c, 0xFF0E1014", ui)
        self.assertIn("Prefs.bubbleFrame(svc)", java("Bubble.java"))

    def test_both_chat_views_share_distinct_message_cards(self):
        ui = java("Ui.java")
        app = java("ChatActivity.java")
        bubble = java("Bubble.java")
        for helper in ("userMessageSurface", "agentMessageSurface", "userLabel", "agentLabel"):
            self.assertIn(helper + "(", ui)
        for src in (app, bubble):
            self.assertIn("Ui.userMessageSurface(", src)
            self.assertIn("Ui.agentMessageSurface(", src)
            self.assertIn("Ui.userLabel(", src)
            self.assertIn("Ui.agentLabel(", src)
            self.assertIn("Gravity.END", src)
            self.assertIn("Gravity.START", src)
        self.assertIn("Ui.tableParts(text)", app)
        self.assertIn("Ui.tableParts(text)", bubble)
        self.assertIn("copyOnLongPress", app)
        self.assertIn("copyOnLongPress", bubble)

    def test_bumped_compatible_android_version(self):
        build = java("BuildInfo.java")
        self.assertRegex(build, r'VERSION\s*=\s*"2\.3\.21"')
        self.assertRegex(build, r"VERSION_CODE\s*=\s*220\s*;")


if __name__ == "__main__":
    unittest.main(verbosity=2)