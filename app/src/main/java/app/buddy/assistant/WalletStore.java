package app.buddy.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Wallet storage: public watch address, and Phantom credentials encrypted by Android Keystore.
 * Never stores a mnemonic or the user's wallet private key. All secret-store writes are synchronous.
 */
final class WalletStore {
    private static final String FILE = "nox01_wallet_public_v1";
    private static final String ADDRESS = "solana_watch_address";
    private static final String ORIGIN = "source";
    private static final String PENDING = "encrypted_phantom_pending";
    private static final String SESSION = "encrypted_phantom_session";
    private static final String KEY_ALIAS = "ee.osx01.mobile.phantom.link.v1";

    static final class Pending {
        final String state;
        final long startedElapsedMillis;
        final byte[] dappSecret;
        final byte[] dappPublic;

        Pending(String state, long startedElapsedMillis, byte[] dappSecret, byte[] dappPublic) {
            this.state = state;
            this.startedElapsedMillis = startedElapsedMillis;
            this.dappSecret = dappSecret;
            this.dappPublic = dappPublic;
        }
    }

    static final class Connection {
        final String address;
        final String session;
        final byte[] dappSecret;
        final byte[] dappPublic;
        final byte[] phantomPublic;

        Connection(String address, String session, byte[] dappSecret,
                   byte[] dappPublic, byte[] phantomPublic) {
            this.address = address;
            this.session = session;
            this.dappSecret = dappSecret;
            this.dappPublic = dappPublic;
            this.phantomPublic = phantomPublic;
        }
    }

    private WalletStore() { }

    static String address(Context context) {
        String value = prefs(context).getString(ADDRESS, "");
        return SolanaAddress.isValid(value) ? value : "";
    }

    static boolean isWatchOnly(Context context) {
        return "manual".equals(prefs(context).getString(ORIGIN, ""));
    }

    static boolean isPhantomConnected(Context context) {
        if (!"phantom".equals(prefs(context).getString(ORIGIN, ""))) return false;
        try { return connection(context) != null; }
        catch (Exception e) { return false; }
    }

    static synchronized void saveWatchAddress(Context context, String address) {
        if (!SolanaAddress.isValid(address)) {
            throw new IllegalArgumentException("Invalid Solana public address");
        }
        if (!prefs(context).edit().putString(ADDRESS, address)
                .putString(ORIGIN, "manual").remove(SESSION).remove(PENDING).commit()) {
            throw new IllegalStateException("Cannot save wallet address");
        }
    }

    static synchronized void savePending(Context c, String state, long startedElapsedMillis,
                                          byte[] dappSecret, byte[] dappPublic) {
        if (state == null || state.length() < 24 || startedElapsedMillis <= 0 ||
                dappSecret == null || dappSecret.length != 32 ||
                dappPublic == null || dappPublic.length != 32) {
            throw new IllegalArgumentException("Invalid Phantom connect request");
        }
        try {
            JSONObject pending = new JSONObject().put("state", state).put("elapsed", startedElapsedMillis)
                    .put("sk", b64(dappSecret)).put("pk", b64(dappPublic));
            String encrypted = PhantomSecretBox.seal(secretKey(true),
                    pending.toString().getBytes(StandardCharsets.UTF_8));
            if (!prefs(c).edit().putString(PENDING, encrypted).commit()) {
                throw new IllegalStateException("Cannot persist pending Phantom session");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not secure Phantom session", e);
        }
    }

    static synchronized Pending pending(Context c) {
        String encrypted = prefs(c).getString(PENDING, null);
        if (encrypted == null) return null;
        try {
            JSONObject pending = readSecure(encrypted);
            byte[] sk = from64(pending.getString("sk"));
            byte[] pk = from64(pending.getString("pk"));
            if (sk.length != 32 || pk.length != 32) throw new IllegalStateException("Invalid link keys");
            return new Pending(pending.getString("state"), pending.getLong("elapsed"), sk, pk);
        } catch (Exception e) {
            // Fail closed: never use a corrupt session, and never downgrade to plaintext.
            prefs(c).edit().remove(PENDING).commit();
            throw new IllegalStateException("Cannot read encrypted Phantom connect request", e);
        }
    }

    static synchronized void clearPending(Context c) {
        prefs(c).edit().remove(PENDING).commit();
    }

    static synchronized void saveConnected(Context c, String address, String session,
                                            byte[] dappSecret, byte[] dappPublic,
                                            byte[] phantomPublic) {
        if (!SolanaAddress.isValid(address) || session == null || session.isEmpty() ||
                session.length() > 4096 || dappSecret == null || dappSecret.length != 32 ||
                dappPublic == null || dappPublic.length != 32 ||
                phantomPublic == null || phantomPublic.length != 32) {
            throw new IllegalArgumentException("Invalid Phantom connection");
        }
        try {
            JSONObject payload = new JSONObject().put("session", session).put("sk", b64(dappSecret))
                    .put("pk", b64(dappPublic)).put("phantom_pk", b64(phantomPublic))
                    .put("address", address);
            String encrypted = PhantomSecretBox.seal(secretKey(true),
                    payload.toString().getBytes(StandardCharsets.UTF_8));
            if (!prefs(c).edit().putString(SESSION, encrypted)
                    .putString(ADDRESS, address).putString(ORIGIN, "phantom")
                    .remove(PENDING).commit()) {
                throw new IllegalStateException("Cannot persist wallet connection");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot secure Phantom connection", e);
        }
    }

    static synchronized Connection connection(Context c) {
        if (!"phantom".equals(prefs(c).getString(ORIGIN, ""))) return null;
        String encrypted = prefs(c).getString(SESSION, null);
        if (encrypted == null) return null;
        try {
            JSONObject session = readSecure(encrypted);
            String address = session.getString("address");
            byte[] sk = from64(session.getString("sk"));
            byte[] pk = from64(session.getString("pk"));
            byte[] phantom = from64(session.getString("phantom_pk"));
            if (!SolanaAddress.isValid(address) || !address.equals(address(c)) ||
                    sk.length != 32 || pk.length != 32 || phantom.length != 32 ||
                    session.getString("session").isEmpty()) {
                throw new IllegalStateException("Phantom session integrity failed");
            }
            return new Connection(address, session.getString("session"), sk, pk, phantom);
        } catch (Exception e) {
            throw new IllegalStateException("Phantom session unavailable; reconnect", e);
        }
    }

    static synchronized void clear(Context c) {
        if (!prefs(c).edit().clear().commit()) throw new IllegalStateException("Cannot clear wallet");
        // Discard the encryption identity too, so previous ciphertext can never be reopened.
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            if (ks.containsAlias(KEY_ALIAS)) ks.deleteEntry(KEY_ALIAS);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot revoke stored wallet session key", e);
        }
    }

    private static String b64(byte[] src) { return Base64.encodeToString(src, Base64.NO_WRAP); }
    private static byte[] from64(String src) { return Base64.decode(src, Base64.DEFAULT); }

    private static JSONObject readSecure(String encrypted) throws Exception {
        return new JSONObject(new String(PhantomSecretBox.open(secretKey(false), encrypted),
                StandardCharsets.UTF_8));
    }

    private static SecretKey secretKey(boolean create) throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(KEY_ALIAS)) {
            KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) store.getEntry(KEY_ALIAS, null);
            return entry.getSecretKey();
        }
        if (!create) throw new IllegalStateException("Wallet secret storage key missing");
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build());
        return generator.generateKey();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
