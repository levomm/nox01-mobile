package app.buddy.assistant;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** AES-256-GCM envelope; persistent keys must be supplied by Android Keystore. */
final class PhantomSecretBox {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int IV_LEN = 12;
    private static final int TAG_LEN = 16;
    private static final int MAX_PLAINTEXT = 16 * 1024;

    private PhantomSecretBox() { }

    static String seal(SecretKey key, byte[] plaintext) {
        if (plaintext == null || plaintext.length == 0 || plaintext.length > MAX_PLAINTEXT) {
            throw new IllegalArgumentException("Invalid secret payload size");
        }
        try {
            byte[] iv = new byte[IV_LEN];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LEN * 8, iv));
            byte[] encrypted = cipher.doFinal(plaintext);
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot secure wallet session", ex);
        }
    }

    static byte[] open(SecretKey key, String envelope) {
        try {
            byte[] payload = Base64.getDecoder().decode(envelope);
            if (payload.length < IV_LEN + TAG_LEN + 1 ||
                    payload.length > IV_LEN + TAG_LEN + MAX_PLAINTEXT) {
                throw new IllegalArgumentException("Invalid encrypted session size");
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LEN);
            byte[] ciphertext = Arrays.copyOfRange(payload, IV_LEN, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LEN * 8, iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot authenticate encrypted session", ex);
        }
    }
}
