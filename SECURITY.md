# NOX01 Mobile turvalisus

NOX01 saab Androidi juurdepääsetavusteenuse kaudu töötada teiste rakendustega. See õigus on võimas ning selle kasutamine nõuab kontrollitud kinnitamisreegleid.

## Turvaaugust teatamine

Kui avastad viisi, kuidas agent saab kasutaja kinnitust vältida, varastada MCP-tokeni või pääseda ligi andmetele väljaspool lubatud tööulatust, **ära postita ekspluateerimisjuhiseid avalikku issue'sse**.

Saada teade privaatselt: [GitHub Security Advisory](https://github.com/levomm/nox01-mobile/security/advisories/new).

Lisa versioon, Androidi versioon, kasutatud agent, kordamise sammud ning oodatud ja tegelik tulemus. Ära saada päris paroole ega API-võtmeid.

## Riskikohad

- Veebilehtedelt, sõnumitest ja teavitustest pärit pahatahtlikud juhised.
- Tegevused, mille juures nõutav kasutaja kinnitus jääb esitamata.
- Liiga lai teavitusreegel, mis lubab automaatset vastamist.
- Lokaalse MCP-liidese tokeni või ühendatud teenuste võtmete lekkimine.
- Kolmanda osapoole AI-mudeli ekslikud ekraanitoimingud.
- Tundlike andmete edastamine pilveteenusepakkujale.

## Soovitused

Kasuta ainult usaldusväärset, sama püsiallkirjaga NOX01 APK-d. Hoia Android ja agendid uuendatud. Anna minimaalsed õigused. Loe automaatikareeglid üle ning kontrolli saajat ja sisu enne saatmist. Ära avalda MCP-porti ega tokeneid avalikku võrku.

## Toetatud versioon

Turvaparandused keskenduvad [viimasele GitHubi väljalaskele](https://github.com/levomm/nox01-mobile/releases/latest). Varasemaid APK-sid ei pruugita parandada.
