"""2.3.19: priced model selector and honest EUR token spend (static integration regression).

Validates instrumentation hooks; Android compile and fixture-driven runtime checks
are independent. The original Codex JSONL fixtures contain real usage counters.
"""
from pathlib import Path
import unittest
import json

ROOT = Path(__file__).resolve().parents[1]
J = ROOT / "app/src/main/java/app/buddy/assistant"

def src(name):
    return (J / name).read_text(encoding="utf-8")

class ApiSpendTests(unittest.TestCase):
    def test_codex_actual_usage_not_context_window(self):
        sh = (ROOT / "linux/buddy-run").read_text(encoding="utf-8")
        stream = sh.split("stream_codex() {",1)[1].split("# ------------------------------------------------------------------- OpenCode",1)[0]
        self.assertIn('buddy_token_usage',stream)
        self.assertIn('cached_input_tokens',stream)
        self.assertIn('output_tokens',stream)
        self.assertIn('input_tokens',stream)
        self.assertIn('--arg model "$MODEL"',stream)
        fixture = (ROOT / "linux/test/fixtures/codex/text_0.162.1.jsonl").read_text()
        ev = [json.loads(l) for l in fixture.splitlines() if l.strip()]
        u = next(x["usage"] for x in ev if x.get("type") == "turn.completed")
        self.assertEqual(u["input_tokens"],16077)
        self.assertEqual(u["cached_input_tokens"],12288)
        self.assertEqual(u["output_tokens"],5)
        self.assertEqual(u["input_tokens"] - u["cached_input_tokens"],3789)

    def test_price_catalog_uses_live_source_and_real_fx(self):
        s=src("ModelPrices.java")
        self.assertIn('https://models.dev/api.json',s)
        self.assertIn('https://api.frankfurter.dev/v1/latest?base=USD&symbols=EUR',s)
        self.assertIn('"openai"',s)
        self.assertIn('cache_read',s)
        self.assertIn('cost.optJSONObject("text")',s)
        self.assertIn('inputUsd',s)
        self.assertIn('outputUsd',s)
        self.assertIn('updatedAt',s)
        self.assertIn('getSharedPreferences',s)
        self.assertNotIn('0.90 /* conversion',s)

    def test_expenses_deduplicate_and_dont_count_cached_twice(self):
        s=src("ApiSpend.java")
        self.assertIn('static void record(Context c, Sessions.Session ses, JSONObject event)',s)
        self.assertIn('input - cached',s)
        self.assertIn('sessionId',s)
        self.assertIn('dayKey',s)
        self.assertIn('monthKey',s)
        self.assertIn('modelRef',s)
        self.assertIn('getSharedPreferences',s)
        self.assertIn('last',s)
        self.assertIn('cached',s)

    def test_model_list_sorts_eligible_prices_and_keeps_unknown_last(self):
        s=src("Models.java")
        self.assertIn('static java.util.List<String[]> pricedOptions(',s)
        self.assertIn('Providers.chatModels(',s)
        self.assertIn('ModelPrices.price(',s)
        self.assertIn('Comparator',s)
        self.assertIn('topTen',s)
        ui=src("Ui.java")
        self.assertIn('chat_prices_top_ten',ui)
        self.assertIn('chat_prices_all',ui)
        self.assertIn('Models.pricedOptions(',ui)
        self.assertIn('ModelPrices.refresh(',ui)

    def test_compact_cost_badge_outside_messages_visible_over_composer(self):
        s=src("ChatActivity.java")
        self.assertIn('private TextView spendBadge',s)
        self.assertIn('spendBadge.setOnClickListener',s)
        self.assertIn('ApiSpend.summary(',s)
        self.assertIn('ApiSpend.detail(',s)
        self.assertIn('root.addView(spendRow',s)
        self.assertIn('case "buddy_token_usage"',src("ChatHub.java"))

    def test_estonian_labels_and_version(self):
        s=(ROOT/"app/src/main/res/values-et/nox01_spend.xml").read_text(encoding="utf-8")
        for key in ("spend_last","spend_chat","spend_day","spend_month","spend_input",
                    "spend_output","spend_cached","spend_plan","spend_unknown",
                    "spend_estimate","chat_prices_top_ten","chat_prices_all","chat_prices_source"):
            self.assertIn('name="'+key+'"',s)
        build=src("BuildInfo.java")
        self.assertIn('VERSION = "2.3.20"',build)
        self.assertIn('VERSION_CODE = 219',build)

if __name__=="__main__":
    unittest.main(verbosity=2)
