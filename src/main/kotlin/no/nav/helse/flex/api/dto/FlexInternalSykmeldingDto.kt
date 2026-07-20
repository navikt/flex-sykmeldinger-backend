package no.nav.helse.flex.api.dto

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.flex.sykmeldinghendelse.SykmeldingHendelse
import no.nav.helse.flex.utils.toJsonNode
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

data class FlexInternalPasientDto(
    val fnr: String? = null,
    val overSyttiAar: Boolean? = null,
)

data class FlexInternalHendelseDto(
    val status: String,
    val brukerSvar: JsonNode?,
    val tilleggsinfo: JsonNode?,
    val source: String?,
    val hendelseOpprettet: Instant,
    val lokaltOpprettet: Instant,
) {
    companion object {
        fun fra(hendelse: SykmeldingHendelse) =
            FlexInternalHendelseDto(
                status = hendelse.status.name,
                brukerSvar = hendelse.brukerSvar?.toJsonNode(),
                tilleggsinfo = hendelse.tilleggsinfo?.toJsonNode(),
                source = hendelse.source,
                hendelseOpprettet = hendelse.hendelseOpprettet,
                lokaltOpprettet = hendelse.lokaltOpprettet,
            )
    }
}

data class FlexInternalSykmeldingDto(
    val id: String,
    val pasient: FlexInternalPasientDto,
    val mottattTidspunkt: OffsetDateTime,
    val behandlingsutfall: BehandlingsutfallDTO,
    val arbeidsgiver: ArbeidsgiverDTO,
    val sykmeldingsperioder: List<SykmeldingsperiodeDTO>,
    val sykmeldingStatus: SykmeldingStatusDTO,
    val hendelser: List<FlexInternalHendelseDto>,
    val skjermesForPasient: Boolean,
    val kontaktMedPasient: KontaktMedPasientDTO,
    val behandletTidspunkt: OffsetDateTime,
    val syketilfelleStartDato: LocalDate?,
    val egenmeldt: Boolean,
    val papirsykmelding: Boolean,
    val merknader: List<MerknadDTO>?,
    val signaturDato: OffsetDateTime?,
    val utenlandskSykmelding: UtenlandskSykmelding?,
) {
    companion object {
        fun fra(
            sykmeldingDto: SykmeldingDTO,
            hendelser: List<SykmeldingHendelse>,
        ) = FlexInternalSykmeldingDto(
            id = sykmeldingDto.id,
            pasient = FlexInternalPasientDto(fnr = sykmeldingDto.pasient.fnr, overSyttiAar = sykmeldingDto.pasient.overSyttiAar),
            mottattTidspunkt = sykmeldingDto.mottattTidspunkt,
            behandlingsutfall = sykmeldingDto.behandlingsutfall,
            arbeidsgiver = sykmeldingDto.arbeidsgiver,
            sykmeldingsperioder = sykmeldingDto.sykmeldingsperioder,
            sykmeldingStatus = sykmeldingDto.sykmeldingStatus,
            hendelser = hendelser.map { FlexInternalHendelseDto.fra(it) },
            skjermesForPasient = sykmeldingDto.skjermesForPasient,
            kontaktMedPasient = sykmeldingDto.kontaktMedPasient,
            behandletTidspunkt = sykmeldingDto.behandletTidspunkt,
            syketilfelleStartDato = sykmeldingDto.syketilfelleStartDato,
            egenmeldt = sykmeldingDto.egenmeldt,
            papirsykmelding = sykmeldingDto.papirsykmelding,
            merknader = sykmeldingDto.merknader,
            signaturDato = sykmeldingDto.signaturDato,
            utenlandskSykmelding = sykmeldingDto.utenlandskSykmelding,
        )
    }
}
