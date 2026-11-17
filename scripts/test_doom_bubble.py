"""NOX01 live DOOM bubble regression tests (standard Python, no SDK needed)."""
from pathlib import Path
import base64
import re
import struct

ROOT = Path(__file__).resolve().parents[1]
read = lambda path: (ROOT / path).read_text(encoding="utf-8")

def check(label, condition):
    assert condition, label
    print("PASS:",label)

atlas = base64.b64decode(read("app/src/main/assets/nox01/doom_faces.b64").strip(), validate=True)
check("Sprite atlas uses PNG", atlas.startswith(b"\x89PNG\r\n\x1a\n"))
check("8 sprite frames in four-by-two 64x80 layout",
      struct.unpack(">II", atlas[16:24]) == (256,160))
check("Sprite atlas has transparent pixels", b"tRNS" in atlas or b"RGBA" in atlas)
src = read("app/src/main/java/app/buddy/assistant/DoomFaces.java")
for state in ("NEUTRAL", "LOOK_LEFT", "LOOK_RIGHT", "EYEBROW", "WORKING", "ANNOYED", "BLOODY", "OVERDRIVE"):
    check("Doom state "+state, re.search(r"static final int "+state+r"\s*=", src) is not None)
check("Atlas is decoded by Android", "Base64.decode(" in src and "BitmapFactory.decodeByteArray" in src)
check("Crisp pixel filtering", "setFilterBitmap(false)" in src)
prefs=read("app/src/main/java/app/buddy/assistant/Prefs.java")
check("Default is doom and legacy buddy preference migrates", 'getString("face", "doom")' in prefs and '"buddy".equals(' in prefs)
setup=read("app/src/main/java/app/buddy/assistant/SetupActivity.java")
check("Doom or custom image option", '{"doom", getString(R.string.setup_face_doom)}' in setup and '{"picture", getString(R.string.setup_face_picture)}' in setup)
bubble=read("app/src/main/java/app/buddy/assistant/Bubble.java")
check("Doom running inside bubble, not chat", 'DoomFaces.drawable(' in bubble and 'private final Runnable doomTick' in bubble)
check("Idle microexpressions and work face", 'DoomFaces.LOOK_LEFT' in bubble and 'DoomFaces.LOOK_RIGHT' in bubble and 'DoomFaces.WORKING' in bubble)
check("Overdrive is gated by activity", 'doomBurstSteps' in bubble and 'doomLastBlood' in bubble)
check("Animation stopped with bubble lifecycle", 'face.removeCallbacks(doomTick);' in bubble)
check("Original bubble face never inserted into chat", 'Face.set(head, R.drawable.ic_launcher_foreground)' in bubble)
en=read("app/src/main/res/values/strings.xml")
et=read("app/src/main/res/values-et/strings.xml")
check("Localized Doom face selector", '<string name="setup_face_doom">DOOM</string>' in en and '<string name="setup_face_doom">DOOM</string>' in et)
version=read("app/src/main/java/app/buddy/assistant/BuildInfo.java")
check("Android update has higher version code", int(re.search(r"VERSION_CODE\s*=\s*(\d+)", version).group(1)) >= 211)
