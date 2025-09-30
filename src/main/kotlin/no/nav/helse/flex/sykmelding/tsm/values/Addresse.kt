package no.nav.helse.flex.sykmelding.tsm.values

data class Adresse(
    val type: AdresseType,
    val gateadresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val postboks: String?,
    val kommune: String?,
    val land: String?,
)

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
    UGYLDIG,
}
