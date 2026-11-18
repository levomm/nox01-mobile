package app.buddy.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Local, token-accurate API usage ledger. One completed agent turn yields one record.
 * Historical invoices/pricing are NOT available from a model-list endpoint: values shown
 * here are estimates from the current published prices and the latest USD/EUR FX rate.
 *
 * ChatGPT/Claude subscription runs are never charged at a made-up pay-per-token rate.
 * Each usage event has a unique ID, so a duplicated transport event is counted once.
 */
final class ApiSpend {
    private static final Object LOCK = new Object();
    private static final int KEEP_EVENTS = 20_000;
    private static final String PREF = "nox01_api_usage";

    private ApiSpend() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    static void record(Context c, Sessions.Session ses, JSONObject event) {
        if (ses == null || ses.id.isEmpty() || event == null) return;
        String usageId = event.optString("id", "");
        String modelRef = event.optString("model", Models.effective(ses));
        if (usageId.isEmpty()) return;
        // Only API events count toward monetary estimates.
        if (Providers.of(modelRef) == null) return;
        long input = Math.max(0, event.optLong("input", 0));
        long cached = Math.min(input, Math.max(0, event.optLong("cached", 0)));
        long output = Math.max(0, event.optLong("output", 0));
        if (input == 0 && output == 0) return;
        long timestamp = event.optLong("at", System.currentTimeMillis());
        if (timestamp < 1_600_000_000_000L || timestamp > System.currentTimeMillis() + 86_400_000L)
            timestamp = System.currentTimeMillis();
        String dayKey = day(timestamp);
        String monthKey = dayKey.substring(0, 7);
        String sessionId = ses.id;
        synchronized (LOCK) {
            try {
                JSONArray arr = new JSONArray(prefs(c).getString("events", "[]"));
                for (int i = arr.length() - 1; i >= 0; i--) {
                    JSONObject prev = arr.optJSONObject(i);
                    if (prev != null && usageId.equals(prev.optString("id"))) return;
                }
                JSONObject entry = new JSONObject().put("id", usageId).put("sessionId", sessionId)
                        .put("modelRef", modelRef).put("input", input).put("cached", cached)
                        .put("output", output).put("at", timestamp)
                        .put("dayKey", dayKey).put("monthKey", monthKey);
                arr.put(entry);
                if (arr.length() > KEEP_EVENTS) {
                    JSONArray pruned = new JSONArray();
                    for (int i = arr.length() - KEEP_EVENTS; i < arr.length(); i++) pruned.put(arr.get(i));
                    arr = pruned;
                }
                prefs(c).edit().putString("events", arr.toString()).apply();
            } catch (Exception ignored) {
                // Never crash a chat because usage tracking isn't available.
            }
        }
    }

    private static String day(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date(timestamp));
    }

    private static final class Totals {
        long input, cached, output, turns;
        int unavailable;
        double eur;
        JSONObject last;
    }

    private static Totals collect(Context c, String sessionId, String period) {
        Totals sum = new Totals();
        synchronized (LOCK) {
            try {
                JSONArray arr = new JSONArray(prefs(c).getString("events", "[]"));
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject e = arr.optJSONObject(i);
                    if (e == null) continue;
                    if (sessionId != null && !sessionId.equals(e.optString("sessionId"))) continue;
                    if (period != null && !period.equals(e.optString(period.length() == 7 ? "monthKey" : "dayKey"))) continue;
                    long input = Math.max(0, e.optLong("input"));
                    long cached = Math.min(input, Math.max(0, e.optLong("cached")));
                    long output = Math.max(0, e.optLong("output"));
                    sum.input += input;
                    sum.cached += cached;
                    sum.output += output;
                    sum.turns++;
                    sum.last = e;
                    String ref = e.optString("modelRef", "");
                    ModelPrices.Price p = ModelPrices.price(c, ref);
                    double fx = ModelPrices.fx(c);
                    if (p == null || !Double.isFinite(fx) || fx <= 0) {
                        sum.unavailable++;
                        continue;
                    }
                    // Codex input_tokens includes cached_input_tokens. Do not charge cached
                    // input at both the full rate and the discount rate.
                    double uncached = Math.max(0, input - cached);
                    double cost = (uncached * p.inputUsd + cached * p.cachedUsd
                            + output * p.outputUsd) / 1_000_000.0 * fx;
                    if (Double.isFinite(cost) && cost >= 0) sum.eur += cost;
                    else sum.unavailable++;
                }
            } catch (Exception ignored) {
            }
        }
        return sum;
    }

    private static String money(Context c, Totals t) {
        if (t.turns == 0) return c.getString(R.string.spend_no_data);
        if (t.unavailable == t.turns) return c.getString(R.string.spend_unknown);
        return "≈ " + ModelPrices.euro(t.eur) + (t.unavailable > 0 ? " +" : "");
    }

    /** Small badge in the chat corner, directly above the message composer. */
    static String summary(Context c, Sessions.Session ses) {
        if (Providers.of(Models.effective(ses)) == null) return c.getString(R.string.spend_plan);
        Totals chat = collect(c, ses.id, null);
        if (chat.turns == 0) return c.getString(R.string.spend_no_data);
        return money(c, chat);
    }

    /** Compact, human-readable drilldown for last request, chat, today and this month. */
    static String detail(Context c, Sessions.Session ses) {
        Totals chat = collect(c, ses.id, null);
        String today = day(System.currentTimeMillis());
        Totals daily = collect(c, null, today);
        Totals monthly = collect(c, null, today.substring(0, 7));
        Totals latest = new Totals();
        if (chat.last != null) {
            JSONObject e = chat.last;
            ModelPrices.Price p = ModelPrices.price(c, e.optString("modelRef", ""));
            double fx = ModelPrices.fx(c);
            latest.turns = 1;
            latest.input = Math.max(0, e.optLong("input"));
            latest.cached = Math.min(latest.input, Math.max(0, e.optLong("cached")));
            latest.output = Math.max(0, e.optLong("output"));
            if (p == null || !Double.isFinite(fx) || fx <= 0) latest.unavailable = 1;
            else latest.eur = ((latest.input - latest.cached) * p.inputUsd
                    + latest.cached * p.cachedUsd + latest.output * p.outputUsd) / 1_000_000.0 * fx;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(c.getString(R.string.spend_last)).append(": ").append(money(c, latest)).append('\n');
        sb.append(c.getString(R.string.spend_chat)).append(": ").append(money(c, chat)).append('\n');
        sb.append(c.getString(R.string.spend_day)).append(": ").append(money(c, daily)).append('\n');
        sb.append(c.getString(R.string.spend_month)).append(": ").append(money(c, monthly)).append("\n\n");
        sb.append(c.getString(R.string.spend_input)).append(": ").append(Models.tokens(chat.input - chat.cached)).append('\n');
        sb.append(c.getString(R.string.spend_cached)).append(": ").append(Models.tokens(chat.cached)).append('\n');
        sb.append(c.getString(R.string.spend_output)).append(": ").append(Models.tokens(chat.output)).append('\n');
        sb.append(c.getString(R.string.spend_turns, chat.turns)).append("\n\n");
        sb.append(ModelPrices.lastUpdate(c)).append('\n');
        sb.append(c.getString(R.string.spend_estimate));
        if (chat.unavailable > 0) sb.append("\n").append(c.getString(R.string.spend_partial, chat.unavailable));
        return sb.toString();
    }
}
