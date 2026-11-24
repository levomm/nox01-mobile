"""Regression contract: NOX01 drives Fieldsmith CAD over its local Android MCP bridge.

This covers tool registration and safe transport wiring. Android compilation is
checked separately by the APK job.
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
TOOLS = (ROOT / "app/src/main/java/app/buddy/assistant/Tools.java").read_text()
BRIDGE = (ROOT / "app/src/main/java/app/buddy/assistant/FieldsmithBridge.java").read_text()
GUIDE = (ROOT / "docs/GUIDE.md").read_text()

assert 'tool("fieldsmith"' in TOOLS, "Agent needs a built-in Fieldsmith tool"
assert 'case "fieldsmith":' in TOOLS, "Fieldsmith tool must be routed to an implementation"
assert 'FieldsmithBridge' in TOOLS, "Tool must call the native bridge"
assert '"status", "tools", "call"' in TOOLS, "Agent needs discover + execute operations"

assert 'http://127.0.0.1:18789/mcp' in BRIDGE, "Only localhost Fieldsmith MCP"
assert 'http://127.0.0.1:18789/api/mcp' in BRIDGE, "Fieldsmith workspace API"
assert 'McpServers.discover(' in BRIDGE, "List real Fieldsmith tools rather than invent schemas"
assert 'new URL(API_URL)' in BRIDGE, "Use the fixed local API endpoint"
assert 'setConnectTimeout(' in BRIDGE and 'setReadTimeout(' in BRIDGE, "Network timeouts required"
assert 'setInstanceFollowRedirects(false)' in BRIDGE, "No HTTP redirect out of loopback"
assert 'expected_revision' in BRIDGE, "Preserve workspace optimistic concurrency"
assert 'getErrorStream()' in BRIDGE, "Surface HTTP error responses"
assert 'MAX_RESPONSE_BYTES' in BRIDGE, "Bound server response sizes"
assert re.search(r'^[ ]*static String run\\(', BRIDGE, re.M), "Single action entrypoint"
assert 'Fieldsmith' in GUIDE and 'NOX01' in GUIDE, "User-facing voice CAD documentation"

print("PASS: Fieldsmith CAD tool is registered, scoped to loopback, and documented")
