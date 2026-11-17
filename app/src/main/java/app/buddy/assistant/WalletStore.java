package app.buddy.assistant;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Public watch-only address. Never stores a seed phrase, private key or signing session.
 * A manually entered address MUST NOT be represented as a Phantom-approved connection.
 */
final class WalletStore {
    private static final String FILE = "nox01_wallet_public_v1";
    private static final String ADDRESS = "solana_watch_address";
    private static final String ORIGIN = "source";

    private WalletStore() { }

    static String address(Context context) {
        String value = prefs(context).getString(ADDRESS, "");
        return SolanaAddress.isValid(value) ? value : "";
    }

    static boolean isWatchOnly(Context context) {
        return "manual".equals(prefs(context).getString(ORIGIN, ""));
    }

    static void saveWatchAddress(Context context, String address) {
        if (!SolanaAddress.isValid(address)) {
            throw new IllegalArgumentException("Invalid Solana public address");
        }
        prefs(context).edit().putString(ADDRESS, address)
                .putString(ORIGIN, "manual").apply();
    }

    static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
