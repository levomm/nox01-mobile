#!/usr/bin/env python3
"""Phantom connect: real JVM callback/base58/AES-GCM tests, plus Android wallet guards.
No network, secrets, live wallet or transfer needed.
"""
from pathlib import Path
import subprocess
import tempfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/app/buddy/assistant"
SOURCE = ["SolanaBase58.java", "PhantomCallback.java", "PhantomSecretBox.java"]

java_test = r"""
package app.buddy.assistant;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class PhantomWireTest {
    static void check(boolean v, String why) { if (!v) throw new AssertionError(why); }
    static void reject(String uri, String state, long start, long end) {
        try { PhantomCallback.parse(uri, state, start, end);
              throw new AssertionError("Accepted malicious callback"); }
        catch (IllegalArgumentException expected) { }
    }
    static void expectInvalid(SecretKey k, String input) {
        try { PhantomSecretBox.open(k, input);
              throw new AssertionError("Accepted unauthenticated ciphertext"); }
        catch (IllegalArgumentException expected) { }
    }

    public static void main(String[] args) throws Exception {
        byte[] zeros = new byte[32];
        check(SolanaBase58.encode(zeros).equals("11111111111111111111111111111111"),
              "Base58 zero prefix");
        check(Arrays.equals(zeros, SolanaBase58.decode(SolanaBase58.encode(zeros))),
              "Base58 all-zero round trip");
        byte[] mixed = new byte[]{0, 0, 1, 2, -3, 55, 0};
        check(Arrays.equals(mixed, SolanaBase58.decode(SolanaBase58.encode(mixed))),
              "Base58 non-zero round trip");
        try { SolanaBase58.decode("0OIl"); throw new AssertionError("bad base58"); }
        catch (IllegalArgumentException expected) { }

        String state = "F7ciiqby-ocOEOqvHP2N3Tug48GQlBoKyY2TMWxl5pA";
        String uri = "nox01://phantom/connect?state=" + state
                + "&phantom_encryption_public_key=abc&nonce=def&data=ghi";
        PhantomCallback.Result accepted = PhantomCallback.parse(uri, state, 10000, 11000);
        check("ghi".equals(accepted.ciphertext), "Encrypted response");
        reject(uri.replace("state="+state, "state=wrong"),state,10000,11000);
        reject(uri.replace("/connect", "/disconnect"),state,10000,11000);
        reject(uri.replace("nox01://phantom", "https://phantom"),state,10000,11000);
        reject(uri + "&state=" + state, state,10000,11000);
        reject(uri + "&data=ghi", state,10000,11000);
        reject(uri, state,10000,310001); // expired
        reject(uri, state,10000,9999);   // clock went backwards
        reject(uri + "#fragment", state,10000,11000);
        reject(uri.replace("&nonce=def",""),state,10000,11000);
        PhantomCallback.Result denial = PhantomCallback.parse(
                "nox01://phantom/connect?state="+state+"&errorCode=4001",state,10000,11000);
        check(denial.denied, "User rejection is not a successful connection");
        reject(uri+"&errorCode=4001",state,10000,11000);

        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        SecretKey key = generator.generateKey();
        SecretKey wrong = generator.generateKey();
        byte[] message = "phantom session and dapp key".getBytes(StandardCharsets.UTF_8);
        String ciphertext = PhantomSecretBox.seal(key, message);
        check(!ciphertext.contains("phantom"),"No raw session data");
        check(Arrays.equals(message,PhantomSecretBox.open(key,ciphertext)),"AES-GCM round trip");
        check(!ciphertext.equals(PhantomSecretBox.seal(key,message)),"Random nonce every time");
        expectInvalid(wrong,ciphertext);
        expectInvalid(key,"bad");
        char ch=ciphertext.charAt(10);
        expectInvalid(key,ciphertext.substring(0,10) + (ch=='A'?'B':'A') + ciphertext.substring(11));
        System.out.println("PASS: Phantom callback, Base58, authenticated Keystore envelope");
    }
}
"""

with tempfile.TemporaryDirectory() as temporary:
    p = Path(temporary)
    test = p / "PhantomWireTest.java"
    test.write_text(java_test)
    subprocess.run(["javac", "-d", str(p), str(test)] + [str(APP / s) for s in SOURCE],
                   check=True)
    subprocess.run(["java", "-cp", str(p), "app.buddy.assistant.PhantomWireTest"],
                   check=True)

link = (APP / "PhantomLink.java").read_text()
store = (APP / "WalletStore.java").read_text()
ui = (APP / "WalletActivity.java").read_text()
manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
an = "{http://schemas.android.com/apk/res/android}"
callbacks = [x for x in manifest.iter("activity")
             if x.get(an+"name") == ".WalletCallbackActivity"]
assert len(callbacks) == 1 and callbacks[0].get(an+"exported") == "true"
deeplinks = list(callbacks[0].iter("data"))
assert len(deeplinks) == 1
assert all(deeplinks[0].get(an+k) == v for k,v in [
    ("scheme","nox01"),("host","phantom"),("path","/connect")])
assert 'new TweetNaclFast.Box(phantomPublic, pending.dappSecret)' in link
assert "PhantomCallback.parse" in link and "WalletStore.clearPending(context)" in link
assert "WalletStore.saveConnected" in link
assert "AndroidKeyStore" in store and "PhantomSecretBox.seal" in store
assert "putString(SESSION, encrypted)" in store
assert "setRandomizedEncryptionRequired(true)" in store
assert 'getString(R.string.nox_wallet_connect_phantom)' in ui
assert "PhantomLink.beginConnect(this)" in ui
assert "nox_wallet_send_disabled" in ui
assert "signTransaction" not in link and "sendTransaction" not in link
print("PASS: restricted Phantom intent filter, wallet UI and zero-transfer guard")
