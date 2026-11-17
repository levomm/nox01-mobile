from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
src = lambda path: (ROOT / path).read_text(encoding="utf-8")
gradle = src("app/build.gradle.kts")
manifest = src("app/src/main/AndroidManifest.xml")
files = src("app/src/main/java/app/buddy/assistant/FilesProvider.java")
chat = src("app/src/main/java/app/buddy/assistant/ChatActivity.java")
theme = src("app/src/main/java/app/buddy/assistant/Theme.java")
prefs = src("app/src/main/java/app/buddy/assistant/Prefs.java")
strings = src("app/src/main/res/values/strings.xml")

def check(name, condition):
    if not condition:
        raise AssertionError(name)
    print("PASS:", name)

check("separate Android package", 'applicationId = "ee.osx01.mobile"' in gradle)
check("matched file provider", 'android:authorities="ee.osx01.mobile.files"' in manifest and 'AUTHORITY = "ee.osx01.mobile.files"' in files)
check("app branded NOX01", '<string name="app_name" translatable="false">NOX01 Mobile</string>' in strings)
check("Estonian registered", 'android:name="et"' in src("app/src/main/res/xml/locales_config.xml"))
et = ET.parse(ROOT / "app/src/main/res/values-et/strings.xml").getroot()
check("Estonian basic chat strings", {"chat_send", "chat_input_hint", "main_new_chat", "bubble_face_description", "onboarding_welcome_title"} <= {x.attrib["name"] for x in et if x.tag == "string"})
check("dark default", 'getString("theme_mode", "dark")' in prefs and 'getString("surface", "black")' in prefs)
check("terminal composer", 'input.setTypeface(sans(this, 400));' in chat and 'pill.setBackground(outlined(this, BG, 8, LINE, 1));' in chat)
check("terminal user prompt", '"> " + text' in chat and 'setBackgroundColor(BG)' in chat)
check("terminal assistant content", 'tv.setTypeface(sans(this, 400));' in chat)
check("no avatar in chat", 'R.drawable.bubble_face' not in chat and 'Face.set(' not in chat)
check("fork update endpoint", 'https://api.github.com/repos/levomm/nox01-mobile/releases/latest' in src("app/src/main/java/app/buddy/assistant/Updates.java"))

# NOX01 v0.2 personalization regressions. These intentionally start RED.
setup = src("app/src/main/java/app/buddy/assistant/SetupActivity.java")
ui = src("app/src/main/java/app/buddy/assistant/Ui.java")
localized = {x.attrib["name"] for x in et if x.tag == "string"}
check("persistent UI font preference", 'String uiFont(Context c)' in prefs and 'setUiFont(Context c, String' in prefs)
check("font selector on Appearance page", 'fontPicker()' in setup and 'setup_ui_font' in setup)
check("font choice changes shared typography", 'Prefs.uiFont(c)' in ui)
check("font change invalidates screen theme key", 'Prefs.uiFont(c)' in theme)
check("named theme presets on Appearance page", 'themePresets()' in setup and 'Prefs.setAccent(this' in setup)
check("Estonian font and theme settings", {'setup_ui_font', 'setup_theme_presets', 'font_geist_mono', 'font_system_mono'} <= localized)
check("Estonian safety and settings", {'settings_safety', 'settings_appearance_sub', 'setup_settings_title', 'ui_nav_chats', 'ui_nav_settings', 'chat_approvals_strict'} <= localized)
