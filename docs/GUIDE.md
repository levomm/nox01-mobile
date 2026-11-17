# NOX01 Mobile kasutusjuhend

[Tutvustus ja allalaadimine](../README.md) · [Privaatsus](PRIVACY.md) · [Turvalisus](../SECURITY.md)

NOX01 on Androidi AI-agent. Agent töötab sinu telefonis, kuid pilvemudelite puhul kasutatakse eraldi AI-teenust. Telefonitööriistade kasutamiseks tuleb anda vajalikud Androidi õigused.

## Paigaldus

1. Laadi alla [viimane NOX01 APK](https://github.com/levomm/nox01-mobile/releases/latest/download/nox01-mobile.apk).
2. Ava APK Androidi paketipaigaldajaga. Kontrolli, et see pärineb selle projekti väljalaskest.
3. Käivita NOX01 ja vali agent: Claude Code, Codex, OpenCode, Antigravity või Grok.
4. Paigalda valitud agent ja logi vajaduse korral selle teenusesse sisse. Esmakordseks paigalduseks võib vaja minna sadu megabaite.
5. Lülita telefoni **Seaded → Juurdepääsetavus** alt sisse NOX01 teenus. Rakenduste juhtimine ilma selle õiguseta ei tööta.
6. Tee lihtne test: „ava telefoni seaded”. Kui telefon küsib lisalube, loe nende kirjeldus enne kinnitamist.

**Piiratud seade:** Android 13 ja uuemad võivad väljastpoolt Play poodi paigaldatud rakenduse juurdepääsetavusteenuse alguses blokeerida. Kontrolli rakenduse Androidi süsteemiinfos valikut „Luba piiratud seaded”, kui süsteem seda võimaldab. Ära lülita püsivalt välja seadme üldisi turvakontrolle.

## Vestlused ja agendid

- **Vestlused:** alusta uut vestlust, vali agent ja mudel, vaata tegevusi ning jätka sama vestlust hiljem.
- **Mudel:** saad agenti ja sobivat mudelit valida vastavalt oma kontole või ühendatud API-teenusele. Mudelite olemasolu sõltub teenusepakkujast.
- **Manused:** lisa pilte või faile, kui valitud agent seda toetab.
- **Linuxi tööriistad:** luba arendustööriistad vestluse jaoks siis, kui neid vaja läheb. See annab agendile rohkem õigusi kui lihtsalt ekraani vaatamine.
- **Hääl:** saad kasutada telefoni või ühendatud teenuse kõnetuvastust; vastuseid saab ette lugeda.

## Rakenduste kasutamine

Kirjuta näiteks „ava Telegram ja koosta vastus”. NOX01 saab proovida rakendust avada, nähtavat teksti lugeda ning vajalikke nuppe kasutada. Kolmanda osapoole rakendus võib ekraani automaatset juhtimist piirata.

**Näide:**

1. Sina: „ava Telegram ja kirjuta sõbrale, et jõuan umbes 20 minuti pärast.”
2. Agent: avab sobiva rakenduse ja valmistab teksti.
3. NOX01: palub vajaduse korral enne saatmist kinnitust.
4. Sina: kontrollid saajat ja sõnumit ning kinnitad või muudad seda.

Ära eelda, et agent saab ilma kontrollita alati õigesse vestlusesse või kontaktini. Eriti tundlikes tegevustes kontrolli alati tulemust.

## Teavitused ja automaatika

NOX01 saab valitud Androidi teavituste alusel käivitada juhiseid. Võid luua reegli konkreetse rakenduse, saatja või märksõnade jaoks. Teavituste lugemine eeldab eraldi Androidi teavitusjuurdepääsu.

Reegli käitumist saab piirata: küsi iga kord kinnitust, kasuta peatamisvõimalusega loendurit või määra selgelt lubatud automaatne toiming. Automaatne vastamine tähendab suuremat riski ja seda ei tohiks lubada tundlike vestluste jaoks.

**Ajastatud ülesanded:** vestluse ülesandeid saab käivitada kindla päeva või kellaaja alusel. Taustatöö võib hilineda, kui telefon piirab aku kasutamist või puudub vajalik võrguühendus. Kinnitamist nõudev toiming ei muutu automaatselt lubatuks ainult seetõttu, et ta on ajastatud.

## Ujuvmull ja välimus

NOX01 kasutab eraldi ujuvmulli, millest saad avada agendi vestluse või tegevuse. DOOM-stiilis pikseltegelane võib ooteajal vasakule ja paremale vaadata, töö ajal ilmet muuta ning harva näidata lühikest intensiivset olekut.

**Seaded → Välimus** võimaldab valida fonte, värve, valgusrežiimi ja kujunduse. Tactical Neu-Dark lisab reljeefsed kaardid ning elastse vajutusreaktsiooni. Ujuvmulli tegelast saab asendada oma pildiga.

## Taustabrauser, abilised ja arendus

Agent võib toetatud seadistuses kasutada rakenduses olevat brauserit või eraldi tausttöö režiimi. Taustal töötamise ulatus ja seadistus sõltuvad Androidi versioonist ja õigustest.

Arendustööriistadega saab telefonis käivitada Linuxi käske, redigeerida faile ja arendada tarkvara. Käske täitev agent saab teha rohkem kahju kui ainult lugemisõigustega agent, seega piira ligipääse.

MCP võimaldab ühendatud klientidel küsida NOX01 telefoni tööriistadelt andmeid või toiminguid. Lokaalne MCP endpoint kasutab autentimist. Ära avalda selle tokenit ega ava seda kontrollimatult internetti.

## Kohalik AI-mudel

Kui sul on Ollama, LM Studio või llama.cpp server, saad sobiva mudeli OpenCode'i kaudu ühendada. Telefonist vaadates ei ole arvutis olev server aadressil `127.0.0.1`, vaid selle tegelikul võrgu-IP-l või turvalisel tunneliaadressil. Väiksemad mudelid võivad keerukate mitmeetapiliste ekraaniülesannetega eksida.

## Rahakott (arendusharus, mitte avalikus APK-s)

NOX01 arendusharu `feature/phantom-wallet-implementation` lisab **Seaded → Rahakott**. Saad avalikku Solana aadressi jälgida, vaadata SOL-i ja ainult Circle'i ametlikku Solana USDC saldot, kopeerida aadressi ning näidata vastuvõtu QR-koodi. **Ühenda Phantom** avab Phantomi mobiilirakenduse ja palub ühendamiseks sinu luba. Seejärel kontrollib NOX01 krüpteeritud vastust, enne kui märgib aadressi ühendatuks.

**Ettevaatust:** Phantomi ühendamist pole veel sinu telefonis kontrollitud. **Raha saatmine on siin keelatud.** Ära sisesta NOX01 rahakoti vaatesse privaatvõtit ega taastefraasi. Kui ühendus ebaõnnestub, saad avalikku aadressi ainult lugemiseks jälgida. Käsitsi lisatud aadressi ei esitata Phantomiga ühendatud kontona.

See osa ei ole veel osa avalikust `v2.3.14` väljalaskest.

## Uuendamine

Kasuta alati [selle projekti viimast väljalaset](https://github.com/levomm/nox01-mobile/releases/latest). Paigalda APK olemasoleva peale, kui allkiri ühildub. Ära eemalda rakendust enne varundamist.

Kui näed Androidi veateadet `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, on installitud ja uus APK erineva allkirjaga. See ei ole põhjus paketiverifitseerimise väljalülitamiseks. Varunda oluline ning kontrolli APK päritolu ja allkirjastust.

## Ohutus

- Ekraani sisu või teavitus võib sisaldada pettust või pahatahtlikku juhist.
- Ära sisesta agenti paroole, 2FA-koode ega maksekaardiandmeid.
- Kontrolli saajaid ja toiminguid enne kinnitamist.
- Ära jaga MCP-tokenit ega API-võtmeid.
- Vajaduse korral peata agent või lülita NOX01 juurdepääsetavusteenus Androidis välja.

[Turvaaukude privaatsest teatamisest](../SECURITY.md) ja [andmetöötlusest](PRIVACY.md) loe eraldi dokumentidest.
