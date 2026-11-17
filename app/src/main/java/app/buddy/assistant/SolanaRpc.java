package app.buddy.assistant;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Strictly read-only Solana mainnet RPC client. Never accepts transaction payloads. */
final class SolanaRpc {
    static final String ENDPOINT = "https://api.mainnet-beta.solana.com";
    static final String CIRCLE_USDC_MINT = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v";
    static final BigInteger LAMPORTS_PER_SOL = BigInteger.TEN.pow(9);
    static final BigInteger UNITS_PER_USDC = BigInteger.TEN.pow(6);
    private static final int MAX_REPLY = 2 * 1024 * 1024;
    private final URL endpoint;

    SolanaRpc() throws Exception {
        this(ENDPOINT);
    }

    /** Only a HTTPS RPC URL is accepted, including when a custom RPC is added later. */
    SolanaRpc(String url) throws Exception {
        endpoint = new URL(url);
        if (!"https".equalsIgnoreCase(endpoint.getProtocol()) || endpoint.getUserInfo() != null
                || endpoint.getHost() == null || endpoint.getHost().isEmpty()) {
            throw new IllegalArgumentException("HTTPS Solana RPC address required");
        }
    }

    static final class Balance {
        final BigInteger solLamports;
        final BigInteger usdcUnits;
        final long fetchedAtMillis;

        Balance(BigInteger solLamports, BigInteger usdcUnits) {
            this.solLamports = solLamports;
            this.usdcUnits = usdcUnits;
            this.fetchedAtMillis = System.currentTimeMillis();
        }
    }

    static final class History {
        final String signature;
        final long blockTime;
        final boolean failed;
        final String account;

        History(String signature, long blockTime, boolean failed, String account) {
            this.signature = signature;
            this.blockTime = blockTime;
            this.failed = failed;
            this.account = account;
        }
    }

    Balance getBalances(String address) throws Exception {
        checkAddress(address);
        // The UI updates balances only if BOTH calls succeed, so errors cannot look like zero.
        BigInteger sol = getSolBalance(address);
        BigInteger usdc = getUsdcBalance(address);
        return new Balance(sol, usdc);
    }

    BigInteger getSolBalance(String address) throws Exception {
        checkAddress(address);
        JSONObject result = getResult("getBalance", new JSONArray()
                .put(address).put(new JSONObject().put("commitment", "confirmed")));
        BigInteger amount = new BigInteger(result.get("value").toString());
        if (amount.signum() < 0) throw new IllegalStateException("Invalid negative SOL balance");
        return amount;
    }

    BigInteger getUsdcBalance(String address) throws Exception {
        checkAddress(address);
        JSONArray accounts = tokenAccounts(address);
        BigInteger units = BigInteger.ZERO;
        for (int i = 0; i < accounts.length(); i++) {
            JSONObject a = accounts.getJSONObject(i);
            JSONObject info = a.getJSONObject("account").getJSONObject("data")
                    .getJSONObject("parsed").getJSONObject("info");
            if (!CIRCLE_USDC_MINT.equals(info.getString("mint"))
                    || !address.equals(info.getString("owner"))) {
                throw new IllegalStateException("RPC returned an unexpected USDC account");
            }
            JSONObject amount = info.getJSONObject("tokenAmount");
            if (amount.getInt("decimals") != 6) {
                throw new IllegalStateException("Unexpected USDC decimal precision");
            }
            BigInteger atomic = new BigInteger(amount.getString("amount"));
            if (atomic.signum() < 0) throw new IllegalStateException("Negative token balance");
            units = units.add(atomic);
        }
        return units;
    }

    private JSONArray tokenAccounts(String address) throws Exception {
        JSONObject result = getResult("getTokenAccountsByOwner", new JSONArray().put(address)
                .put(new JSONObject().put("mint", CIRCLE_USDC_MINT))
                .put(new JSONObject().put("encoding", "jsonParsed").put("commitment", "confirmed")));
        return result.getJSONArray("value");
    }

    /** Real on-chain signatures only; directions and fiat values are intentionally not invented. */
    List<History> getRecentSignatures(String address) throws Exception {
        checkAddress(address);
        List<String> accounts = new ArrayList<>();
        accounts.add(address);
        JSONArray tokenAccounts = tokenAccounts(address);
        for (int i = 0; i < tokenAccounts.length(); i++) {
            String token = tokenAccounts.getJSONObject(i).getString("pubkey");
            if (!SolanaAddress.isValid(token)) throw new IllegalStateException("Unexpected token account");
            accounts.add(token);
        }
        // Public RPC rate limits: avoid unbounded fan-out for owners with many token accounts.
        if (accounts.size() > 9) throw new IllegalStateException("Too many USDC accounts for public RPC history");
        List<History> history = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String account : accounts) {
            JSONArray records = getArray("getSignaturesForAddress", new JSONArray()
                    .put(account).put(new JSONObject().put("limit", 20).put("commitment", "confirmed")));
            for (int i = 0; i < records.length(); i++) {
                JSONObject r = records.getJSONObject(i);
                String signature = r.getString("signature");
                if (!signature.matches("[1-9A-HJ-NP-Za-km-z]{64,88}") || !seen.add(signature)) continue;
                history.add(new History(signature, r.optLong("blockTime", 0),
                        !r.isNull("err"), account));
            }
        }
        history.sort(Comparator.comparingLong((History h) -> h.blockTime).reversed());
        return new ArrayList<>(history.subList(0, Math.min(20, history.size())));
    }

    private static void checkAddress(String address) {
        if (!SolanaAddress.isValid(address)) throw new IllegalArgumentException("Invalid Solana public key");
    }

    private JSONObject getResult(String method, JSONArray params) throws Exception {
        Object result = call(method, params);
        if (!(result instanceof JSONObject)) throw new IllegalStateException("Malformed RPC result");
        return (JSONObject) result;
    }

    private JSONArray getArray(String method, JSONArray params) throws Exception {
        Object result = call(method, params);
        if (!(result instanceof JSONArray)) throw new IllegalStateException("Malformed RPC array");
        return (JSONArray) result;
    }

    private Object call(String method, JSONArray params) throws Exception {
        // No write methods are accepted anywhere in this class.
        if (!"getBalance".equals(method) && !"getTokenAccountsByOwner".equals(method)
                && !"getSignaturesForAddress".equals(method)) {
            throw new SecurityException("Wallet RPC is read-only");
        }
        JSONObject body = new JSONObject().put("jsonrpc", "2.0").put("id", 1)
                .put("method", method).put("params", params);
        HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
        try {
            conn.setConnectTimeout(9000);
            conn.setReadTimeout(14000);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "application/json");
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = conn.getOutputStream()) { out.write(bytes); }
            int code = conn.getResponseCode();
            if (code == 429) throw new IllegalStateException("Solana RPC limit reached, retry later");
            if (code < 200 || code >= 300) throw new IllegalStateException("Solana RPC HTTP " + code);
            byte[] received;
            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                byte[] chunk = new byte[8192];
                int count;
                while ((count = in.read(chunk)) != -1) {
                    if (buffer.size() + count > MAX_REPLY) throw new IllegalStateException("RPC reply too large");
                    buffer.write(chunk, 0, count);
                }
                received = buffer.toByteArray();
            }
            JSONObject root = new JSONObject(new String(received, StandardCharsets.UTF_8));
            if (!root.isNull("error")) {
                JSONObject err = root.getJSONObject("error");
                throw new IllegalStateException("Solana RPC error " + err.optInt("code"));
            }
            return root.get("result");
        } finally {
            conn.disconnect();
        }
    }
}
