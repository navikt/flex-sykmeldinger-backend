package no.nav.helse.flex.sykmelding.domain

data class Pasient(
    val navn: Navn?,
    val navKontor: String?,
    val navnFastlege: String?,
    val fnr: String,
    val kontaktinfo: List<Kontaktinfo>,
)

data class Behandler(
    val navn: Navn,
    val adresse: Adresse?,
    val ids: List<PersonId>,
    val kontaktinfo: List<Kontaktinfo>,
)

data class SignerendeBehandler(
    val ids: List<PersonId>,
    val helsepersonellKategori: HelsepersonellKategori,
)

data class PersonId(
    val id: String,
    val type: PersonIdType,
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String
)

data class Kontaktinfo(
    val type: KontaktinfoType, val value: String
)

data class Adresse(
    val type: AdresseType,
    val gateadresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val postboks: String?,
    val kommune: String?,
    val land: String?,
)

enum class PersonIdType {
    FNR,
    DNR,
    HNR,
    HPR,
    HER,
    PNR,
    SEF,
    DKF,
    SSN,
    FPN,
    XXX,
    DUF,
    IKKE_OPPGITT,
    UGYLDIG;

}

enum class HelsepersonellKategori {
    HELSESEKRETAR,
    KIROPRAKTOR,
    LEGE,
    MANUELLTERAPEUT,
    TANNLEGE,
    FYSIOTERAPEUT,
    SYKEPLEIER,
    HJELPEPLEIER,
    HELSEFAGARBEIDER,
    USPESIFISERT,
    UGYLDIG,
    JORDMOR,
    AUDIOGRAF,
    NAPRAPAT,
    AMBULANSEARBEIDER,
    PSYKOLOG,
    FOTTERAPEUT,
    TANNHELSESEKRETAR,
    IKKE_OPPGITT;
}

enum class KontaktinfoType {
    TELEFONSVARER,
    NODNUMMER,
    FAX_TELEFAKS,
    HJEMME_ELLER_UKJENT,
    HOVEDTELEFON,
    FERIETELEFON,
    MOBILTELEFON,
    PERSONSOKER,
    ARBEIDSPLASS_SENTRALBORD,
    ARBEIDSPLASS_DIREKTENUMMER,
    ARBEIDSPLASS,
    TLF,
    IKKE_OPPGITT,
    UGYLDIG;
}

enum class AdresseType {
    BOSTEDSADRESSE,
    FOLKEREGISTERADRESSE,
    FERIEADRESSE,
    FAKTURERINGSADRESSE,
    POSTADRESSE,
    BESOKSADRESSE,
    MIDLERTIDIG_ADRESSE,
    ARBEIDSADRESSE,
    UBRUKELIG_ADRESSE,
    UKJENT,
    UGYLDIG;
}
