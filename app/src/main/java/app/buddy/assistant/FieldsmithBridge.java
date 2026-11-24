package app.buddy.assistant;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * The built-in Fieldsmith CAD workbench tool.
 *
 * Fieldsmith owns the geometry and solver. NOX01 only connects to its loopback
 * MCP server, reads the tool catalog, and relays an explicitly named operation.
 * No API key, external host, or second MCP-server configuration is needed.
 */
final class FieldsmithBridge {
    private static final String MCP_URL = "http://127.0.0.1:18789/mcp";
    private static final String API_URL = "http://127.0.0.1:18789/api/mcp";
    private static final String HEALTH_URL = "http://127.0.0.1:18789/api/health";
    private static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;
    private static final int MAX_REQUEST_BYTES = 128 * 1024;
    private static final int MAX_TOOL_OUTPUT_CHARS = 24_000;
    private static final String NOT_READY =
            "Fieldsmith ei vasta telefonis aadressil 127.0.0.1:18789. "
            + "Kui see on paigaldatud, ava see NOX01 open_app-tööriistaga "
            + "(app: sh.fieldsmith.app), lase tal käivituda ja proovi uuesti. "
            + "Kui see puudub, tuleb Fieldsmith esmalt paigaldada.";

    private FieldsmithBridge() {}

    /** Dispatches one native NOX01 phone tool call. */
    static String run(JSONObject a) throws Exception {
        String action = a.optString("action", "status");
        try {
            switch (action) {
                case "status":
                    JSONObject health = request("GET", HEALTH_URL, null, 2_500);
                    if (!"ok".equals(health.optString("status"))) {
                        return "Fieldsmith ei ole valmis: " + compact(health.toString());
                    }
                    return "Fieldsmith on saadaval. " + compact(new JSONObject()
                            .put("version", health.optString("version"))
                            .put("workspace_revision", health.optLong("workspace_revision", -1))
                            .put("max_recommended_dofs", health.optLong("max_recommended_dofs", -1))
                            .put("capabilities", health.optJSONArray("capabilities"))
                            .toString());

                case "tools": {
                    JSONArray catalog = McpServers.discover(MCP_URL, null);
                    int offset = Math.max(0, Math.min(a.optInt("offset", 0), catalog.length()));
                    int limit = Math.max(1, Math.min(a.optInt("limit", 20), 40));
                    JSONArray page = new JSONArray();
                    for (int i = offset; i < Math.min(catalog.length(), offset + limit); i++) {
                        page.put(catalog.getJSONObject(i));
                    }
                    return compact(new JSONObject().put("total", catalog.length())
                            .put("offset", offset)
                            .put("next_offset", offset + page.length() < catalog.length()
                                    ? offset + page.length() : JSONObject.NULL)
                            .put("tools", page).toString());
                }

                case "call": {
                    String tool = a.optString("tool", "").trim();
                    if (!tool.matches("[a-z][a-z0-9_]{0,63}")) {
                        return "Anna Fieldsmithi tööriista nimi. Küsi neid action=tools abil.";
                    }
                    JSONObject arguments = a.optJSONObject("arguments");
                    if (arguments == null) {
                        if (a.has("arguments") && !a.isNull("arguments")) {
                            return "Fieldsmithi arguments peab olema JSON-objekt.";
                        }
                        arguments = new JSONObject();
                    }
                    JSONObject body = new JSONObject().put("tool", tool).put("arguments", arguments);
                    if (a.has("expected_revision") && !a.isNull("expected_revision")) {
                        long revision = a.optLong("expected_revision", -1);
                        if (revision < 0) return "expected_revision peab olema 0 või suurem.";
                        body.put("expected_revision", revision);
                    }
                    JSONObject response = request("POST", API_URL, body, 600_000);
                    JSONObject rpc = response.optJSONObject("rpc");
                    if (rpc == null) return "Fieldsmith vastas ootamatu kujuga: " + compact(response.toString());
                    JSONObject workspace = response.optJSONObject("workspace");
                    String revisionText = workspace == null ? "" : "workspace_revision="
                            + workspace.optLong("revision", -1) + "\n";
                    JSONObject error = rpc.optJSONObject("error");
                    if (error != null) {
                        return "Fieldsmith tööriista viga: " + revisionText + compact(error.toString());
                    }
                    JSONObject result = rpc.optJSONObject("result");
                    if (result == null) return revisionText + compact(rpc.toString());
                    JSONArray chunks = result.optJSONArray("content");
                    StringBuilder answer = new StringBuilder(revisionText);
                    if (result.optBoolean("isError")) answer.append("Fieldsmith raporteeris vea.\n");
                    for (int i = 0; chunks != null && i < chunks.length(); i++) {
                        JSONObject chunk = chunks.optJSONObject(i);
                        if (chunk == null) continue;
                        String text = chunk.optString("text", "");
                        if (!text.isEmpty()) answer.append(text).append('\n');
                        if (answer.length() > MAX_TOOL_OUTPUT_CHARS) break;
                    }
                    if (chunks == null || chunks.length() == 0) answer.append(result.toString());
                    return compact(answer.toString());
                }

                default:
                    return "Tundmatu Fieldsmithi tegevus. Kasuta status, tools või call.";
            }
        } catch (java.net.ConnectException e) {
            return NOT_READY;
        } catch (java.net.SocketTimeoutException e) {
            return "Fieldsmith ei vastanud õigeaegselt. Kontrolli, kas rakendus on avatud. "
                    + "Ära korda solverit pimesi, vaid küsi esmalt status.";
        } catch (java.io.IOException e) {
            return NOT_READY + " Tehniline viga: " + e.getMessage();
        }
    }

    private static JSONObject request(String method, String url, JSONObject payload, int timeoutMs) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(Math.min(timeoutMs, 10_000));
        conn.setReadTimeout(timeoutMs);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestProperty("Accept", "application/json");
        try {
            if (payload != null) {
                byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
                if (body.length > MAX_REQUEST_BYTES) {
                    throw new IllegalArgumentException("Fieldsmith request too large");
                }
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setFixedLengthStreamingMode(body.length);
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(body);
                }
            }

            int code = conn.getResponseCode();
            InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            if (stream != null) {
                try (InputStream in = stream) {
                    byte[] buf = new byte[8192];
                    for (int n; (n = in.read(buf)) != -1; ) {
                        if (bytes.size() + n > MAX_RESPONSE_BYTES) {
                            throw new IllegalArgumentException("Fieldsmith response too large");
                        }
                        bytes.write(buf, 0, n);
                    }
                }
            }
            String raw = bytes.toString("UTF-8");
            if (code >= 400) throw new IllegalStateException("Fieldsmith HTTP " + code + ": " + compact(raw));
            return new JSONObject(raw);
        } finally {
            conn.disconnect();
        }
    }

    private static String compact(String s) {
        if (s.length() <= MAX_TOOL_OUTPUT_CHARS) return s;
        return s.substring(0, MAX_TOOL_OUTPUT_CHARS) + "\n[Fieldsmith output truncated]";
    }
}
