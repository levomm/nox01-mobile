package app.buddy.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Public approximate per-million-token prices from models.dev, converted using
 * Frankfurter's USD/EUR rate. Not an invoice or an estimate of subscription charges.
 *
 * Catalog (~7 MB over the wire) is fetched at most daily, parsed off the UI thread,
 * and reduced to known connected models before caching it on this device. Unknown
 * models stay unpriced; their numbers are never guessed.
 */
final class ModelPrices {
    static final String CATALOG_URL = "https://models.dev/api.json";
    static final String FX_URL = "https://api.frankfurter.dev/v1/latest?base=USD&symbols=EUR";
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final long RETRY_MS = 15L * 60L * 1000L;
    private static final Object LOCK = new Object();
    private static final Map<String, Price> CACHE = new HashMap<>();
    private static boolean loaded, loading;
    private static long updatedAt, fxUpdatedAt, lastAttempt;
    private static double eurPerUsd;

    static final class Price {
        final String providerId, modelId;
        final double inputUsd, outputUsd, cachedUsd;
        final long updatedAt;

        Price(String providerId, String modelId, double inputUsd, double outputUsd, double cachedUsd, long updatedAt) {
            this.providerId = providerId;
            this.modelId = modelId;
            this.inputUsd = inputUsd;
            this.outputUsd = outputUsd;
            this.cachedUsd = cachedUsd;
            this.updatedAt = updatedAt;
        }

        /** Benchmark for ranking: one million uncached input and one million output tokens. */
        double compareUsd() {
            return inputUsd + outputUsd;
        }
    }

    private ModelPrices() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("nox01_model_prices", Context.MODE_PRIVATE);
    }

    private static String key(String providerId, String modelId) {
        return providerId + ":" + modelId;
    }

    private static void load(Context c) {
        synchronized (LOCK) {
            if (loaded) return;
            loaded = true;
            SharedPreferences sp = prefs(c);
            updatedAt = sp.getLong("prices_at", 0);
            fxUpdatedAt = sp.getLong("fx_at", 0);
            eurPerUsd = Double.longBitsToDouble(sp.getLong("fx_bits", Double.doubleToRawLongBits(0.0)));
            try {
                JSONObject root = new JSONObject(sp.getString("catalog", "{}"));
                for (java.util.Iterator<String> it = root.keys(); it.hasNext();) {
                    String k = it.next();
                    JSONObject v = root.optJSONObject(k);
                    if (v == null) continue;
                    String[] parts = k.split(":", 2);
                    if (parts.length != 2) continue;
                    CACHE.put(k, new Price(parts[0], parts[1], v.optDouble("i", Double.NaN),
                            v.optDouble("o", Double.NaN), v.optDouble("c", Double.NaN), updatedAt));
                }
            } catch (Exception ignored) {
                CACHE.clear();
            }
        }
    }

    static String providerId(Providers.P p) {
        if (p == null) return "";
        String name = p.name.trim().toLowerCase(Locale.ROOT);
        String host = p.host().toLowerCase(Locale.ROOT);
        if ("openai".equals(name) || host.equals("api.openai.com")) return "openai";
        if ("anthropic".equals(name) || host.equals("api.anthropic.com")) return "anthropic";
        if ("deepseek".equals(name) || host.contains("deepseek.com")) return "deepseek";
        if ("openrouter".equals(name) || host.contains("openrouter.ai")) return "openrouter";
        if ("google".equals(name) || "gemini".equals(name) || host.contains("generativelanguage.googleapis.com")) return "google";
        if ("xai".equals(name) || "grok".equals(name) || host.contains("api.x.ai")) return "xai";
        return ""; // custom providers are not necessarily priced like the upstream model
    }

    static Price price(Context c, String modelRef) {
        Providers.P p = Providers.of(modelRef);
        if (p == null || p.off) return null;
        load(c);
        String provider = providerId(p);
        String model = Providers.model(modelRef);
        synchronized (LOCK) {
            Price exact = CACHE.get(key(provider, model));
            if (exact != null) return exact;
            // OpenAI dated model aliases commonly have the same list price as their base model.
            String base = model.replaceFirst("-20[0-9]{2}-[0-9]{2}-[0-9]{2}$", "");
            return !base.equals(model) ? CACHE.get(key(provider, base)) : null;
        }
    }

    static double fx(Context c) {
        load(c);
        synchronized (LOCK) {
            return eurPerUsd > 0 && Double.isFinite(eurPerUsd) ? eurPerUsd : Double.NaN;
        }
    }

    static String costPerMillion(Context c, String ref) {
        Price p = price(c, ref);
        double fx = fx(c);
        if (p == null || !Double.isFinite(fx) || !Double.isFinite(p.inputUsd) || !Double.isFinite(p.outputUsd))
            return c.getString(R.string.spend_unknown);
        return c.getString(R.string.chat_prices_per_million,
                euro(p.inputUsd * fx), euro(p.outputUsd * fx));
    }

    static String euro(double amount) {
        if (!Double.isFinite(amount) || amount < 0) return "—";
        int decimals = amount < 0.01 ? 5 : amount < 1 ? 4 : 2;
        return String.format(Locale.forLanguageTag("et-EE"), "%." + decimals + "f €", amount);
    }

    static String lastUpdate(Context c) {
        load(c);
        synchronized (LOCK) {
            if (updatedAt == 0 || fxUpdatedAt == 0) return c.getString(R.string.chat_prices_unavailable);
            String day = new SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("et-EE"))
                    .format(new Date(Math.min(updatedAt, fxUpdatedAt)));
            return c.getString(R.string.chat_prices_source, day);
        }
    }

    static void refresh(Context c, Runnable onDone) {
        Context app = c.getApplicationContext();
        load(app);
        synchronized (LOCK) {
            long now = System.currentTimeMillis();
            if (loading || (now - updatedAt < DAY_MS && now - fxUpdatedAt < DAY_MS)
                    || now - lastAttempt < RETRY_MS) return;
            loading = true;
            lastAttempt = now;
        }
        new Thread(() -> {
            boolean changed = false;
            try {
                // Catalog only contains publicly published prices, never the user's secret API key.
                JSONObject catalog = new JSONObject(download(CATALOG_URL, 12_000_000));
                long fetched = System.currentTimeMillis();
                Map<String, Price> next = new HashMap<>();
                // Only cache provider/model pairs that NOX01 actually offers. This is not
                // a claim that all 324 providers' prices match an arbitrary custom API.
                for (Providers.P p : Providers.all()) {
                    String provider = providerId(p);
                    if (provider.isEmpty()) continue;
                    JSONObject models = catalog.optJSONObject(provider);
                    models = models == null ? null : models.optJSONObject("models");
                    if (models == null) continue;
                    for (int i = 0; i < p.models.length(); i++) {
                        JSONObject m = p.models.optJSONObject(i);
                        if (m == null) continue;
                        String model = m.optString("id");
                        JSONObject info = models.optJSONObject(model);
                        if (info == null) {
                            String base = model.replaceFirst("-20[0-9]{2}-[0-9]{2}-[0-9]{2}$", "");
                            info = models.optJSONObject(base);
                        }
                        if (info == null) continue;
                        JSONObject cost = info.optJSONObject("cost");
                        if (cost == null) continue;
                        JSONObject textCost = cost.optJSONObject("text");
                        if (textCost != null) cost = textCost;
                        double in = cost.optDouble("input", Double.NaN);
                        double out = cost.optDouble("output", Double.NaN);
                        if (!(in >= 0 && out >= 0) || !Double.isFinite(in) || !Double.isFinite(out)) continue;
                        double cache = cost.has("cache_read") ? cost.optDouble("cache_read", in) : in;
                        if (!Double.isFinite(cache) || cache < 0) cache = in;
                        next.put(key(provider, model), new Price(provider, model, in, out, cache, fetched));
                    }
                }
                JSONObject serialized = new JSONObject();
                for (Map.Entry<String, Price> e : next.entrySet()) {
                    Price p = e.getValue();
                    serialized.put(e.getKey(), new JSONObject()
                            .put("i", p.inputUsd).put("o", p.outputUsd).put("c", p.cachedUsd));
                }
                synchronized (LOCK) {
                    CACHE.clear();
                    CACHE.putAll(next);
                    updatedAt = fetched;
                    prefs(app).edit().putString("catalog", serialized.toString())
                            .putLong("prices_at", updatedAt).apply();
                }
                changed = true;
            } catch (Exception ignored) { // old cache remains usable when offline
            }
            try {
                JSONObject rate = new JSONObject(download(FX_URL, 16_000));
                double fx = rate.optJSONObject("rates").optDouble("EUR", Double.NaN);
                if (Double.isFinite(fx) && fx > 0) {
                    synchronized (LOCK) {
                        eurPerUsd = fx;
                        fxUpdatedAt = System.currentTimeMillis();
                        prefs(app).edit().putLong("fx_bits", Double.doubleToRawLongBits(fx))
                                .putLong("fx_at", fxUpdatedAt).apply();
                    }
                    changed = true;
                }
            } catch (Exception ignored) { // no fabricated exchange rate
            }
            synchronized (LOCK) { loading = false; }
            if (changed && onDone != null) new Handler(Looper.getMainLooper()).post(onDone);
        }, "nox01-prices").start();
    }

    private static String download(String url, int maxBytes) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(25_000);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Accept-Encoding", "gzip");
        try {
            if (conn.getResponseCode() != 200) throw new java.io.IOException("HTTP " + conn.getResponseCode());
            InputStream raw = conn.getInputStream();
            try (InputStream in = "gzip".equalsIgnoreCase(conn.getContentEncoding())
                         ? new java.util.zip.GZIPInputStream(raw) : raw;
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    if (out.size() + n > maxBytes) throw new java.io.IOException("price catalog too large");
                    out.write(buf, 0, n);
                }
                return out.toString("UTF-8");
            }
        } finally { conn.disconnect(); }
    }
}
