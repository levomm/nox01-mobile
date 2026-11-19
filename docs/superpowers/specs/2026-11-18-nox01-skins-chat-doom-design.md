# NOX01 2.3.15: DOOM, üldine kujundus ja vestluse kontrast

**Kuupäev:** 2026-11-18. **Otsus:** kasutaja kinnitas töö käsuga „Just. gogogooo”.
**Piirid:** NOX01 Mobile Android Java, `ee.osx01.mobile`, olemasoleva väljalaske v2.3.14 peale ühilduv uuendus. Rahakotimoodul ja Cloudflare on sellest muudatusest väljas.

## Käitumine
- DOOM-i nägude allikas on üksnes kasutaja antud `001Doom.png`, neli eraldi väljalõiget ja senine nendest saadud `doom_faces.b64`. Valgesilmne nägu asendab WORKING-spriti; RUNNING/EXECUTING näitab seda kohe ja pidevalt, mitte alles pärast tööriistakutset. IDLE vaheldab neutraalse, kulm vasakule, kulm paremale ja vasak/parem pilgu. ERROR võib korraks näidata verisemat spriti, kuid aktiivset tööd ei tohi tavaline tööriistakutse asendada verise näoga. Pildi nearest-neighbor ja alfa peavad säilima. Keelatud on uute suvaliste nägude genereerimine.
- `Theme` jääb üheks tõeallikaks. Globaalsed presetid: Tactical Neu-Dark, Green Terminal, Red Alert, Cold Steel, Clean Mono; säilivad vanade kasutajate warm/neutral/black seaded. Üks preset määrab nii tumerežiimi, surface'i kui accent-värvi. Kõik ekraanid ja ujuvmull saavad `Theme.changed` teate; süsteemi Toast'i OS-i kujundust ei väideta kontrollitavaks.
- `Ui.neuSurface()` ei kasuta jäigalt üht fikseeritud paletti: tumedaid, heledaid ja erineva accent'iga pindu tuletatakse `Ui.BG`, `Ui.CARD`, `Ui.CARD2`, `Ui.OUTLINE` järgi. Rõhksüsteemil on kasutajale üks `Kaardiefekt` valik (tavaline / ruumiline), olemasolev font ja accent jäävad samast Välimuse menüüst juhitavaks. Mulli raamistiil (tavaline / minimaalne) kasutab samu globaalvärve, kuid ei peida silmi ega muuda tegelasi.
- Kõigis kahes vestlusvaates on kasutaja sõnum **paremal** oma tumerohelisel/hallil pindkaardil ja agendi tekst **vasakul** selgelt eraldi kaardil. Pealkirjad `SINA` ja `NOX01` või accent-ääris, kõrge kontrast, 8–12 dp vahed. Markdown, lingid, kopeerimine, tabelid, failid ja ligipääsetavus säilivad. Muutused ei anna agentidele uusi õigusi.

## Kontrollid
- Statiilised UI regressioonitestid, DOOM atlas mõõtmetega 256x160, kõik 8 kaadrit; WORKING kaadris hele silmapaar; teiste kaadrite baidid jäävad samaks.
- Põhivälimuse valik muudab `Theme` võtme ja mulli välimust; valikutes ei ole katkiseid seoseid; vanad välimusandmed säilivad.
- GitHub Actions Java kompileerimine, APK allkirja, kordusehituse ja Androidi versioonikoodi kontroll. Telefonipõhine visuaaltest tuleb eraldi märkida, kui seadet ei saa käivitada.
