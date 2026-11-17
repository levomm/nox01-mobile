package app.buddy.assistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Solana wallet phase 1: read-only view and receive address. No agent/MCP tools.
 * A manually entered address is labelled WATCH ONLY, not "Connected Phantom".
 * Transfers are unavailable until the Phantom signer has passed its security review.
 */
public final class WalletActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView solValue, usdcValue, status;
    private LinearLayout history;
    private Button refresh;
    private String shownAddress;
    private boolean loading;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Theme.apply(this);
        render();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private static LinearLayout.LayoutParams block(Activity a, int bottomDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = Ui.dp(a, bottomDp);
        return p;
    }

    private LinearLayout card() {
        LinearLayout panel = Ui.column(this);
        panel.setPadding(Ui.dp(this, 19), Ui.dp(this, 18),
                Ui.dp(this, 19), Ui.dp(this, 18));
        panel.setBackground(Ui.neu(this) ? Ui.neuSurface(this, 22)
                : Ui.rounded(this, Ui.CARD, 22));
        return panel;
    }

    private void render() {
        shownAddress = WalletStore.address(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout root = Ui.column(this);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 14),
                Ui.dp(this, 18), Ui.dp(this, 32));
        scroll.addView(root);
        setContentView(scroll);

        TextView title = Ui.largeTitle(this, getString(R.string.nox_wallet_title));
        title.setPaddingRelative(0, Ui.dp(this, 12), 0, Ui.dp(this, 6));
        root.addView(title, block(this, 4));
        TextView network = Ui.monoText(this, "SOLANA  /  MAINNET-BETA", 12, Ui.OK);
        root.addView(network, block(this, 16));
        TextView privacy = Ui.text(this, getString(R.string.nox_wallet_security), 13, Ui.MUTED);
        root.addView(privacy, block(this, 20));

        if (shownAddress.isEmpty()) {
            LinearLayout panel = card();
            panel.addView(Ui.title(this, getString(R.string.nox_wallet_not_connected), 18),
                    block(this, 9));
            panel.addView(Ui.text(this, getString(R.string.nox_wallet_watch_explain),
                    14, Ui.TEXT2), block(this, 14));
            Button manual = Ui.button(this, getString(R.string.nox_wallet_watch), true);
            manual.setOnClickListener(v -> importAddress());
            panel.addView(manual, block(this, 11));
            panel.addView(Ui.text(this, getString(R.string.nox_wallet_phantom_pending),
                    13, Ui.MUTED));
            root.addView(panel, block(this, 16));
            return;
        }

        LinearLayout panel = card();
        panel.addView(Ui.title(this, getString(R.string.nox_wallet_watch_label), 18),
                block(this, 8));
        TextView addr = Ui.monoText(this, shownAddress, 13, Ui.TEXT);
        addr.setTextIsSelectable(true);
        panel.addView(addr, block(this, 13));
        LinearLayout buttons = Ui.row(this);
        Button copy = Ui.button(this, getString(R.string.nox_wallet_copy), false);
        copy.setOnClickListener(v -> {
            ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(
                    ClipData.newPlainText("Solana address", shownAddress));
            Toast.makeText(this, R.string.nox_wallet_copied, Toast.LENGTH_SHORT).show();
        });
        buttons.addView(copy);
        Button qr = Ui.button(this, getString(R.string.nox_wallet_receive), false);
        qr.setOnClickListener(v -> receiveQr());
        LinearLayout.LayoutParams qp = Ui.wrap();
        qp.leftMargin = Ui.dp(this, 8);
        buttons.addView(qr, qp);
        panel.addView(buttons, block(this, 13));
        Button unlink = Ui.outlinedButton(this, getString(R.string.nox_wallet_remove), Ui.MUTED);
        unlink.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.nox_wallet_remove)
                .setMessage(R.string.nox_wallet_remove_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.nox_wallet_remove, (dialog, which) -> {
                    WalletStore.clear(this);
                    render();
                }).show());
        panel.addView(unlink);
        root.addView(panel, block(this, 16));

        LinearLayout balances = card();
        balances.addView(Ui.title(this, getString(R.string.nox_wallet_balances), 18),
                block(this, 12));
        balances.addView(Ui.text(this, "SOL", 13, Ui.MUTED));
        solValue = Ui.monoText(this, "—", 27, Ui.TEXT);
        balances.addView(solValue, block(this, 16));
        balances.addView(Ui.text(this, "USDC  ·  Circle / Solana", 13, Ui.MUTED));
        usdcValue = Ui.monoText(this, "—", 27, Ui.TEXT);
        balances.addView(usdcValue, block(this, 16));
        refresh = Ui.button(this, getString(R.string.nox_wallet_refresh), true);
        refresh.setOnClickListener(v -> loadBalances());
        balances.addView(refresh, block(this, 10));
        status = Ui.text(this, getString(R.string.nox_wallet_loading), 12, Ui.MUTED);
        balances.addView(status);
        root.addView(balances, block(this, 16));

        LinearLayout historyCard = card();
        historyCard.addView(Ui.title(this, getString(R.string.nox_wallet_history), 18),
                block(this, 8));
        history = Ui.column(this);
        historyCard.addView(history);
        root.addView(historyCard, block(this, 16));

        LinearLayout notice = card();
        notice.addView(Ui.title(this, getString(R.string.nox_wallet_send_disabled), 16),
                block(this, 8));
        notice.addView(Ui.text(this, getString(R.string.nox_wallet_send_explain), 13, Ui.MUTED));
        root.addView(notice, block(this, 6));
        loadBalances();
    }

    private void importAddress() {
        final EditText input = new EditText(this);
        input.setHint(R.string.nox_wallet_address_hint);
        input.setTextSize(13);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setSelectAllOnFocus(true);
        LinearLayout wrap = Ui.column(this);
        wrap.setPadding(Ui.dp(this, 22), 0, Ui.dp(this, 22), 0);
        wrap.addView(input);
        new AlertDialog.Builder(this).setTitle(R.string.nox_wallet_watch)
                .setMessage(R.string.nox_wallet_watch_explain)
                .setView(wrap)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.nox_wallet_save, (d, which) -> {
                    String address = input.getText().toString().trim();
                    if (!SolanaAddress.isValid(address)) {
                        new AlertDialog.Builder(this).setMessage(R.string.nox_wallet_bad_address)
                                .setPositiveButton(android.R.string.ok, (dialog, w) -> importAddress())
                                .show();
                        return;
                    }
                    WalletStore.saveWatchAddress(this, address);
                    render();
                }).show();
    }

    private void loadBalances() {
        if (loading || shownAddress == null || shownAddress.isEmpty()) return;
        final String requested = shownAddress;
        loading = true;
        solValue.setText("—");
        usdcValue.setText("—");
        status.setText(R.string.nox_wallet_loading);
        history.removeAllViews();
        refresh.setEnabled(false);
        executor.execute(() -> {
            try {
                SolanaRpc rpc = new SolanaRpc();
                SolanaRpc.Balance balance = rpc.getBalances(requested);
                List<SolanaRpc.History> recent = null;
                String historyError = null;
                try { recent = rpc.getRecentSignatures(requested); }
                catch (Exception e) { historyError = e.getMessage(); }
                final List<SolanaRpc.History> records = recent;
                final String note = historyError;
                runOnUiThread(() -> {
                    if (!alive(requested)) return;
                    loading = false;
                    refresh.setEnabled(true);
                    solValue.setText(new BigDecimal(balance.solLamports).movePointLeft(9)
                            .stripTrailingZeros().toPlainString());
                    usdcValue.setText(new BigDecimal(balance.usdcUnits).movePointLeft(6)
                            .stripTrailingZeros().toPlainString());
                    status.setText(getString(R.string.nox_wallet_updated,
                            DateFormat.getTimeInstance(DateFormat.SHORT).format(
                                    new Date(balance.fetchedAtMillis))));
                    showHistory(records, note);
                });
            } catch (Exception error) {
                final String detail = error.getMessage();
                runOnUiThread(() -> {
                    if (!alive(requested)) return;
                    loading = false;
                    refresh.setEnabled(true);
                    solValue.setText("—");
                    usdcValue.setText("—");
                    status.setText(getString(R.string.nox_wallet_error) + " "
                            + (detail == null ? "" : detail));
                    showHistory(null, null);
                });
            }
        });
    }

    private boolean alive(String requested) {
        return !isFinishing() && !isDestroyed() && requested.equals(shownAddress)
                && refresh != null && status != null;
    }

    private void showHistory(List<SolanaRpc.History> entries, String failure) {
        history.removeAllViews();
        if (entries == null || entries.isEmpty()) {
            history.addView(Ui.text(this, failure == null ? getString(R.string.nox_wallet_no_history)
                    : getString(R.string.nox_wallet_history_error), 13, Ui.MUTED));
            return;
        }
        for (SolanaRpc.History item : entries) {
            String shortSig = item.signature.substring(0, Math.min(8, item.signature.length()))
                    + "…" + item.signature.substring(Math.max(0, item.signature.length() - 7));
            String time = item.blockTime > 0
                    ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(new Date(item.blockTime * 1000L)) : "";
            TextView row = Ui.text(this, shortSig + "  •  " + time + "  •  "
                    + getString(item.failed ? R.string.nox_wallet_failed : R.string.nox_wallet_confirmed),
                    13, item.failed ? Ui.ERR : Ui.TEXT2);
            row.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
            row.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://explorer.solana.com/tx/" + item.signature))));
            history.addView(row);
        }
    }

    private void receiveQr() {
        if (!SolanaAddress.isValid(shownAddress)) return;
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 4);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            BitMatrix matrix = new MultiFormatWriter().encode(shownAddress, BarcodeFormat.QR_CODE,
                    500, 500, hints);
            Bitmap bitmap = Bitmap.createBitmap(matrix.getWidth(), matrix.getHeight(),
                    Bitmap.Config.ARGB_8888);
            for (int y = 0; y < matrix.getHeight(); y++) {
                for (int x = 0; x < matrix.getWidth(); x++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            ImageView image = new ImageView(this);
            image.setImageBitmap(bitmap);
            image.setContentDescription(getString(R.string.nox_wallet_receive));
            image.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14));
            new AlertDialog.Builder(this).setTitle(R.string.nox_wallet_receive)
                    .setMessage(R.string.nox_wallet_qr_note).setView(image)
                    .setPositiveButton(android.R.string.ok, null).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.nox_wallet_qr_error, Toast.LENGTH_LONG).show();
        }
    }
}
