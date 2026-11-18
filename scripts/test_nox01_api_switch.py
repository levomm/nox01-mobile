"""NOX01 one-tap account/API switching regression (2.3.18).

The API route must not inherit ChatGPT Apps schemas (a nested '_search'
oneOf tool broke Responses), nor resume a Codex thread on another provider.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/app/buddy/assistant"

def read(name):
    return (JAVA / name).read_text(encoding="utf-8")

class ApiSwitchRegression(unittest.TestCase):
    def test_api_route_disables_chatgpt_only_app_tools_even_for_openai_key(self):
        bash = (ROOT / "linux/buddy-run").read_text(encoding="utf-8")
        block = bash.split('if [ -n "$SVC" ]; then\n    #', 1)[1].split('local win', 1)[0]
        self.assertIn("cfg+=(-c features.apps=false", block)
        self.assertNotIn(".openai_own // false", block, "Even OpenAI keys must not inherit ChatGPT Apps schemas")
        self.assertIn("features.tool_suggest=false", block)
        self.assertIn("mcp_servers.phone.url", bash, "Keep phone MCP available")

    def test_changing_codex_provider_starts_new_native_thread(self):
        bash = (ROOT / "linux/buddy-run").read_text(encoding="utf-8")
        block = bash.split("run_codex() {", 1)[1].split("# Translate Codex", 1)[0]
        self.assertIn("codex-provider", block)
        self.assertIn("old_provider", block)
        self.assertIn("expected_provider", block)
        self.assertIn('rm -f "$S/codex-thread"', block)
        self.assertIn('printf \'%s\' "$expected_provider"', block)
        self.assertIn('BUDDY_TOKEN=$TOKEN codex exec', block)

    def test_quick_api_model_is_a_chat_model_not_image_or_pro_default(self):
        models = read("Models.java")
        self.assertIn("static String quickApiModel(String backend)", models)
        self.assertIn('"gpt-4.1-mini"', models)
        self.assertIn('"gpt-4o-mini"', models)
        self.assertIn("Providers.chatModels(", models)
        self.assertIn("Providers.forAgent(backend)", models)

    def test_chat_model_sheet_has_one_tap_route_choice_and_short_list(self):
        chat = read("ChatActivity.java")
        block = chat.split("private void chooseModel()", 1)[1].split("// ------------------------------------------------------ helpers", 1)[0]
        self.assertIn("routePicker(", block)
        self.assertIn("quickApiModel(", read("Ui.java"))
        self.assertIn("chat_model_more", read("Ui.java"))
        self.assertIn("chooseModel(true)", block)
        self.assertIn("setModel(ses,", block)

    def test_global_default_picker_supports_same_quick_choice(self):
        ui = read("Ui.java")
        block = ui.split("static void modelSheet(Activity a, String backend", 1)[1].split("static TextView codeBlock", 1)[0]
        self.assertIn("routePicker(", block)
        self.assertIn("quickApiModel(", block)
        self.assertIn("chat_model_more", block)

    def test_localized_label_and_version(self):
        et = read("BuildInfo.java")
        self.assertIn('VERSION = "2.3.18"', et)
        self.assertIn("VERSION_CODE = 217", et)
        strings = (ROOT / "app/src/main/res/values-et/nox01_api_switch.xml")
        self.assertTrue(strings.exists())
        s = strings.read_text(encoding="utf-8")
        for key in ("chat_source_title", "chat_source_plan", "chat_source_api",
                    "chat_model_more", "chat_source_new_thread", "chat_source_api_missing"):
            self.assertIn('name="' + key + '"', s)

if __name__ == "__main__":
    unittest.main(verbosity=2)
