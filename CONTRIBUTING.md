# NOX01 Mobile kaastöö

NOX01 Mobile on avatud lähtekoodiga Androidi arendusprojekt. Oodatud on veateated, eestikeelse tõlke parandused, ideed, testitulemused ja põhjendatud pull request'id.

## Veateatest on abi, kui lisad

- Telefoni mudeli ja Androidi versiooni
- NOX01 väljalaske versiooni
- Kasutatud agendi ja mudeli
- Sammud vea kordamiseks
- Oodatud ning tegeliku tulemuse
- Vajadusel kuvatõmmise, millest oled eemaldanud isikuandmed

[Ava veateade](https://github.com/levomm/nox01-mobile/issues/new/choose).

**Turvahaavatavusi ei tohi avaldada avalikus issue's.** Vaata [turvajuhist](SECURITY.md).

## Arendus ja ehitamine

```bash
git clone --recurse-submodules https://github.com/levomm/nox01-mobile.git
cd nox01-mobile
./gradlew assembleRelease
```

Tegelikud Androidi tööriistade sõltuvused on defineeritud Gradle'i ja ehitusskriptides. Alternatiiv on `./build.sh` Termuxis, eeldusel, et vajalik Java/Androidi tööriistaahel on paigaldatud. `git submodule update --init --recursive` taastab alamprojektid.

**Oluline:** enda ehitatud APK ei pruugi olla allkirjastatud sama võtmega mis ametlik NOX01 väljalase, mistõttu seda ei pruugi saada olemasoleva installi peale paigaldada.

## Testimine

GitHub Actionsi `build.yml` kontrollib abistavaid teste, rakenduse APK ehitamist ja püsiallkirja. Käivita võimaluse korral ka lokaalsed testid enne PR-i.

```bash
python3 scripts/test_nox01_branding.py
python3 scripts/test_nox01_launcher.py
python3 scripts/test_doom_bubble.py
python3 scripts/test_neu_dark_ui.py
```

Kõik testid ei tõenda tegelikku käitumist Androidi seadmel, seega lisa UI muudatustele võimalusel testiseadme tulemused.

## Tõlkimine

Kasutajaliidese algsed ressursid on `app/src/main/res/values/strings.xml`, eestikeelsed `app/src/main/res/values-et/strings.xml`. Säilita `%1$s`, `%2$d` jms parameetrid; ära muuda `translatable="false"` märgitud ressursse.

## Peamised failid

| Asukoht | Roll |
|---|---|
| `app/src/main/java/app/buddy/assistant/MainActivity.java` | Vestluste avavaade |
| `app/src/main/java/app/buddy/assistant/Bubble.java` | Ujuvmull |
| `app/src/main/java/app/buddy/assistant/DoomFaces.java` | Ujuvmulli näoolekud |
| `app/src/main/java/app/buddy/assistant/Ui.java` | Androidi UI komponendid |
| `app/src/main/java/app/buddy/assistant/Theme.java` | Kujunduse teemad ja värvid |
| `app/src/main/java/app/buddy/assistant/Guard.java` | Turva- ja kinnitamisloogika |
| `app/src/main/java/app/buddy/assistant/McpServer.java` | Kohalik MCP |
| `app/src/main/java/app/buddy/assistant/BuildInfo.java` | Rakenduse versioon |
| `branding/` | NOX01 vektorlogo |
| `docs/graphics/` | Dokumendi tehnilised illustratsioonid |

Märkus: sisemiste pakettide, klasside ja failiteede ümbernimetamine ilma migratsiooniplaanita võib olemasolevad installid katki teha. Avalik bränd on NOX01.

## Litsents

[Apache 2.0](LICENSE). Olemasolevate kolmanda osapoole komponentide ning lähtekoodi autoriõiguse märked tuleb säilitada.
