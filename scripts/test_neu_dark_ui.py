"""Tactical Neu-Dark must be a real NOX01 Android UI, not a generated mockup."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parent.parent
SRC=ROOT/"app/src/main/java/app/buddy/assistant"
RES=ROOT/"app/src/main/res"
def read(path):
    return (SRC/path).read_text(encoding="utf-8")
def check(what, works):
    assert works, what
    print("PASS:",what)

ui=read("Ui.java")
main=read("MainActivity.java")
theme=read("Theme.java")
prefs=read("Prefs.java")
onboarding=read("OnboardingActivity.java")
setup=read("SetupActivity.java")
check("Live pressed scale with spring-like overshoot", "static void elasticPress(View v)" in ui
      and "ACTION_DOWN" in ui and "ACTION_UP" in ui
      and "OvershootInterpolator" in ui
      and "areAnimatorsEnabled()" in ui)
check("Reusable real neumorphic drawables", "static Drawable neuSurface(" in ui
      and "static Drawable neuInsetSurface(" in ui and "LayerDrawable" in ui
      and "GradientDrawable.Orientation.TL_BR" in ui)
check("New theme is selectable", '"neu"' in theme and "prefs_surface_neu" in theme)
check("New theme is a default for old black installs", '"neu_2026_migrated"' in prefs
      and '"neu"' in prefs and '"black"' in prefs)
check("Tactical preset in Appearance", "R.string.preset_tactical_neu" in setup
      and '{"#5CC08A", "neu"}' in setup)
check("Chat cards raised separately, not one flat group", "neuSurface(this, 22)" in main
      and "chatGroup(List<Sessions.Session>" in main and "chatRow(s)" in main
      and "elasticPress(r)" in main)
check("New chat button uses tactile press", "elasticPress(fab)" in main
      and "neuAccentSurface(" in main)
check("Nav pill and each tab tactile", "neuSurface(a, 34)" in ui
      and "neuInsetSurface(a, 28)" in ui
      and "elasticPress(it)" in ui)
check("Real [01] brand mark replaces old Buddy static header face",
      "R.drawable.ic_nox01_mark" in main and "Face.set(face, R.drawable.bubble_face)" not in main
      and "R.drawable.ic_nox01_mark" in onboarding
      and "face.setImageResource(R.drawable.bubble_face)" not in onboarding)
logo=RES/"drawable/ic_nox01_mark.xml"
check("Real vector [01] logo exists", logo.is_file())
root=ET.parse(logo).getroot()
paths=list(root.iter("path"))
check("Approved coral and white brand vectors", len(paths)>=4
      and any("#FFF37656" in v for p in paths for v in p.attrib.values())
      and any("#FFF5F4F0" in v for p in paths for v in p.attrib.values()))
for lang in ("values","values-et"):
    strings=(RES/lang/"strings.xml").read_text(encoding="utf-8")
    check(f"{lang} Tactical Neu-Dark localized", 'name="prefs_surface_neu"' in strings
          and 'name="preset_tactical_neu"' in strings)
check("Next Android version allows in-place update", int(__import__('re').search(r'VERSION_CODE\s*=\s*(\d+)', read("BuildInfo.java")).group(1)) >= 212)
print("PASS: tactile home, new [01] logo, selectable neu dark and accessibility-safe animations")
