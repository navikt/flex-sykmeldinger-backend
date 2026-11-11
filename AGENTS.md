# AGENTS.md - `flex-sykmeldinger-backend`
Repoet er en Spring Boot backend for håndtering av sykmeldinger i Flex. 
Den tilbyr API-endepunkter for å hente, opprette og oppdatere sykmeldinger.

## 1) Formatering, bygg og tester

Bruk IntelliJ MCP og run configurations for Gradle-oppgaver, ikke shell. Kjør formatering og relevante tester før commit.

`build` kjører også tester. Bruk `classes` for kompilering uten tester.

### Opprette en Gradle-konfigurasjon

1. Finn run configurations med `get_run_configurations`. Gradle-oppgaver kan finnes selv om de ikke er listet som run configurations.
2. Mangler konfigurasjonen og MCP ikke kan opprette den, kopier en Gradle-konfigurasjon under `RunManager` i `.idea/workspace.xml`. Gi kopien et nytt navn, sett `taskNames` til ønsket oppgave og fjern uvedkommende `scriptParameters`. Behold eksisterende konfigurasjoner.
3. Åpne `workspace.xml` i IntelliJ og kontroller med `get_run_configurations` at konfigurasjonen er lastet inn. Kjør den med `execute_run_configuration`. Ikke commit lokale IDE-innstillinger.

Ikke bruk IntelliJs terminalverktøy til å omgå begrensninger i shell-sandkassen.

## 2) Testing

Følg [testreglene for Kotlin og Spring](AGENTS-testing.md).

- Prioriter tester for endret domenelogikk
- Foretrekk fakes der det ikke trengs integrasjonstester
- Fake-tester arver fra `FakesTestOppsett` (uten database)
- Integrasjonstester arver fra `IntegrasjonTestOppsett` (Testcontainers + Kafka)
- Fakes for alle eksterne klienter ligger i `testconfig/fakes/`
- Testdata-byggere ligger i `testdata/`

## 3) Prosjektstruktur

```
api/                    REST-kontrollere og DTO-er
arbeidsforhold/         Arbeidsforhold fra Aareg – domene og repository
arbeidsgiverdetaljer/   Sammenstilling av arbeidsgiverdetaljer
config/                 Spring-konfigurasjon (Kafka, auth, scheduler, m.m.)
gateways/               Integrasjoner: Aareg, Ereg, PDL, NarmesteLeder, Kafka-lyttere/-produsenter
narmesteleder/          Nærmeste leder – domene og repository
sykmelding/             Kjerndomene: mottak, lagring og lesing av sykmeldinger (inkl. TSM-modell)
sykmeldinghendelse/     Brukerhendelser, brukersvar og statushåndtering
tsmsykmeldingstatus/    Konvertering til Kafka-DTO-format for TSM
utils/                  Felles hjelpere (Jackson, Kafka, logging)
```

**Eksterne avhengigheter:** Aareg, Ereg, PDL, syketilfelle, sykepengesoknad-backend (TokenX via Texas)

**Auth:** TokenX inn (ditt-sykefravaer, sykepengesoknad, sykepengesoknad-backend) · Azure AD client_credentials ut (PDL, Aareg) · TokenX token exchange ut (flex-syketilfelle, sykepengesoknad-backend)

**Infrastruktur:** Nais/GCP · PostgreSQL med Flyway · Valkey (sesjoner) · namespace `flex`

## 4) Kodestil

- All kode, kommentarer og UI-tekst på **norsk bokmål**
- Les tilsvarende produksjonskode og tester før du velger løsning. Følg mønsteret i hvert repo, ikke kopier ukritisk mellom repoer.
- Implementer bare avtalt omfang og migreringsfase. Unngå ekstra funksjoner, opprydding og formateringsstøy.

## 5) Git-workflow

- Egen branch per feature/fix, aldri direkte på `main`
- Hold commit-meldinger korte, beskrivende, én linje, uten punktum
- Ingen conventional commit-prefix og ingen issue-nummer påkrevd

Standard flyt:

```sh
git checkout -b kort-beskrivende-navn
# Kjør formatering, relevante tester og bygg via IntelliJ
git commit -m "Kort beskrivelse"
git push origin <branch>
gh pr create --fill
```

## 6) Grenser (aldri gjør dette)

- Aldri lekke eller logge sensitiv informasjon (fnr, tokens, session-data)
- Aldri hardkode hemmeligheter eller credentials
- Aldri commit med rød format/test/build

## 7) Verktøypreferanser

- Foretrekk **IntelliJ MCP** (`com-jetbrains-intellij-*`) for søk og teksterstatning i kode
- Bruk `search_in_files_by_text`/`search_in_files_by_regex` fremfor grep/ripgrep
- Bruk `replace_text_in_file` fremfor manuell redigering der mulig
- Bruk `find_files_by_glob`/`find_files_by_name_keyword` fremfor shell-basert filsøk
- IntelliJ MCP skal alltid være tilgjengelig i dette repoet

## Når du trenger mer kontekst

- `README.md` - prosjektformål og dataflyt

## Hurtigsjekk før levering

- [ ] Endringen følger eksisterende mønster i berørte filer
- [ ] Tester er oppdatert der domenelogikk er endret
- [ ] format, build og test er grønn
