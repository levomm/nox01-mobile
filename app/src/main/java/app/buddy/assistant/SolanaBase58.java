package app.buddy.assistant;

import java.math.BigInteger;
import java.util.Arrays;

/** Base58 for public Solana addresses and encrypted Phantom transport bytes. */
final class SolanaBase58 {
    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final BigInteger BASE = BigInteger.valueOf(58);
    private static final int MAX_ENCODED_LENGTH = 4096;

    private SolanaBase58() { }

    static String encode(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > 3072) {
            throw new IllegalArgumentException("Invalid base58 input size");
        }
        int zeros = 0;
        while (zeros < bytes.length && bytes[zeros] == 0) zeros++;
        BigInteger value = new BigInteger(1, bytes);
        StringBuilder out = new StringBuilder();
        while (value.signum() != 0) {
            BigInteger[] divmod = value.divideAndRemainder(BASE);
            out.append(ALPHABET.charAt(divmod[1].intValue()));
            value = divmod[0];
        }
        for (int i = 0; i < zeros; i++) out.append('1');
        return out.reverse().toString();
    }

    static byte[] decode(String encoded) {
        if (encoded == null || encoded.isEmpty() || encoded.length() > MAX_ENCODED_LENGTH) {
            throw new IllegalArgumentException("Invalid base58 length");
        }
        int zeros = 0;
        while (zeros < encoded.length() && encoded.charAt(zeros) == '1') zeros++;
        BigInteger value = BigInteger.ZERO;
        for (int i = 0; i < encoded.length(); i++) {
            int digit = ALPHABET.indexOf(encoded.charAt(i));
            if (digit < 0) throw new IllegalArgumentException("Invalid base58 character");
            value = value.multiply(BASE).add(BigInteger.valueOf(digit));
        }
        byte[] binary = value.signum() == 0 ? new byte[0] : value.toByteArray();
        if (binary.length > 0 && binary[0] == 0) binary = Arrays.copyOfRange(binary, 1, binary.length);
        byte[] out = new byte[zeros + binary.length];
        System.arraycopy(binary, 0, out, zeros, binary.length);
        return out;
    }
}
