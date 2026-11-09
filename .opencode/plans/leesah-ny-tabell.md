# Ny Leesah-projeksjon for nærmeste leder

## Status etter implementering

Separat tabell, checkpoint, consumergruppe og eksplisitt leseskifte er implementert.
Ny consumer er av og lesemodus er `GAMMEL` som standard. Gammel writer følger
før-PR-semantikken; ny writer følger Kafka-offset og beholder inaktiv tilstand.
Siste IDE-kjørte format og full build er grønne med 577 tester uten feil.
Ingen deploy eller aktivering er utført. Kontrollpunktene for produksjon står åpne.

## Mål og avgrensning

- Bygg en tom, separat projeksjon fra `team-esyfo.syfo-narmesteleder-leesah` til `narmeste_leder_esyfo`.
- Behold `teamsykmelding.syfo-narmesteleder-leesah` som eneste skriver til `narmeste_leder`.
- Produksjonslesere bruker gammel tabell mens ny tabell bygges opp.
- Bytt lesing med eksplisitt konfigurasjon som har gammel tabell som standard.
- La begge consumers kjøre i rollback-vinduet. Stopp gammel consumer i en senere, egen endring.
- Ikke kopier gammel database, dual-write, bruk fallback eller lag UNION mellom tabellene.
- Ikke endre API-kontrakter eller gjør dette avhengig av de to andre backend-planene.
- Frontend er gjennomgått uten blokkerende funn: direkte eSyfo-API og DELETE → `NlAvbrutt` → team sykmelding-command → gammel Leesah → speiling er bekreftet.

## Utgangspunkt og berørte lesebaner

- Lokal branch er `split/narmesteleder-esyfo` på `0d40c4a3e27bd50f426688c5e15c08d9dd2a1a20`, kode oppå nyere `main`.
- Remote PR #671 har branch `narmesteleder-esyfo-migrering` med `ad6f31a` og AGENTS-endring; dokumentasjons-PR #672 er separat.
- Force-push er blokkert av cplt. Planen forutsetter ingen git-endringer eller synkronisering av brancher.
- Lokal `.gitignore` og `.codegraph/` tilhører eksisterende arbeidsstatus og skal stå urørt.
- Ved planstart hadde `NarmestelederKafkaListener.kt` to lyttere som delte `behandle` og skrev til samme gamle repository.
- Begge brukte `auto.offset.reset = latest`. Implementert ny lytter har egen gruppe og starter fra tidligste tilgjengelige offset.
- Den gamle lytteren ignorerer tombstones; den nye må slette raden for tilsvarende key.
- Ved planstart brukte gammel `OppdateringAvNarmesteLeder.kt` timestamp-sjekk og `lagreHvisNyere`; repository brukte `ON CONFLICT ... WHERE narmeste_leder.timestamp < EXCLUDED.timestamp`. Dette var semantikken fra første PR-løsning, ikke `origin/main` før PR.
- Gammel bane er nå tilbakeført til `origin/main`-oppførsel: aktiv melding lagres/oppdateres, inaktiv melding sletter eksisterende rad eller ignoreres om den mangler. Den har ingen timestamp-vakt. PII-sanitert logging og parsing er beholdt. Ny bane har separat offset/checkpoint.
- `NarmesteLederLeesah.kt` inneholder UUID, identifikatorer, orgnummer, aktiv-periode og timestamp, men ikke navn.
- `NarmesteLeder.kt` er merket `@Table("narmeste_leder")`; nytt repository må peke entydig på ny tabell.
- `ArbeidsgiverDetaljerService.kt` henter aktive ledere med `findAllByBrukerFnrInAndAktivTomIsNull`.
- Den kobler leder og arbeidsforhold i minnet via orgnummer, krever navn og velger høyeste `aktivFom`.
- `SykmeldingController.kt` bruker denne tjenesten til arbeidsgiverdetaljer i API-svaret.
- `TilleggsinfoSammenstillerService.kt` bruker samme tjeneste ved sammenstilling av arbeidsgiver og nærmeste leder.
- `TestdataResetListener.kt` sletter ledere via `findAllByBrukerFnrIn` i gammel repository under test/testdatareset.
- Sjekk at testdatareset også rydder ny projeksjon i testmiljø, uten å blande produksjonsskrivere.
- Ingen direkte SQL-join mot `narmeste_leder` finnes i gjennomgåtte `src/main`-treff.
- `ArbeidsforholdRepository` leverer arbeidsforhold separat; koblingen skjer i `ArbeidsgiverDetaljerService`.
- Gjennomgå eventuelle nye repository-kall og SQL-joins ved implementering før lesebryteren aktiveres.
- `V5__recreate_table_narmesteleder.sql` lager gammel tabell; `V6` gjør navn nullable.
- `V49__narmeste_leder_aktiv_tom.sql` legger `aktiv_tom` til gammel tabell i lokal PR.
- `AivenKafkaConfig.kt` bruker felles gruppe `flex-sykmeldinger-backend-consumer`, `MANUAL_IMMEDIATE` og ingen auto-commit.
- `application.yaml` har global `aiven-kafka.auto-offset-reset: none` og `flex.group.id` for auth, ikke denne Kafka-gruppen.
- `nais/app/dev.yaml`, `nais/app/prod.yaml` og `nais/app/naiserator.yaml` er gjeldende miljøkonfigurasjon.
- `SchedulerConfig.kt` har `@EnableAsync` og egen executor for Aareg; ikke bruk den for å omordne Leesah-records.
- `AivenKafkaErrorHandler.kt` styrer retry; sjekk at feil fra ny lytter aldri kvitteres som vellykket.

## Kontrakt og forutsetninger

- Kilde: [eSyfo-kontrakt for Leesah](https://github.com/navikt/esyfo-narmesteleder/blob/main/docs/kafka/syfo-narmesteleder-leesah.md).
- Siste record per key er riktig siste tilstand; ikke tolk topicet som komplett hendelseshistorikk.
- Konsumering er «at least once». Produsent-retry kan kort legge eldre tilstand etter nyere med ny offset.
- Loggrekkefølge gjelder per key og stabil partisjon, aldri på tvers av ulike keys.
- Key er `narmesteLederId` som UUID. Verifiser key mot verdien før projeksjonen lagres.
- Null-verdi er tombstone og sletter raden i ny projeksjon.
- Kun validerte records speiles. Orgnummer er alltid ni siffer; ukjente properties og statuser må tåles.
- Speilingen kopierer key og value uendret, men ikke offset; sammenlign aldri offsets mellom topicene.
- Topicet er compact med ubegrenset retention, omtrent 31 dagers minimum og 35 dagers maksimum lag.
- Tombstones utløper: bekreft faktisk `delete.retention.ms` og at bootstrap fullføres før denne grensen.
- Bekreft faktisk bootstrap-dekning og at speilingen er ajour før lesebytte.
- Avklar om kort mellomtilstand ved produsent-replay med ny offset tåles av live-lesere.
- Teknisk checkpoint fjerner duplikatbehandling, men kan ikke fjerne denne mellomtilstanden.
- Hvis live-lesere ikke tåler den: stopp migreringen, ikke legg inn timestamp-sortering som skjult kontrakt.

## Konkret implementeringsrekkefølge

1. Bekreft deploy-status for `V49` i dev/prod før noen vurderer å endre den.
2. Endre aldri en allerede anvendt Flyway-migrering. Behold gammel tabell lesbar i rollback-vinduet.
   Hvis `V49` ikke er anvendt i noe relevant miljø, erstatt PR-ens endring av gammel tabell med den nye tabellmigreringen. Hvis den er anvendt, behold historikken og tilpass ny kode uten å omskrive filen.
3. I `src/main/resources/db/migration/`: sjekk høyeste versjon ved implementering og reserver neste ledige nummer.
4. Lag migrering for tom `narmeste_leder_esyfo` med feltene i `NarmesteLeder`.
5. Ta med `aktiv_tom` som nullable, nullable navn og indeks på `bruker_fnr`; unik `narmeste_leder_id`.
6. La ny tabell ha alle felt uten å kreve endring i gammelt skjema; ikke migrer gamle rader.
7. Lag egen varig teknisk checkpoint-tabell med unik `(topic, partition)` og sist påførte offset.
8. Avgrens checkpoint til ny projeksjon, behold det også når en tombstone sletter domenefeltet.
9. I `narmesteleder/domain/`: bruk separat tabelltilknyttet modell eller eksplisitt mapping.
10. I `narmesteleder/`: opprett eget repository og egen skrivebane for ny projeksjon.
11. Behold `NarmesteLederRepository` og gammel skrive-/lesesemantikk fra før PR-en for gammel consumer. Bevaring av inaktive rader og nye filtre skal ligge på ny bane, ikke innføres på gammel tabell som del av denne migreringen. Hvis gammel bane allerede er deployet med endret semantikk, må tilbakeføring håndteres særskilt før videre deploy.
12. Ikke la `lagreHvisNyere` eller timestamp styre ny upsert. Record-rekkefølge er autoriteten.
13. I `gateways/NarmestelederKafkaListener.kt`: skill den gamle lytteren fra den nye.
14. Gammel topic fortsetter å skrive utelukkende til `narmeste_leder` med gjeldende semantikk.
15. Ny topic går utelukkende til ny skrivebane; ingen domenehendelser, commands eller andre forretningssideeffekter.
16. For ny record, valider key/UUID og les Leesah-payload; tåle ukjente felter og status.
17. For tombstone, slett ny rad etter key uten å parse en manglende value.
18. Behold eksisterende PDL-navneoppslag der det trengs for aktiv leder, men ikke send commands eller skriv andre domener.
19. Avklar PDL-kapasitet, feil og manglende navn ved bootstrap; ingen lesebytte før nødvendige navn finnes.
20. Skriv domene-upsert eller delete og checkpoint i én lokal DB-transaksjon.
21. Serialiser oppdateringer per `(topic, partition)` med DB-lås eller betinget oppdatering.
22. Løs også konkurrerende førstegangsinnsetting; sammenlign mot varig checkpoint under samme lås.
23. Avvis offset `<=` checkpoint. Ikke oppdater rad eller checkpoint ved slike gjentakelser.
24. Kvitter Kafka-offset først etter DB-commit; ved feil før commit skal både rad og checkpoint rulles tilbake.
25. Ack-feil etter commit skal kunne gi gjentatt record uten duplikateffekter.
26. Ingen asynkron behandling som endrer rekkefølge per partisjon.
27. Ikke beskriv Kafka og PostgreSQL som én distribuert transaksjon.
28. I `config/kafka/AivenKafkaConfig.kt`: gi ny lytter egen consumer-gruppe og isolert earliest-konfig.
29. Behold gammel gruppes offsets, fabrikk og lytter uendret ved oppstart av ny projeksjon.
30. Hvis ny gruppe allerede har committed offsets, gir `earliest` ikke restart fra begynnelsen.
31. Bruk ny bootstrap-gruppe og tom projeksjon ved behov; ikke reset aktiv gruppe blindt.
32. Ikke replay en ny gruppe inn i en ikke-tom projeksjon uten godkjent, eksplisitt prosedyre.
33. I `application.yaml` og miljøfilene `nais/app/{dev,prod}.yaml`: innfør eksplisitt lesemodus.
34. Standard er gammel tabell også når ny consumer er deployet og fyller ny tabell.
35. I `ArbeidsgiverDetaljerService.kt`: velg ett repository per kall ut fra lesemodus.
36. Ny lesebane filtrerer `aktivTomIsNull`. Gammel bane bruker før-PR-semantikken der inaktive ledere slettes; hvis den allerede beholder inaktive rader, må et aktiv-filter beholdes til dette er håndtert. Behold kobling til arbeidsforhold og eksisterende behandling av manglende navn.
37. La `SykmeldingController` og `TilleggsinfoSammenstillerService` fortsette via samme tjeneste.
38. Behold API-DTO-er og endepunkter uendret. Ingen UNION eller fallback ved manglende ny rad.
39. Gjennomgå `TestdataResetListener` og testoppsett for separat sletting i test, uten produksjonsrebuild.
40. Oppdater bare dokumentasjon i eksisterende README ved faktisk implementering, ikke i dette planoppdraget.

Behold teknisk `id`/ID-generering hvis den nye modellen bruker eksisterende Spring Data-mapping.
Radlåsen ordner ikke records som applikasjonen selv har startet i feil rekkefølge.
Behold én sekvensiell behandlingsbane per partisjon, også ved rebalance, og test
at ingen tidligere upåført record hoppes over. Kompaktering gir lovlige offset-hull;
checkpoint-logikken skal ikke kreve fortløpende heltall.

## Tester som må være grønne før lesebytte

- Bruk `FakesTestOppsett` til domenetester og `IntegrasjonTestOppsett` til PostgreSQL/Kafka-transaksjoner.
- Test gammelt topic → bare gammel tabell og nytt topic → bare ny tabell, også ved samme UUID.
- Test compact-bootstrap der bare siste record per key er igjen: riktig aktiv/inaktiv tilstand.
- Test tom ny tabell, tom tombstone, ukjent key og create → null → create med nye offsets.
- Test duplicate offset, lavere offset, restart, rebalansering og samme UUID på stabil partisjon.
- Test parallell førstegangsinnsetting, checkpoint-lås og at høyere offset vinner uten timestamp-guard.
- Test at databasefeil gir rollback uten ack, og at ack-feil etter commit tåler retry. Test tom partisjon og kompakterte offset-hull ved bootstrap-barrieren.
- Test at produsent-replay med ny offset kan gi mellomtilstand; dokumenter go/no-go for lesere.
- Test ukjent JSON-property/status, UUID-key, orgnummer og upåvirket ny projeksjon ved feil record.
- Test PDL-navn for aktiv leder, nullable navn og `aktivTom` ved avkrefting og reaktivering.
- Test at standard leser gammel tabell og konfigurasjonsbytte leser ny uten fallback.
- Test `SykmeldingController` og `TilleggsinfoSammenstillerService` via arbeidsgiverdetaljer.
- Test at `TestdataResetListener` og fakes/integrasjonstester rydder begge adskilte tabeller.
- Erstatt eksisterende tester som forventer at begge topics skriver samme rad.
- Test gammel consumers før-PR-semantikk uten timestamp-vakt; ny bane beholder inaktiv rad separat.
- Kjør format, build og tester bare via IntelliJ MCP-run configurations ved implementering.
- Eldre AGENTS-eksempel med shell-kommandoer overstyrer ikke kravet om IntelliJ MCP her.

## Utrulling, kontrollpunkter og rollback

- 🔴 Rød sone: nøkkelvalidering, transaksjon, checkpoint og lesebytte krever manuell kodegjennomgang.
- Før deploy: bekreft eSyfo-ACL for read på nytt topic i dev og prod samt faktisk topic-konfigurasjon.
- Bekreft key/partition-stabilitet, speiling ajour og `delete.retention.ms` med eSyfo.
- Ta vare på ikke-personidentifiserende startoffset og fang end-offset per partisjon ved bootstrap-start.
- Observer checkpoint, end-offset, lag, tombstone-antall og feil per topic/partisjon uten key, payload eller persondata.
- End-offset er eksklusiv. Kontroller at consumer-posisjonen har passert tilgjengelige records før hver fanget grense, og at alle er DB-committet uten utestående behandling. Checkpoint er siste faktisk behandlede record; krev ikke likhet med `endOffset - 1` ved tom partisjon eller kompakterte hull. Skriv aldri barrieren direkte til checkpoint. Kontroller akseptabel live-lag etterpå og at speileren er ajour, ikke bare den nye consumeren.
- Sammenlign aggregert dekning og aktive tellinger mot forventning; ingen persondata i logger eller metrikker.
- Gate 1: tomt skjema og gammel lesemodus deployes før ny consumer starter fra earliest.
- Gate 2: bootstrap fullført innen tombstone-vinduet, riktig navn/aktivitet og ny consumer ajour.
- Gate 3: godkjenn replay-mellomtilstand eksplisitt før ny lesemodus settes i dev og deretter prod.
- Unngå blandede lesebaner i rolling pods: hold gammel lesemodus gjennom deploy, bytt konfig etter at alle pods har ny kode.
- Under lesebytte: følg versjon og lesemodus per pod; ikke erklær byttet fullført før alle pods er samstemte.
- Ved feil: sett lesemodus tilbake til gammel, deploy om nødvendig og bekreft alle pods.
- Begge consumers fortsetter under rollback-vinduet, slik at gammel tabell fortsatt har ferske data.
- Rollback forutsetter også at produsenten fortsatt leverer nødvendige endringer på gammelt topic. En kjørende consumer med null lag på et tausnet topic beviser ikke fersk tilstand. Koordiner eSyfos produsentovertakelse med rollback-vinduet; gammel tabell er ikke et gyldig rollback-mål etter at den mister oppdateringer.
- Ikke stopp gammel consumer, dropp gammel tabell eller kjør offset-reset som del av rollback.
- Senere, i egen endring etter godkjent vindu, stopp gammel consumer eksplisitt og marker gammel tabell foreldet.
- Ingen tvungen push, branch-bytte eller endring i PR #671/#672 inngår i denne planen.

## Implementeringsstatus 9. november 2026

- Lagt til V50 med separat tabell og varig checkpoint. V49 er urørt; deploy-status er ikke kontrollert.
- Ny consumergruppe har `earliest`, sekvensiell behandling og avslått oppstart som standard. Gammel consumer og gamle gruppe-offsets er beholdt. Lesemodus er `GAMMEL` som standard; `ESYFO` krever aktiv ny consumer.
- Ny skrivebane validerer topic, partisjon og offset før DB-tilgang, låser checkpoint, håndterer tombstones og skriver rad/checkpoint i samme DB-transaksjon. Ack skjer etter commit. Ny lesebane bruker bare ny tabell, med aktiv-filter og samme navneregler. Checkpoint-metrikken leser varig DB-verdi også etter restart/rebalance.
- IDE-konfigurasjonene `flex-sykmeldinger-backend [ktlintFormat]` og `Migration retry build` (`build`, inkludert hele testsuiten) er grønne etter korrigering av prod-fakens PDL-forventning. Ingen deploy eller lesebytte er utført.
- **Manuelle go/no-go-gates er åpne:** ACL i dev/prod, faktisk retention og `delete.retention.ms`, speiling og bootstrap-dekning per partisjon (inkludert kompakterte hull), PDL-kapasitet/navnedekning, toleranse for produsent-replay med nyere offset og eldre tilstand, samstemt read-switch per pod, samt kontrollert rollback til gammel lesemodus. Kontroller V49-deploy-status før deploy.
- Korrigering: Personbasert testdatareset i dev/test kan slette ESYFO-rader uten å nullstille partisjonscheckpoint; det er ikke bootstrap-reset. Begge reset-filene er bevart uendret i denne oppfølgingen. Tidligere påstand om at slik sletting krever nullstilling av checkpoint var for streng. Etter de to P2-fiksene er `flex-sykmeldinger-backend [ktlintFormat]` og `Migration retry build` grønne på gjeldende checkout, inkludert reset-integrasjonstesten.
- Oppfølging mot `origin/main`: gammel writer på HEAD hadde fortsatt første PRs timestamp-vakt og inaktive rader. Gammel writer/repository/fake og tilhørende tester er nå endret til før-PR-semantikk (aktiv lagring/oppdatering, inaktiv sletting eller ignorering når raden mangler, ingen timestamp-vakt). V49 er urørt fordi deploy-status ikke er kjent. Gammelleser filtrerer fortsatt eventuelle allerede lagrede inaktive rader; ingen bulkcleanup. IDE-konfigurasjonene `flex-sykmeldinger-backend [ktlintFormat]` og `Migration retry build` var grønne etter rettingen: 575 tester, null feil. De to reset-filene er uendret i denne oppfølgingen.
- Siste PDL-fiks på bare ny bane: funksjonelle PDL-feil gir nullable navn i prod (dev beholder standardnavn), med sanitert logg. Tekniske feil gir fortsatt rollback og retry uten ack/checkpoint. Navneoppslaget er fortsatt innenfor DB-låsen: å flytte det ut krever ny duplikatkontroll etter oppslaget og ville gi unødvendige PDL-kall ved replay; dette er ikke innført her. IDE-format og full `Migration retry build` er grønne på gjeldende checkout: 577 tester, null feil, også etterfulgt tombstone/annen key og teknisk PDL-feil. Reset-filene er ikke endret.
