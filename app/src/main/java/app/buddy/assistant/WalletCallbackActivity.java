package app.buddy.assistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/** Exported only for the one nox01://phantom/connect redirect; no secrets logged. */
public final class WalletCallbackActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        int notice;
        try {
            notice = PhantomLink.handleConnectRedirect(this,
                    getIntent() == null ? null : getIntent().getData())
                    ? R.string.nox_wallet_phantom_connected
                    : R.string.nox_wallet_phantom_cancelled;
        } catch (Exception failure) {
            // No exception details here: a callback can contain session-related content.
            notice = R.string.nox_wallet_phantom_failed;
        }
        Toast.makeText(this, notice, Toast.LENGTH_LONG).show();
        Intent wallet = new Intent(this, WalletActivity.class);
        wallet.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(wallet);
        finish();
    }
}
