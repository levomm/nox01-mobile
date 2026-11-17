package app.buddy.assistant;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

/** The eight pixel-art NOX01 bubble states, cut from the user's supplied sprite sheet.
 * Rendered with nearest-neighbour sampling: no filter that would blur the original pixels.
 * Atlas order: idle, left, right, eyebrow, working, annoyed, bloody, overdrive.
 */
final class DoomFaces {
    static final int NEUTRAL = 0;
    static final int LOOK_LEFT = 1;
    static final int LOOK_RIGHT = 2;
    static final int EYEBROW = 3;
    static final int WORKING = 4;
    static final int ANNOYED = 5;
    static final int BLOODY = 6;
    static final int OVERDRIVE = 7;
    static final int EYEBROW_LEFT = 8;
    static final int EYEBROW_RIGHT = 9;
    private static Bitmap mirroredEyebrow;

    private static final int CELL_W = 64, CELL_H = 80, COLUMNS = 4;
    private static Bitmap[] frames;
    private static boolean attempted;

    private DoomFaces() {}

    private static synchronized Bitmap frame(Context context, int state) {
        if (!attempted) {
            attempted = true;
            try (InputStream stream = context.getAssets().open("nox01/doom_faces.b64")) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream(16 * 1024);
                byte[] chunk = new byte[4096];
                int n;
                while ((n = stream.read(chunk)) != -1) buffer.write(chunk, 0, n);
                byte[] bytes = Base64.decode(buffer.toByteArray(), Base64.DEFAULT);
                Bitmap atlas = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (atlas != null && atlas.getWidth() == CELL_W * COLUMNS && atlas.getHeight() == CELL_H * 2) {
                    frames = new Bitmap[8];
                    for (int i = 0; i < frames.length; i++) {
                        frames[i] = Bitmap.createBitmap(atlas, (i % COLUMNS) * CELL_W,
                                (i / COLUMNS) * CELL_H, CELL_W, CELL_H);
                    }
                }
            } catch (IOException | IllegalArgumentException e) {
                android.util.Log.w("NOX01", "DOOM bubble sprites unavailable", e);
            }
        }
        return frames == null ? null : frames[Math.max(0, Math.min(7, state))];
    }

    static Drawable drawable(Context c, int state) {
        // One eyebrow sprite, two directions. Mirror once rather than storing
        // duplicate bitmaps or scaling pixels with bilinear filtering.
        Bitmap b;
        if (state == EYEBROW_LEFT || state == EYEBROW_RIGHT) {
            b = frame(c, EYEBROW);
            if (b != null && state == EYEBROW_RIGHT) {
                synchronized (DoomFaces.class) {
                    if (mirroredEyebrow == null) {
                        android.graphics.Matrix flip = new android.graphics.Matrix();
                        flip.setScale(-1f, 1f);
                        mirroredEyebrow = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), flip, false);
                    }
                    b = mirroredEyebrow;
                }
            }
        } else b = frame(c, state);
        if (b == null) return null;
        BitmapDrawable sprite = new BitmapDrawable(c.getResources(), b);
        sprite.setFilterBitmap(false);
        sprite.setDither(false);
        return sprite;
    }

    static void show(ImageView v, int state) {
        Drawable d = drawable(v.getContext(), state);
        if (d != null) v.setImageDrawable(d);
        else v.setImageResource(R.drawable.ic_launcher_foreground);
    }
}
