# NOX01 Wallet v1: olemasoleva Phantom-rahakoti ühendamine

**Kuupäev:** 2026-11-17  
**Staatus:** kavand kasutaja ülevaatuseks; see dokument ei väida, et funktsioon on rakendatud.  
**Repo:** `levomm/nox01-mobile`, Android Java, `ee.osx01.mobile`.

## Eesmärk ja piirid

NOX01 Mobile saab eraldi eestikeelse **Rahakott** vaate, mis ühendab kasutaja olemasoleva **Phantom Solana** konto ja näitab Solana mainnet'i SOL-i ning Circle'i päris USDC saldosid. Samuti saab aadressi kopeerida, näha vastuvõtu QR-koodi ning avada viimaste ülekannete üksikasju Solana Exploreris.

**V1 toetab vaatamist ja kasutaja algatatud SOL-i / Circle'i USDC ülekandeid.** NOX01 ei loo privaatvõtit ega küsi taastefraasi. Tehingu koostab NOX01, **kasutaja kinnitab selle Phantomis** ning pärast Phantomi allkirja saadab NOX01 tehingu Solana RPC-sse. AI ei tohi allkirjastamist ega Phantomi kinnitust automatiseerida ega alustada makset MCP, teavitusreegli või ajastuse kaudu.

Kasutaja valis olemasoleva Phantomi ja palus lisada ka saatmise. Väljalase nõuab uut finantsfunktsioonide turvakontrolli; see dokument on kavand, mitte töötav tehingumoodul.

## Lähenemiste võrdlus

1. **Soovitus: Phantom Mobile Deep Link `connect` ja `signTransaction` + Solana RPC (valitud).** Phantomis kinnitatakse ühendus ja iga saadetav tehing. NOX01 näitab saadavat summat, aadressi ja hinnangulist võrgutasu enne Phantomisse suunamist ning edastab pärast allkirja serialiseeritud tehingu RPC kaudu. Phantomi `signAndSendTransaction` deep link on aegunud ja seda ei kasutata.
2. **Avaliku aadressi käsitsi importimine.** Turvaline ja lihtne varuvariant, kui Phantomit pole paigaldatud või deep link ei toimi; see ei tõesta iseenesest, et kasutaja on aadressi omanik. UI märgib selle `Jälgitav aadress`, mitte `Ühendatud Phantom`.
3. **Solana Mobile Wallet Adapter.** Hea tulevaste eri rahakottide integratsiooniks, kuid Phantomi ametlikud Androidi juhised soovitavad selle asemel otse deep link'e. Phantomit ei näidata Solana Mobile'i MWA-toetatud rahakottide avalikus loendis; seega pole MWA esimese versiooni ühendusviis.

## Rakenduse UI

- NOX01 **Seaded → Rahakott** ning valikuliselt avalehe `Rahakott` otsetee. NOX01 olemasolev Tactical Neu-Dark, fondi- ja värviseaded säilivad.
- Ühendamata vaade: `Ühenda Phantom`, `Sisesta avalik aadress` (varuvariant) ja lühike hoiatus, et NOX01 ei küsi salajasi võtmeid.
- Ühendatud vaade: `Solana mainnet`, aadressi lühikuju, `Kopeeri aadress`, `Võta vastu` (QR), `Saada`, `Ava Phantom`, SOL saldo, **USDC (Circle, Solana)** saldo ja `Värskenda`.
- `Saada` vaade: valuuta (SOL/ametlik Solana USDC), saaja avalik aadress, summa, võrgutasu hinnang, tegelikult saadetav kogus, `Kontrolli tehingut` ja `Kinnita Phantomis`. Makse suuruse automaatset ümardamist ega saldosid ületavaid makseid ei lubata. Pärast allkirja näitab NOX01 `Saadetud võrku`, `Kinnitamisel` või `Kinnitatud`, mitte valepositiivset `Õnnestus` enne plokiahela kinnitust.
- `Viimased tehingud` näitab maksimaalselt 20 võrgust pärit kirjet; allikas, aeg, staatus ja turvaline link Exploreri täpsele tehingule. Enne põhjalikku tehinguanalüüsi ei tohi väljamõeldud sisenev/väljuv suunda või euroväärtust näidata.
- `Ühenda lahti` kustutab NOX01 salvestatud avaliku aadressi ja krüpteeritud Phantomi sessiooni. Kasutajat juhendatakse vajadusel Phantomis dApp'i ühenduse tühistama.
- Lõppseadme ühenduse/saldo olekud: laadimine, ühendamise tühistamine, puuduv Phantom, vigane aadress, RPC timeout, RPC rate limit, andmeid pole, värskendus aegunud. Saldot ei kuvata värskena, kui võrgupäring ebaõnnestus.

## Android-ühendus ja andmevoog

1. Nupp `Ühenda Phantom` töötab ainult inimese otsese vajutuse järel. NOX01 loob ühekordse X25519 võtme- ja olekuidentifikaatori; avab `https://phantom.app/ul/v1/connect` parameetritega `app_url`, `dapp_encryption_public_key`, `redirect_link=nox01://phantom/connect?state=...`, `cluster=mainnet-beta`.
2. Callback vastu võetakse rangelt piiratud `exported` Activity kaudu. Kontrollitakse skeemi, host'i, teed, state'i, aegumist, puuduvate/duplitseeritud parameetrite olemasolu ja krüptograafilist vastuse dekrüpteerimist. Kontrollimata callback ei muuda rahakotiaadressi.
3. Vastus sisaldab krüpteeritud `public_key` ja `session`. Kuna `signTransaction` nõuab algset X25519 võtmeidentiteeti ja sessioonitokenit, tuleb need salvestada **Android Keystore'i kaitstud krüpteeritud salvestusse**, mitte tavapärasesse `SharedPreferences`-i ega logidesse. Iga uue ühenduse puhul luuakse uus juhuslik võtmepaar, callback'id seotakse ühekordse state'iga, allkirjastamiseks vajalik salajane sessioon jäetakse ainult keystore'iga kaitstud hoiule ja eemaldatakse lahtiühendamisel. Kui turvaline salvestus pole saadaval, tuleb uuesti ühendada, mitte langeda krüpteerimata salvestuse peale.
4. Pärast ühendamist salvestatakse valideeritud avalik Solana aadress ja Phantomi ühendussessioon rangelt eraldi. `allowBackup=false` säilib. Käsitsi imporditud aadressi ei märgita Phantomis autoriseerituks ning selliselt aadressilt saatmise nuppu ei pakuta.
5. **Rahakoti maksevaate avamisel peatatakse NOX01 agendi autonoomsed ekraanitoimingud ja teavitusautomaatika**, võimalusel varjatakse ujuvmull, kasutatakse `FLAG_SECURE`-t tundlike tehinguandmete puhul ning nõutakse otsest inimkasutaja puudutust. Phantom ei saa tehniliselt NOX01 juurdepääsetavusteenust täielikult välja lülitada: seetõttu peab NOX01 oma tegevusjärjekorra peatamise ning kinnituse mittedelegeerimise eraldi testima. Kasutaja võib süsteemset juurdepääsetavusteenust alati ise välja lülitada.
6. **Phantom deep link'i transporti ei asendata ajutise isetehtud krüptoga.** Vahetuseks kasutada tunnustatud ja testitud NaCl Boxi / XSalsa20-Poly1305 teostust vastavalt Phantomi dokumentatsioonile; enne sõltuvuse lisamist kontrollida litsentsi, hooldatavust ja GitHub Actionsi kordusehitust.

## Solana RPC andmed

- Esimene versioon kasutab `mainnet-beta`; RPC endpoint on HTTPS, vaikimisi `https://api.mainnet-beta.solana.com`, hiljem võib lisada kasutaja enda RPC URL-i. TLS- ja hosti kontrolli ei nõrgendata.
- SOL: `getBalance`, täisarvulised lamport'id → SOL üheksa kümnendkohaga.
- USDC: `getTokenAccountsByOwner` koos **mint-filteriga** `EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v` (Circle'i ametlik USDC Solana mainnetis). Kogused liidetakse täisarvudes ja kuvatakse kuue kümnendkohaga, vältides `double`-arvutusvigu.
- Ajaloovaates `getSignaturesForAddress` omaniku ja seotud USDC tokenikontode kohta; tulemused ühendatakse, sorteeritakse ja deduplitseeritakse, kuid piiratakse mahult. Üksikasjalik analüüs on v1-s Exploreri lingi taga.
- Kõik RPC sisendid ja väljundid valideeritakse; ebaõnnestumisel näidatakse viga, mitte oletuslikku nullsaldot.
- **SOL ülekanne:** ainult System Programi `transfer`, saaja 32-baidine base58 aadress; `getLatestBlockhash` ja `getFeeForMessage`; lamport'id täisarvuna. Reserveerida vajalik tehingutasu, keelduda ebapiisava saldo korral.
- **USDC ülekanne:** ainult ametlik `EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v` mint, SPL Token Programi `TransferChecked` ja 6 kümnendkohta; kontrollida omaniku tokenikontot, luua saajale ATA ainult vajadusel ja kuvada selle täiendav rentikulu; tasud SOL-is.
- Saaja, summa, token-mint, konto identiteet ja dekodeeritud tehingukäsud kontrollitakse uuesti **enne** Phantomi deep link'i loomist ja **pärast** allkirjastatud tehingu saamist. Allkirjastatud tehingu ootamatud lisakäsud või teistsugused adressaadid põhjustavad katkestuse.
- `signTransaction` tagastab allkirjastatud tehingu; NOX01 kasutab `sendTransaction` JSON-RPC väljakutset ainult selle konkreetse *inimese Phantomis allkirjastatud* tehingu edastamiseks ning `getSignatureStatuses` kinnituse jälgimiseks. Aegunud blockhash, kasutaja tühistamine, simulation error ja network timeout peavad andma selge veateate. Kui edastuse vastus kaob, kontrollitakse enne uuesti saatmist, ega sama signatuur pole juba plokiahelas.

## Failid ja vastutused (kavand)

| Komponent | Vastutus |
| --- | --- |
| `WalletActivity.java` | Eestikeelne saldo-, vastuvõtu- ja saatmise vaade, vormi kontrollid ning tehingukinnitus |
| `PhantomLink.java` | Phantom deep link; callback, ühekordsed võtmed, turvaline parsimine |
| `SolanaRpc.java` | Piiratud lugemis-, tasuarvutuse, allkirjastatud tehingu edastuse ja kinnituse kontrolli JSON-RPC klient |
| `WalletStore.java` | Avaliku aadressi salvestus ning Android Keystore'iga krüpteeritud Phantomi sessioon |
| `WalletCallbackActivity.java` | Ühendamise ja `signTransaction` tagasipöördumise eraldatud käsitleja |
| `SetupActivity.java` | Seadete `Rahakott` menüürida |
| `AndroidManifest.xml` | Activity'd ja kitsas callback-intent-filter |
| `values/strings.xml`, `values-et/strings.xml` | Täielik lokaliseeritus |
| `SolanaTransferBuilder.java` | Lubatud SOL/USDC ülekande sõnumi koostamine ja serialiseeritud tehingu sõltumatu valideerimine |
| `scripts/test_nox01_wallet.py` ja muud testid | Regresioonikontrollid, maksete fiksuurid, allkirja- ja callback-testid |

## Turvakontroll ja veakäsitlus

- Sõltumatu rahakott jääb **Phantomisse**; NOX01 ei salvesta taastefraasi ega rahakoti privaatvõtit. Allkirjastamisprotokolli sessiooni hoitakse ainult keystore'iga krüpteeritult, et Phantom saaks oma kasutaja allkirja anda.
- RPC teenusepakkuja saab näha päringu saanud avalikku aadressi; esitada selge privaatsusteade.
- Solana aadressi base58 dekodeerimine peab andma täpselt 32 baiti; USDC kuvamine ainult kinnitatud mint'i põhjal (samanimeline võltstoken jäetakse välja).
- Kõik URL-id koostada staatiliste lubatud päritoludega. Ebausaldatud URI-sid või tehingupäringuid ei avata automaatselt.
- Rahakoti ühendus ja lõplik saatmiskinnitus on inimese tegevused. AI võib pakkuda tekstilist makseettepanekut, kuid ei või iseseisvalt avada maksevormi, koostada allkirjastamispäringut, vajutada Phantomi nuppe ega tehingut edastada. MCP, skillid, teavitused ja ajastused ei saa maksmise tööriista.
- Phantom appi pole või ühendus katkeb: kasutaja näeb mõistetavat veateadet ja võib valida ainult aadressi jälgimise.

## Katsed ja avaldamise kriteeriumid

1. Testidega kontrollida tegelikku X25519 / NaCl dekodeerimist ja pöördkaotusi, callback'i state'i korduskasutust, aegumist ja pahatahtlikke uri-sid.
2. SOL ja USDC RPC testid mitme tokenikonto, nullkonto, pika arvu, vale mint'i, võrguvea ja rate limit'i korral. Lisaks kontrollida valede aadresside, lõppematu komakoha, ebapiisava SOL-tasu, vajaliku ATA üürikulu ja saaja kontrolli juhtumid.
3. Androidi build + allkirja SHA-256 + kordusehituse testid läbivad.
4. Testseadmes **ilma pärisrahata** proovida Phantomis ühendust, allkirjastamise tühistamist ja tagasi liikumist; testnetis/devenetis teha kasutaja kinnitatud väikese testtehinguga signTransaction → RPC saatmine → status. Mainneti raha ei kasutata automaatsetes testides.
5. Uus versioon GitHubi välja anda alles pärast neid kontrolle; kui testtelefoni või Phantomi testvõimalust ei ole, märkida piirang selgelt ning hoida saatmine välja lülitatud või väljalase `pre-release`-na kuni kohapealse kinnitamiseni. Raha saatmist ei reklaamita testimata valmisfunktsioonina.

## Viited

- [Phantom Android deep links](https://docs.phantom.com/phantom-deeplinks/deeplinks-ios-and-android)
- [Phantom connect](https://docs.phantom.com/phantom-deeplinks/provider-methods/connect)
- [Phantom encryption](https://docs.phantom.com/phantom-deeplinks/encryption)
- [Phantom signTransaction](https://docs.phantom.com/phantom-deeplinks/provider-methods/signtransaction)
- [SignAndSendTransaction on aegunud](https://docs.phantom.com/phantom-deeplinks/provider-methods/signandsendtransaction)
- [Phantom redirectid](https://docs.phantom.com/phantom-deeplinks/specifying-redirects)
- [Phantom sessioonid](https://docs.phantom.com/phantom-deeplinks/handling-sessions)
- [Solana Mobile ühilduvad rahakotid](https://wallets.solanamobile.com/)
- [Solana RPC getBalance](https://solana.com/docs/rpc/http/getbalance)
- [Solana RPC getTokenAccountsByOwner](https://solana.com/docs/rpc/http/gettokenaccountsbyowner)
- [Solana ametlik USDC mint](https://solana.com/docs/payments/how-payments-work)
