# NOX01 Mobile privaatsus

**Uuendatud:** 17. november 2026

NOX01 Mobile on avatud lähtekoodiga Androidi AI-rakendus. Selle töö sõltub sinu valitud AI-teenusepakkujast ning antud Androidi õigustest. Vaata lähtekoodi [GitHubis](https://github.com/levomm/nox01-mobile).

## Millist teavet kasutatakse?

**Ekraani juhtimine.** Androidi juurdepääsetavusteenus võimaldab agendil lugeda ekraanil nähtavat sisu, puudutada, sisestada teksti ning kerida. Nähtav sisu võib sisaldada teiste inimeste sõnumeid ja isikuandmeid. Teenuse saad Androidi seadetes välja lülitada.

**Teavitused.** Teavitusreeglid vajavad eraldi süsteemiluba. Sobiva reegli käivitumisel antakse vajalik teavituse sisu valitud agendile, kes võib seda töödelda oma AI-teenuse kaudu. Ära luba automaatset vastamist tundlikes vestlustes, kui sa pole reegleid üle kontrollinud.

**Vestlused, manused ja hääl.** Sisestatud tekst, lisatud failid, kõnesisend ja tööks vajalik ekraaniteave võivad jõuda agenti teenindava mudeli või kõnetuvastuse teenuseni.

## Valitud mudeli teenusepakkuja

Agent võib kasutada näiteks Anthropic, OpenAI, Google, xAI või OpenCode'i kaudu ühendatud mudelite teenuseid. Nende ettevõtete andmetöötlusele kehtivad nende enda tingimused. Kui kasutad sobivat enda serveris töötavat mudelit, on andmete liikumine teistsugune kui pilveteenuse korral.

## Telefoni salvestatud andmed

Rakendus säilitab vestlused, seadistused, ühendused ja tööfailid oma rakenduse andmeruumis. Desinstallimine võib need andmed kustutada. Võimalusel tee enne eemaldamist varukoopia.

## Võrgupäringud

Agentide allalaadimiseks, autentimiseks ja kasutamiseks on vajalik võrguühendus teenusepakkujatega. GitHubi väljalasete kontroll võib kasutada GitHubi API-t. Rakenduse paikne MCP-liides kuulab telefoni loopback-aadressil ning nõuab tokenit. See ei taga, et ühendatud väline AI-teenus andmeid ei saa.

## Solana / Phantom rahakott (arendusversioon)

Rahakoti menüüsse saad sisestada Solana **avaliku aadressi**, mille põhjal küsib NOX01 Solana RPC-lt SOL-i ja ametliku Solana USDC saldot ning avalikku tehinguajalugu. RPC teenusepakkuja näeb päringuga seotud avalikku aadressi ja võrgupäringu tehnilisi andmeid.

Kui valid **Ühenda Phantom**, avaneb eraldi Phantomi rakendus, kus kinnitad ühendamise. Phantomi vastus on krüpteeritud ja NOX01 salvestab ühenduse toimimiseks vajaliku sessiooni ning rakenduse X25519 võtme Android Keystore'i kaitstud krüpteeritud salvestusse. NOX01 ei küsi Phantomi rahakoti privaatvõtit ega taastefraasi. Androidi ligipääsetavusteenuse tõttu on rahakotiga seotud ekraanitoimingud erilise riskiga: **agentidel pole siin maksetööriistu ning saatmine pole selle arendusetapi ehituses lubatud**.

Rahakoti lokaalsed andmed saad kustutada nupuga **Eemalda aadress**. Phantomi enda usaldatud rakenduste loendist tuleb ühendus vajadusel eraldi tühistada. Tegemist on testimata arendusfunktsiooniga, mitte valmis makseteenusega.

## Õigused ja kontroll

NOX01 võib küsida juurdepääsu ekraanile, teavitustele, mikrofonile, alarmidele, taustatööle või pakettide paigaldamisele. Kõigi õiguste andmine pole iga kasutusviisi jaoks vajalik. Androidi seadetes saab antud õigusi hiljem muuta.

Enne sõnumi saatmist, ostmist, kustutamist või muud olulist tegevust peaksid üle vaatama rakenduse kinnitamisreeglid. Mõned automaatikareeglid võivad olla sinu eraldi seadistusel lubatud automaatseks käivitamiseks.

## Küsimused

Privaatsusküsimused: [ava projekti GitHubi issue](https://github.com/levomm/nox01-mobile/issues). Turvahaavatavustest teata privaatselt [turvapoliitika](../SECURITY.md) järgi.

NOX01 ei ole Anthropic, OpenAI, Google ega xAI ametlik rakendus.
