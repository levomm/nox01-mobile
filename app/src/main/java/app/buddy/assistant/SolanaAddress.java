package app.buddy.assistant;

import java.math.BigInteger;
import java.util.Arrays;

/** Solana mainnet public-key validation. Never accepts a secret or seed phrase. */
final class SolanaAddress {
    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final BigInteger BASE = BigInteger.valueOf(58);

    private SolanaAddress() { }

    static boolean isValid(String address) {
        try {
            decode(address);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /** Decodes the canonical base58 encoding of exactly 32 public-key bytes. */
    static byte[] decode(String address) {
        if (address == null || address.length() < 32 || address.length() > 44) {
            throw new IllegalArgumentException("Invalid Solana public key length");
        }
        BigInteger value = BigInteger.ZERO;
        int zeroPrefix = 0;
        boolean leading = true;
        for (int i = 0; i < address.length(); i++) {
            char c = address.charAt(i);
            int digit = ALPHABET.indexOf(c);
            if (digit < 0) throw new IllegalArgumentException("Invalid base58 character");
            if (leading && digit == 0) zeroPrefix++;
            else leading = false;
            value = value.multiply(BASE).add(BigInteger.valueOf(digit));
        }
        byte[] raw = value.equals(BigInteger.ZERO) ? new byte[0] : value.toByteArray();
        if (raw.length > 0 && raw[0] == 0) raw = Arrays.copyOfRange(raw, 1, raw.length);
        if (zeroPrefix + raw.length != 32) {
            throw new IllegalArgumentException("Solana public key must decode to 32 bytes");
        }
        byte[] decoded = new byte[32];
        System.arraycopy(raw, 0, decoded, zeroPrefix, raw.length);
        return decoded;
    }
}
