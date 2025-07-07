package no.nav.helse.flex.api.dto

data class MerknadDTO(
    val type: MerknadtypeDTO,
    val beskrivelse: String?,
)

enum class MerknadtypeDTO {
    DELVIS_GODKJENT,
    TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER,
    UGYLDIG_TILBAKEDATERING,
    UNDER_BEHANDLING,
    UKJENT_MERKNAD,
    TILBAKEDATERT_PAPIRSYKMELDING,
}
