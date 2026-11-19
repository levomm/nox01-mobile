"""Contract checks for Hive's chat-completions-only API integration in NOX01."""

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
providers = (ROOT / "app/src/main/java/app/buddy/assistant/Providers.java").read_text()
runner = (ROOT / "linux/buddy-run").read_text()
relay = (ROOT / "app/src/main/java/app/buddy/assistant/Relay.java").read_text()

assert '{"TheHiveAI", "", "https://api-cdn.thehive.ai/api/v3", "chat"}' in providers, "Hive preset missing"
assert re.search(r'static boolean isHive\(String base\)', providers), "Hive API must be recognized by hostname"
assert re.search(r'if\s*\("codex"\.equals\(agent\)\)\s*return\s*!openai\.isEmpty\(\)\s*&&\s*!isHive\(openai\)', providers), (
    "Chat-Completions-only Hive must not be advertised as a Codex Responses provider"
)
assert 'isHive(p.openai) ? "chat" : "responses"' in providers, "Hive must be marked chat-only for the helper"
for model in ("deepseek-ai/deepseek-v4.1-flash-1017", "zai-org/glm-5.3-flash"):
    assert model in providers, f"Hive model {model} missing"
assert '1_000_000' in providers, "Hive models must carry the documented context window"
assert 'if (isHive(openai)) return hiveModels();' in providers, "Known Hive models must not require an undocumented /models endpoint"
assert 'ask(1, key, openai, models)' in providers, "Hive check should use Chat Completions, not Responses"
assert 'npm:"@ai-sdk/openai-compatible"' in runner, "OpenCode must speak the OpenAI-compatible wire protocol"
assert '"/chat/completions"' in relay, "Relay must support the chat completions route"
print("PASS: Hive preset, exact model IDs, chat-only routing, check and OpenCode relay")
