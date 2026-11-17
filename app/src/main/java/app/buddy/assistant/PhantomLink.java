package app.buddy.assistant;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Base64;

import com.iwebpp.crypto.TweetNaclFast;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Phantom's official Solana mobile connect deeplink, not a browser JS injection.
 * Phantom's X25519/XSalsa20-Poly1305 reply is authenticated BEFORE we set the address.
 */
final class PhantomLink {
    private static final String CONNECT_URL = "https://phantom.app/ul/v1/connect";
    private static final String APP_URL = "https://levomm.github.io/nox01-mobile/";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PhantomLink() { }

    /** Only call from a human-facing UI button; never expose as a tool or scheduled task. */
    static void beginConnect(Activity activity) {
        TweetNaclFast.Box.KeyPair pair = TweetNaclFast.Box.keyPair();
        byte[] stateBytes = new byte[32];
        RANDOM.nextBytes(stateBytes);
        String state = Base64.encodeToString(stateBytes,
                Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);
        WalletStore.savePending(activity, state, SystemClock.elapsedRealtime(),
                pair.getSecretKey(), pair.getPublicKey());
        Arrays.fill(pair.getSecretKey(), (byte) 0);
        Uri redirect = new Uri.Builder().scheme("nox01").authority("phantom")
                .appendPath("connect").appendQueryParameter("state", state).build();
        Uri request = Uri.parse(CONNECT_URL).buildUpon()
                .appendQueryParameter("app_url", APP_URL)
                .appendQueryParameter("dapp_encryption_public_key", SolanaBase58.encode(pair.getPublicKey()))
                .appendQueryParameter("redirect_link", redirect.toString())
                .appendQueryParameter("cluster", "mainnet-beta").build();
        Intent intent = new Intent(Intent.ACTION_VIEW, request);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        try {
            activity.startActivity(intent);
        } catch (RuntimeException e) {
            WalletStore.clearPending(activity);
            throw new IllegalStateException("Phantom is unavailable", e);
        }
    }

    /** Called ONLY by WalletCallbackActivity after the system redirects back. */
    static boolean handleConnectRedirect(Context context, Uri redirect) {
        WalletStore.Pending pending = WalletStore.pending(context);
        if (pending == null) throw new IllegalArgumentException("No Phantom connection pending");
        try {
            // Includes constant-time state comparison and monotonic elapsed-time window.
            PhantomCallback.Result callback = PhantomCallback.parse(
                    redirect == null ? null : redirect.toString(), pending.state,
                    pending.startedElapsedMillis, SystemClock.elapsedRealtime());
            // A valid state is single-use, whether Phantom approved or the user denied.
            WalletStore.clearPending(context);
            if (callback.denied) return false;
            byte[] phantomPublic = SolanaBase58.decode(callback.phantomPublic);
            byte[] nonce = SolanaBase58.decode(callback.nonce);
            byte[] ciphertext = SolanaBase58.decode(callback.ciphertext);
            if (phantomPublic.length != 32 || nonce.length != 24 ||
                    ciphertext.length < 16 || ciphertext.length > 6144) {
                throw new IllegalArgumentException("Invalid encrypted Phantom reply sizes");
            }
            TweetNaclFast.Box box = new TweetNaclFast.Box(phantomPublic, pending.dappSecret);
            byte[] plaintext = box.open(ciphertext, nonce);
            if (plaintext == null || plaintext.length == 0 || plaintext.length > 4096) {
                throw new IllegalArgumentException("Phantom reply failed authenticated decryption");
            }
            try {
                JSONObject approved = new JSONObject(new String(plaintext, StandardCharsets.UTF_8));
                String address = approved.getString("public_key");
                String session = approved.getString("session");
                if (!SolanaAddress.isValid(address) || session.isEmpty() || session.length() > 4096) {
                    throw new IllegalArgumentException("Phantom returned an invalid public wallet address");
                }
                WalletStore.saveConnected(context, address, session,
                        pending.dappSecret, pending.dappPublic, phantomPublic);
                return true;
            } catch (org.json.JSONException e) {
                throw new IllegalArgumentException("Invalid authenticated Phantom reply structure", e);
            } finally {
                Arrays.fill(plaintext, (byte) 0);
            }
        } finally {
            Arrays.fill(pending.dappSecret, (byte) 0);
        }
    }
}
