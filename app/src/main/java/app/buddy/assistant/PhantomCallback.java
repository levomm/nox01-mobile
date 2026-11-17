package app.buddy.assistant;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/** Only the shape and freshness of a Phantom connect redirect, NOT cryptographic authentication. */
final class PhantomCallback {
    static final long TTL_MILLIS = 5 * 60 * 1000L;
    private static final int MAX_URL_LENGTH = 8192;

    static final class Result {
        final String phantomPublic;
        final String nonce;
        final String ciphertext;
        final boolean denied;

        Result(String phantomPublic, String nonce, String ciphertext, boolean denied) {
            this.phantomPublic = phantomPublic;
            this.nonce = nonce;
            this.ciphertext = ciphertext;
            this.denied = denied;
        }
    }

    private PhantomCallback() { }

    static Result parse(String uri, String expectedState, long startedMillis, long nowMillis) {
        if (uri == null || uri.length() > MAX_URL_LENGTH || expectedState == null ||
                expectedState.length() < 24 || startedMillis <= 0 || nowMillis < startedMillis ||
                nowMillis - startedMillis > TTL_MILLIS) {
            throw new IllegalArgumentException("Phantom response expired or malformed");
        }
        URI url;
        try { url = URI.create(uri); }
        catch (RuntimeException e) { throw new IllegalArgumentException("Invalid Phantom redirect", e); }
        if (!"nox01".equals(url.getScheme()) || !"phantom".equals(url.getHost()) ||
                !"/connect".equals(url.getPath()) || url.getPort() != -1 ||
                url.getRawUserInfo() != null || url.getRawFragment() != null) {
            throw new IllegalArgumentException("Untrusted Phantom callback origin");
        }
        String raw = url.getRawQuery();
        if (raw == null || raw.isEmpty()) throw new IllegalArgumentException("Missing Phantom query");
        Map<String, String> params = new HashMap<>();
        for (String part : raw.split("&", -1)) {
            int eq = part.indexOf('=');
            if (eq <= 0) throw new IllegalArgumentException("Malformed Phantom parameter");
            String key = decode(part.substring(0, eq));
            String val = decode(part.substring(eq + 1));
            if (params.putIfAbsent(key, val) != null) {
                throw new IllegalArgumentException("Duplicate Phantom callback parameter");
            }
        }
        String state = params.get("state");
        if (state == null || !MessageDigest.isEqual(
                state.getBytes(StandardCharsets.UTF_8), expectedState.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Phantom state mismatch");
        }
        if (params.containsKey("errorCode")) {
            if (params.containsKey("data") || params.containsKey("nonce") ||
                    params.containsKey("phantom_encryption_public_key")) {
                throw new IllegalArgumentException("Contradictory Phantom callback");
            }
            return new Result(null, null, null, true);
        }
        String key = params.get("phantom_encryption_public_key");
        String nonce = params.get("nonce");
        String ciphertext = params.get("data");
        if (key == null || key.isEmpty() || nonce == null || nonce.isEmpty() ||
                ciphertext == null || ciphertext.isEmpty()) {
            throw new IllegalArgumentException("Incomplete Phantom response");
        }
        return new Result(key, nonce, ciphertext, false);
    }

    private static String decode(String part) {
        try { return URLDecoder.decode(part, "UTF-8"); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid encoded parameter", e); }
    }
}
