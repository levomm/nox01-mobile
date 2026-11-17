# NOX01 Wallet v1: olemasoleva Phantom-rahakoti ühendamine

**Kuupäev:** 2026-11-17  
**Staatus:** kavand kasutaja ülevaatuseks; see dokument ei väida, et funktsioon on rakendatud.  
**Repo:** `levomm/nox01-mobile`, Android Java, `ee.osx01.mobile`.

## Eesmärk ja piirid

NOX01 Mobile saab eraldi eestikeelse **Rahakott** vaate, mis ühendab kasutaja olemasoleva **Phantom Solana** konto ja näitab Solana mainnet'i SOL-i ning Circle'i päris USDC saldosid. Samuti saab aadressi kopeerida, näha vastuvõtu QR-koodi ning avada viimaste ülekannete üksikasju Solana Exploreris.

**V1 on rangelt ainult vaatamiseks.** NOX01 ei loo uut privaatvõtit, ei küsi seed phrase'i, ei halda varasid, ei koosta ega allkirjasta tehinguid ning ei lisa maksete või allkirjastamise tööriistu AI-agentidele, MCP-le, skillidele ega automaatikale.

Kasutaja kinnitas, et olemasolev Phantom sobib; eelmises vestluses lepiti kokku esialgne ainult vaatamise versioon.

## Lähenemiste võrdlus

1. **Soovitus: Phantom Mobile Deep Link `connect` + Solana RPC (valitud).** Kasutaja kinnitab Phantomis avaliku aadressi avaldamise. NOX01 jätab meelde ainult avaliku aadressi ja hangib saldo plokiahela RPC-st. Enamiku toimingute jaoks pole Phantomit hiljem tarvis avada.
2. **Avaliku aadressi käsitsi importimine.** Turvaline ja lihtne varuvariant, kui Phantomit pole paigaldatud või deep link ei toimi; see ei tõesta iseenesest, et kasutaja on aadressi omanik. UI märgib selle `Jälgitav aadress`, mitte `Ühendatud Phantom`.
3. **Solana Mobile Wallet Adapter.** Hea tulevaste eri rahakottide integratsiooniks, kuid Phantomi ametlikud Androidi juhised soovitavad selle asemel otse deep link'e. Phantomit ei näidata Solana Mobile'i MWA-toetatud rahakottide avalikus loendis; seega pole MWA esimese versiooni ühendusviis.

## Rakenduse UI

- NOX01 **Seaded → Rahakott** ning valikuliselt avalehe `Rahakott` otsetee. NOX01 olemasolev Tactical Neu-Dark, fondi- ja värviseaded säilivad.
- Ühendamata vaade: `Ühenda Phantom`, `Sisesta avalik aadress` (varuvariant) ja lühike hoiatus, et NOX01 ei küsi salajasi võtmeid.
- Ühendatud vaade: `Solana mainnet`, aadressi lühikuju, `Kopeeri aadress`, `Võta vastu` (QR), `Ava Phantom`, SOL saldo, **USDC (Circle, Solana)** saldo ja `Värskenda`.
- `Viimased tehingud` näitab maksimaalselt 20 võrgust pärit kirjet; allikas, aeg, staatus ja turvaline link Exploreri täpsele tehingule. Enne põhjalikku tehinguanalüüsi ei tohi väljamõeldud sisenev/väljuv suunda või euroväärtust näidata.
- `Eemalda aadress` kustutab NOX01 lokaalse seose. Vajaduse korral tuleb kasutajal Phantomis dApp-i ühendus eraldi tühistada, kuna v1 ei säilita Phantomi allkirjastamissessiooni.
- Lõppseadme ühenduse/saldo olekud: laadimine, ühendamise tühistamine, puuduv Phantom, vigane aadress, RPC timeout, RPC rate limit, andmeid pole, värskendus aegunud. Saldot ei kuvata värskena, kui võrgupäring ebaõnnestus.

## Android-ühendus ja andmevoog

1. Nupp `Ühenda Phantom` töötab ainult inimese otsese vajutuse järel. NOX01 loob ühekordse X25519 võtme- ja olekuidentifikaatori; avab `https://phantom.app/ul/v1/connect` parameetritega `app_url`, `dapp_encryption_public_key`, `redirect_link=nox01://phantom/connect?state=...`, `cluster=mainnet-beta`.
2. Callback vastu võetakse rangelt piiratud `exported` Activity kaudu. Kontrollitakse skeemi, host'i, teed, state'i, aegumist, puuduvate/duplitseeritud parameetrite olemasolu ja krüptograafilist vastuse dekrüpteerimist. Kontrollimata callback ei muuda rahakotiaadressi.
3. Vastus sisaldab krüpteeritud `public_key` ja `session`. Seanssi **ei säilitata** ning ühekordsed võtmed kustutatakse pärast edukat sidumist, tühistamist või aegumist (nt 5 min). Kui Android tapab rakenduse poole ühendamise pealt, tuleb kasutajal ühendamine uuesti alustada; ajutist võtit ei talletata turvamata failis.
4. Pärast edukat käsitsi kinnitamist salvestatakse ainult kontrollitud avalik Solana aadress Androidi privaatsesse seadistusruumi; `allowBackup=false` säilib. Käsitsi imporditud aadressi ei märgita Phantomis autoriseerituks.
5. Blokeeritakse või peatatakse agendi autonoomsed UI-toimingud, kui avatud on rahakoti ühendamine või kaitstud NOX01 rahakotivaade. NOX01 ei saa lubada, et kolmanda osapoole Phantomi UI ei oleks Androidi üldistele juurdepääsetavusteenustele nähtav; seega ühtegi raha liigutavat funktsiooni siia moodulisse ei lisata.
6. **Phantom deep link'i transporti ei asendata ajutise isetehtud krüptoga.** Vahetuseks kasutada tunnustatud ja testitud NaCl Boxi / XSalsa20-Poly1305 teostust vastavalt Phantomi dokumentatsioonile; enne sõltuvuse lisamist kontrollida litsentsi, hooldatavust ja GitHub Actionsi kordusehitust.

## Solana RPC andmed

- Esimene versioon kasutab `mainnet-beta`; RPC endpoint on HTTPS, vaikimisi `https://api.mainnet-beta.solana.com`, hiljem võib lisada kasutaja enda RPC URL-i. TLS- ja hosti kontrolli ei nõrgendata.
- SOL: `getBalance`, täisarvulised lamport'id → SOL üheksa kümnendkohaga.
- USDC: `getTokenAccountsByOwner` koos **mint-filteriga** `EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v` (Circle'i ametlik USDC Solana mainnetis). Kogused liidetakse täisarvudes ja kuvatakse kuue kümnendkohaga, vältides `double`-arvutusvigu.
- Ajaloovaates `getSignaturesForAddress` omaniku ja seotud USDC tokenikontode kohta; tulemused ühendatakse, sorteeritakse ja deduplitseeritakse, kuid piiratakse mahult. Üksikasjalik analüüs on v1-s Exploreri lingi taga.
- Kõik RPC sisendid ja väljundid valideeritakse; ebaõnnestumisel näidatakse viga, mitte oletuslikku nullsaldot. Mitte ühtegi `sendTransaction` või `sign*` meetodit.

## Failid ja vastutused (kavand)

| Komponent | Vastutus |
| --- | --- |
| `WalletActivity.java` | Ainult kasutajale nähtav eestikeelne rahakotivaade; nupud ja olekud |
| `PhantomLink.java` | Phantom deep link; callback, ühekordsed võtmed, turvaline parsimine |
| `SolanaReadOnlyRpc.java` | Limiitide ja timeoutidega ainult lugemise JSON-RPC klient |
| `WalletStore.java` | Ainult avaliku aadressi, ühenduse päritolu ja ajatembli salvestus |
| `WalletCallbackActivity.java` | Väike eraldatud Androidi redirect-käsitleja |
| `SetupActivity.java` | Seadete `Rahakott` menüürida |
| `AndroidManifest.xml` | Activity'd ja kitsas callback-intent-filter |
| `values/strings.xml`, `values-et/strings.xml` | Täielik lokaliseeritus |
| `scripts/test_nox01_wallet.py` ja muud testid | Regresioonikontrollid, RPC fikstuurid, krüptoühendus, halvad callback'id |

## Turvakontroll ja veakäsitlus

- Sõltumatu rahakott jääb **Phantomisse**; NOX01 ei salvesta taastefraasi, privaatvõtit ega kasutatavat allkirjastamissessiooni.
- RPC teenusepakkuja saab näha päringu saanud avalikku aadressi; esitada selge privaatsusteade.
- Solana aadressi base58 dekodeerimine peab andma täpselt 32 baiti; USDC kuvamine ainult kinnitatud mint'i põhjal (samanimeline võltstoken jäetakse välja).
- Kõik URL-id koostada staatiliste lubatud päritoludega. Ebausaldatud URI-sid või tehingupäringuid ei avata automaatselt.
- Wallet-connect on inimese tegevus. AI, MCP, teavitused ja ajastused ei tohi selle kaudu rahalist tegevust algatada.
- Phantom appi pole või ühendus katkeb: kasutaja näeb mõistetavat veateadet ja võib valida ainult aadressi jälgimise.

## Katsed ja avaldamise kriteeriumid

1. Testidega kontrollida tegelikku X25519 / NaCl dekodeerimist ja pöördkaotusi, callback'i state'i korduskasutust, aegumist ja pahatahtlikke uri-sid.
2. SOL ja USDC RPC testid mitme tokenikonto, nullkonto, pika arvu, vale mint'i, võrguvea ja rate limit'i korral.
3. Androidi build + allkirja SHA-256 + kordusehituse testid läbivad.
4. Testseadmes **ilma rahaliste tehinguteta** proovida Phantomis ühenduse kinnitamist, appi naasmist, värskendust, lahtiühendamist ja puuduvat Phantomit.
5. Uus versioon GitHubi välja anda alles pärast neid kontrolle; kui testtelefoni ei ole võimalik kasutada, märkida seda selgelt ja kasutada väljalaske jaoks esialgu `pre-release` märgistust.

## Viited

- [Phantom Android deep links](https://docs.phantom.com/phantom-deeplinks/deeplinks-ios-and-android)
- [Phantom connect](https://docs.phantom.com/phantom-deeplinks/provider-methods/connect)
- [Phantom encryption](https://docs.phantom.com/phantom-deeplinks/encryption)
- [Phantom redirectid](https://docs.phantom.com/phantom-deeplinks/specifying-redirects)
- [Phantom sessioonid](https://docs.phantom.com/phantom-deeplinks/handling-sessions)
- [Solana Mobile ühilduvad rahakotid](https://wallets.solanamobile.com/)
- [Solana RPC getBalance](https://solana.com/docs/rpc/http/getbalance)
- [Solana RPC getTokenAccountsByOwner](https://solana.com/docs/rpc/http/gettokenaccountsbyowner)
- [Solana ametlik USDC mint](https://solana.com/docs/payments/how-payments-work)
