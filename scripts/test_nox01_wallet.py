#!/usr/bin/env python3
"""NOX01 Wallet safety regression: no live accounts, no transactions, no secrets."""
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/app/buddy/assistant"
MANIFEST = ROOT / "app/src/main/AndroidManifest.xml"


def check(label, ok):
    assert ok, label
    print("PASS:", label)


address_source = (JAVA / "SolanaAddress.java").read_text()
rpc_source = (JAVA / "SolanaRpc.java").read_text()
check("SolanaAddress validates exactly 32 bytes", "zeroPrefix + raw.length != 32" in address_source)
check("Circle USDC mainnet mint is pinned",
      'EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v' in rpc_source)
check("USDC is filtered by on-chain mint", '.put("mint", CIRCLE_USDC_MINT)' in rpc_source)
check("USDC amount never uses floating point", "BigInteger units" in rpc_source
      and 'new BigInteger(amount.getString("amount"))' in rpc_source)
check("Wallet RPC methods are read-only", all(
    f'"{method}".equals(method)' in rpc_source for method in
    ("getBalance", "getTokenAccountsByOwner", "getSignaturesForAddress")))
check("Wallet RPC contains no transaction submission",
      not re.search(r'"(sendTransaction|signTransaction|signAndSendTransaction)"',
                    rpc_source))
check("Wallet RPC is HTTPS only", '"https".equalsIgnoreCase' in rpc_source)
check("Wallet RPC enforces timeouts and response limits",
      "setConnectTimeout" in rpc_source and "setReadTimeout" in rpc_source
      and "MAX_REPLY" in rpc_source)
check("No Phantom seed phrase or private key fields",
      "seed_phrase" not in rpc_source and "private_key" not in rpc_source)

if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        pkg = Path(tmp) / "app/buddy/assistant"
        pkg.mkdir(parents=True)
        source = pkg / "SolanaAddress.java"
        source.write_text(address_source)
        tester = pkg / "WalletAddressTest.java"
        tester.write_text("""package app.buddy.assistant;
class WalletAddressTest {
    public static void main(String[] args) {
        String[] good = {"11111111111111111111111111111111",
                "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
                "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"};
        String[] bad = {"", "0", "invalid!!", "1111111111111111111111111111111",
                "111111111111111111111111111111111",
                "0PjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"};
        for (String a : good) if (!SolanaAddress.isValid(a)) throw new AssertionError(a);
        for (String a : bad) if (SolanaAddress.isValid(a)) throw new AssertionError(a);
        if (SolanaAddress.decode(good[0]).length != 32) throw new AssertionError();
        System.out.println("SolanaAddress Java tests passed");
    }
}
""")
        subprocess.run(["javac", "-d", tmp, str(source), str(tester)], check=True)
        subprocess.run(["java", "-cp", tmp, "app.buddy.assistant.WalletAddressTest"],
                       check=True)

print("NOX01 Wallet read-only API tests passed")
