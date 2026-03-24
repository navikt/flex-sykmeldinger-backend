package no.nav.helse.flex.api

import no.nav.helse.flex.api.dto.*
import java.time.LocalDate
import java.time.OffsetDateTime

object SykmeldingDtoRegler {
    fun skjermForPasientDersomSpesifisert(sykmeldingDto: SykmeldingDTO): SykmeldingDTO =
        if (sykmeldingDto.skjermesForPasient) {
            sykmeldingDto.copy(
                medisinskVurdering = null,
                utdypendeOpplysninger = emptyMap(),
                tiltakNAV = null,
                andreTiltak = null,
                meldingTilNAV = null,
            )
        } else {
            sykmeldingDto
        }
}

data class FlexInternalPasientDto(
    val fnr: String? = null,
    val overSyttiAar: Boolean? = null,
)

data class FlexInternalSykmeldingDto(
    val id: String,
    val pasient: FlexInternalPasientDto,
    val mottattTidspunkt: OffsetDateTime,
    val behandlingsutfall: BehandlingsutfallDTO,
    val arbeidsgiver: ArbeidsgiverDTO,
    val sykmeldingsperioder: List<SykmeldingsperiodeDTO>,
    val sykmeldingStatus: SykmeldingStatusDTO,
    val skjermesForPasient: Boolean,
    val kontaktMedPasient: KontaktMedPasientDTO,
    val behandletTidspunkt: OffsetDateTime,
    val syketilfelleStartDato: LocalDate?,
    val egenmeldt: Boolean,
    val papirsykmelding: Boolean,
    val harRedusertArbeidsgiverperiode: Boolean,
    val merknader: List<MerknadDTO>?,
    val signaturDato: OffsetDateTime?,
    val utenlandskSykmelding: UtenlandskSykmelding?,
) {
    companion object {
        fun konverterTilFlexInternal(sykmeldingDto: SykmeldingDTO): FlexInternalSykmeldingDto =
            FlexInternalSykmeldingDto(
                id = sykmeldingDto.id,
                pasient = FlexInternalPasientDto(fnr = sykmeldingDto.pasient.fnr, overSyttiAar = sykmeldingDto.pasient.overSyttiAar),
                mottattTidspunkt = sykmeldingDto.mottattTidspunkt,
                behandlingsutfall = sykmeldingDto.behandlingsutfall,
                arbeidsgiver = sykmeldingDto.arbeidsgiver,
                sykmeldingsperioder = sykmeldingDto.sykmeldingsperioder,
                sykmeldingStatus = sykmeldingDto.sykmeldingStatus,
                skjermesForPasient = sykmeldingDto.skjermesForPasient,
                kontaktMedPasient = sykmeldingDto.kontaktMedPasient,
                behandletTidspunkt = sykmeldingDto.behandletTidspunkt,
                syketilfelleStartDato = sykmeldingDto.syketilfelleStartDato,
                egenmeldt = sykmeldingDto.egenmeldt,
                papirsykmelding = sykmeldingDto.papirsykmelding,
                harRedusertArbeidsgiverperiode = sykmeldingDto.harRedusertArbeidsgiverperiode,
                merknader = sykmeldingDto.merknader,
                signaturDato = sykmeldingDto.signaturDato,
                utenlandskSykmelding = sykmeldingDto.utenlandskSykmelding,
            )
    }
}
