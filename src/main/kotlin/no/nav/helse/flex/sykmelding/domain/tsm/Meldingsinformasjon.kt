package no.nav.helse.flex.sykmelding.domain.tsm

import no.nav.helse.flex.sykmelding.domain.tsm.values.Organisasjon
import java.time.OffsetDateTime

enum class MetadataType {
    ENKEL,
    EMOTTAK,
    UTENLANDSK_SYKMELDING,
    PAPIRSYKMELDING,
    EGENMELDT,
}

sealed interface Meldingsinformasjon {
    val type: MetadataType
    val vedlegg: List<String>?
}

data class Egenmeldt(
    val msgInfo: MeldingMetadata,
) : Meldingsinformasjon {
    override val type: MetadataType = MetadataType.EGENMELDT
    override val vedlegg: List<String> = emptyList()
}

data class Papirsykmelding(
    val msgInfo: MeldingMetadata,
    val sender: Organisasjon,
    val receiver: Organisasjon,
    val journalPostId: String,
) : Meldingsinformasjon {
    override val vedlegg = null
    override val type = MetadataType.PAPIRSYKMELDING
}

data class Utenlandsk(
    val land: String,
    val journalPostId: String,
) : Meldingsinformasjon {
    override val vedlegg = null
    override val type: MetadataType = MetadataType.UTENLANDSK_SYKMELDING
}

data class EmottakEnkel(
    val msgInfo: MeldingMetadata,
    val sender: Organisasjon,
    val receiver: Organisasjon,
    override val vedlegg: List<String>?,
) : Meldingsinformasjon {
    override val type = MetadataType.ENKEL
}

data class EDIEmottak(
    val mottakenhetBlokk: MottakenhetBlokk,
    val ack: Ack,
    val msgInfo: MeldingMetadata,
    val sender: Organisasjon,
    val receiver: Organisasjon,
    val pasient: MetadataPasient?,
    override val vedlegg: List<String>?,
) : Meldingsinformasjon {
    override val type = MetadataType.EMOTTAK
}

enum class Meldingstype {
    SYKMELDING,
}

data class MeldingMetadata(
    val type: Meldingstype,
    val genDate: OffsetDateTime,
    val msgId: String,
    val migVersjon: String?,
)

data class MottakenhetBlokk(
    val ediLogid: String,
    val avsender: String,
    val ebXMLSamtaleId: String,
    val mottaksId: String?,
    val meldingsType: String,
    val avsenderRef: String,
    val avsenderFnrFraDigSignatur: String?,
    val mottattDato: OffsetDateTime,
    val orgnummer: String?,
    val avsenderOrgNrFraDigSignatur: String?,
    val partnerReferanse: String,
    val herIdentifikator: String,
    val ebRole: String,
    val ebService: String,
    val ebAction: String,
)

enum class AckType {
    JA,
    NEI,
    KUN_VED_FEIL,
    IKKE_OPPGITT,
    UGYLDIG,
}

data class Ack(
    val ackType: AckType,
)

data class MetadataPasient(
    val ids: List<MetadataId>,
    val navn: MetadataNavn,
    val fodselsdato: String,
    val kjonn: String,
    val nasjonalitet: String?,
    val adresse: MetadataAdresse,
    val kontaktinfo: List<MetadataKontaktinfo>,
)

data class MetadataId(
    val id: String,
    val type: String,
)

data class MetadataNavn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
)

data class MetadataAdresse(
    val type: String,
    val gateadresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val postboks: String?,
    val kommune: String?,
    val land: String?,
)

data class MetadataKontaktinfo(
    val type: String,
    val value: String,
)
