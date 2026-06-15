# AGENTS.md - `flex-sykmeldinger-backend`
Repoet er en Spring Boot backend for håndtering av sykmeldinger i Flex. 
Den tilbyr API-endepunkter for å hente, opprette og oppdatere sykmeldinger.

## 1) Kommandoer (må kjøres før commit)

```sh
./gradlew ktlintFormat # formater kode med Ktlint
./gradlew test # kjør tester
./gradlew build # bygg for produksjon
```

## 2) Testing

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
- Bruk eksisterende mønstre i koden fremfor nye varianter

## 5) Git-workflow

- Egen branch per feature/fix, aldri direkte på `main`
- Hold commit-meldinger korte, beskrivende, én linje, uten punktum
- Ingen conventional commit-prefix og ingen issue-nummer påkrevd

Standard flyt:

```sh
git checkout -b kort-beskrivende-navn
./gradlew ktlintFormat # formater kode med Ktlint
./gradlew test # kjør tester
./gradlew build # bygg for produksjon
git commit -m "Kort beskrivelse"
git push origin <branch>
gh pr create --fill
```

## 6) Grenser (aldri gjør dette)

- Aldri lekke eller logge sensitiv informasjon (fnr, tokens, session-data)
- Aldri hardkode hemmeligheter eller credentials
- Aldri commit med rød format/test/build

## Når du trenger mer kontekst

- `README.md` - prosjektformål og dataflyt

## Hurtigsjekk før levering

- [ ] Endringen følger eksisterende mønster i berørte filer
- [ ] Tester er oppdatert der domenelogikk er endret
- [ ] format, build og test er grønn

