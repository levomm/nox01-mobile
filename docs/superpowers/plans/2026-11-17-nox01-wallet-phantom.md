# NOX01 Wallet / Phantom SOL ja USDC — rakendusplaan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** NOX01 Androidi kasutaja saab Phantomi kaudu Solana SOL-i ja Circle'i USDC-d vaadata, vastu võtta ning enda kinnitatud tehingutega saata.

**Architecture:** Eraldi rahakoti UI + kontrollitud Phantomi ühendus + allkirjastamissessioon Android Keystore'i taga + Solana RPC klient ja kitsalt lubatud ülekandetehingu koostaja. Cloudflare x402/AgentPay on eraldi tulevane maksete REST/MCP adapter ning seda ei ühendata otse tehingu allkirjastamisega.

**Tech Stack:** Android Java, Gradle, HTTPS JSON-RPC, Android Keystore, X25519/NaCl Box, Phantom Android deep links, Solana mainnet, Circle USDC SPL Token.

**Spec:** `docs/superpowers/specs/2026-11-17-nox01-phantom-wallet-v1-design.md`

## Global Constraints

- Android applicationId on `ee.osx01.mobile`; minSdk 30; API-de kasutamise tõttu säilivad senised allkirja- ja uuendusmehhanismid.
- Privaatvõtit ega recovery phrase'i ei küsita ega salvestata. Phantomi vajalik ühendusseanss krüpteeritakse Android Keystore'i abil, mitte ei salvestata toorelt.
- Kasutaja peab iga saatmise Phantomis ise kinnitama. EI AI/MCP/skillide/ajastuste iseseisvatele rahatehingutele.
- Solana mainnet SOL ja *ainult* Circle USDC mint `EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v`.
- Cloudflare x402 töötab eraldi, eeskätt Base'i USDC kontekstis. Ühine kasutajaliides võib hiljem toetada mõlemat, kuid puudub automaatne chain-bridge või iseseisev tasumine.
- Esimene väljalase peab läbima negatiivsed testid, allkirjastatud APK build'i, minimaalsete õiguste ülevaatuse ja Phantomiga telefonitesti.

---

### Task 1: Rahakotiaadressi valideerimine ja ainult-lugemise RPC

**Files:**
- Create: `app/src/main/java/app/buddy/assistant/SolanaAddress.java`
- Create: `app/src/main/java/app/buddy/assistant/SolanaRpc.java`
- Test: `scripts/test_nox01_wallet.py`

**Interfaces:**
- `SolanaAddress.isValid(String address)` tagastab true ainult base58-dekodeerimisel täpselt 32 baidi korral.
- `SolanaRpc.getSolBalance(String address)` ja `getUsdcBalance(String address)` kasutavad ainult Solana RPC lugemismeetodeid; summad on `BigInteger` / `BigDecimal`, mitte `double`.

- [ ] **Step 1: Lisa punased testid vigastele aadressidele, vale mint'i kontodele ja mitme USDC konto liitmisele.**
```python
assert not solana_address_valid("invalid!!")
assert solana_address_valid("11111111111111111111111111111111")
assert sum([1200000, 3500000]) == 4700000 # 4.7 USDC / 6 decimals
```
- [ ] **Step 2: Käivita `python3 scripts/test_nox01_wallet.py`; enne teostust peab ebaõnnestuma.**
- [ ] **Step 3: Kirjuta kaks ülalnimetatud Java klassi, ainult `getBalance` ja `getTokenAccountsByOwner`; `getSignaturesForAddress` ainult ajaloole; määra HTTPS timeout ja rate-limit veateated.**
- [ ] **Step 4: Käivita fikstuurid ja Androidi kompileerimine.**
- [ ] **Step 5: Commit `feat(wallet): read-only Solana SOL and USDC RPC`.**

### Task 2: Phantom deep link'i turvaline ühendamine

**Files:**
- Create: `app/src/main/java/app/buddy/assistant/PhantomLink.java`
- Create: `app/src/main/java/app/buddy/assistant/WalletStore.java`
- Create: `app/src/main/java/app/buddy/assistant/WalletCallbackActivity.java`
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `scripts/test_nox01_wallet.py`

**Interfaces:**
- `PhantomLink.beginConnect(Activity a)` avab kasutaja nupuvajutusel Phantomi connect deep link'i.
- `WalletCallbackActivity` kontrollib state'i, ühekordsust ja ajalimiiti enne krüptograafilist vastuse dekrüpteerimist.
- `WalletStore` säilitab avaliku aadressi ja Phantomi sessiooni AES-GCM krüpteeritult Android Keystore'i võtmega; `clear()` eemaldab kõik andmed.

- [ ] **Step 1: Kirjuta testid korduskasutatud state'ile, katkestatud ühendusele, vigasele nonce'ile ning krüpteerimata sessioonidump'i puudumisele.**
```python
assert callback_state != ""
assert stale_callback_is_rejected
assert untrusted_scheme_is_rejected
```
- [ ] **Step 2: Käivita kontrollid ja veendu, et need enne teostust ebaõnnestuvad.**
- [ ] **Step 3: Integreeri hooldatud NaCl Box / X25519 teek, mille kasutatav API ja sõltuvuslitsents kontrollitakse; tee kitsas `nox01://phantom/connect` callback.**
- [ ] **Step 4: Testi Phantomi kaudu päris ühendamist ilma rahata ning salvestatud seansi eemaldamist.**
- [ ] **Step 5: Commit `feat(wallet): connect Phantom with protected session`.**

### Task 3: Eestikeelne rahakotivaade

**Files:**
- Create: `app/src/main/java/app/buddy/assistant/WalletActivity.java`
- Modify: `app/src/main/java/app/buddy/assistant/SetupActivity.java`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-et/strings.xml`
- Test: `scripts/test_nox01_wallet.py`

**Interfaces:**
- `WalletActivity` näitab ainult ühenduse päritoluga märgistatud saldosid, aadressi ja `Võta vastu` vaadet. `Saada` on kuni Task 4 allkirjastamise valmimiseni keelatud.

- [ ] **Step 1: Testi menüü `Rahakott` olemasolu, õiget QR-aadressi ja võrguta olekut.**
```python
assert "Rahakott" in et_strings
assert "Ühenda Phantom" in et_strings
assert "USDC" in et_strings
```
- [ ] **Step 2: Tee test punaseks.**
- [ ] **Step 3: Ehita olemasoleva `Ui.java` ja `Theme.java` järgi Tactical Neu-Dark rahakotivaade.**
- [ ] **Step 4: Käivita Android build ja testseadmes ainult lugemise läbikäik.**
- [ ] **Step 5: Commit `feat(wallet): Estonian wallet UI and receive flow`.**

### Task 4: Ainult inimese kinnitatud SOL/USDC saatmine

**Files:**
- Create: `app/src/main/java/app/buddy/assistant/SolanaTransferBuilder.java`
- Extend: `PhantomLink.java`, `SolanaRpc.java`, `WalletActivity.java`
- Test: `scripts/test_nox01_wallet.py` koos allkirjastamata, allkirjastatud ja pahatahtlike tehingute fikstuuridega.

**Interfaces:**
- `SolanaTransferBuilder.buildSolTransfer(owner,recipient,lamports,blockhash)` ainult System Program transfer.
- `SolanaTransferBuilder.buildUsdcTransfer(owner,recipient,units,blockhash)` ainult ametliku USDC mint'i TransferChecked ning vajadusel ATA loomine; avalik summa näidatakse enne allkirjastamist.
- `PhantomLink.beginSignTransaction(Activity,byte[] serializedTx)` vajab kasutaja otsest vajutust.
- `SolanaRpc.submitSignedTransaction(byte[] raw)` on lubatud vaid Phantomi allkirja ja väljade sõltumatu valideerimise järel.

- [ ] **Step 1: Testi vigast saajat, summat, valeminti, kõrvalisi instruktsioone, blockhash'i aegumist ning mittenõustuvat allkirjastamist.**
```python
assert invalid_recipient_blocks_payment
assert extra_instruction_blocks_payment
assert signature_mismatch_blocks_payment
assert cancelled_phantom_does_not_submit
```
- [ ] **Step 2: Testid peavad olema enne teostust punased.**
- [ ] **Step 3: Lisa ainult lubatud transfer-sõnumid, allkirja kontroll ning Phantomi signTransaction → Solana sendTransaction → getSignatureStatuses töövoog.**
- [ ] **Step 4: Kui Phantomi tagasipöördumine ebaõnnestub, ära saada tehingut; testneti/deveneti test enne mainneti nupu aktiveerimist.**
- [ ] **Step 5: Commit `feat(wallet): user-approved Phantom SOL and USDC transfers`.**

### Task 5: Agendi piiramise audit, väljalase ja tulevane Cloudflare-liides

**Files:**
- Modify: `WalletActivity.java`, `SetupActivity.java`, `BuildInfo.java`, Android manifest
- Modify: `docs/GUIDE.md`, `docs/PRIVACY.md`
- Create: `docs/architecture/nox01-wallet-agentpay-boundary.md`
- Tests: `scripts/test_nox01_wallet.py`, GitHub Actions release checks

- [ ] **Step 1: Lisa regressioonitest, et MCP tööriistade, skillide ja agentide avalikud nimekirjad ei sisaldaks `signTransaction`, `sendTransaction` ega salajase sessiooni lugemist.**
```python
assert "phantom_private_session" not in exported_agent_tool_schema
assert "wallet_send" not in exported_agent_tool_schema
```
- [ ] **Step 2: Kontrolli, et automaatika ei saa vajutada Phantomi kinnitusi ning `FLAG_SECURE` ja kaitstud rahakotiekraani käitumine on testitud.**
- [ ] **Step 3: Lisa Chain/Network abstraktsiooni eraldatud dokument: Solana/Phantom (SOL ja USDC), Base/x402 (eraldi makseadapter; ei ole aktiivne). Ära ehita võltsühendust olemasoleva `osx01-hexbrain-gateway` Workeri külge.**
- [ ] **Step 4: Käivita puhas GitHub Actions, kontrolli APK allkirja ja uuendusvõimet; Phantom Androidi testis kinnita ühendamine, tühistamine ja üks rahata testtehing.**
- [ ] **Step 5: Avalda ainult pärast ülevaatust; puuduliku Phantomi seadmetesti korral jäta saatmine piiratud testversiooni ja ära esita seda valmis rahafunktsioonina.**
