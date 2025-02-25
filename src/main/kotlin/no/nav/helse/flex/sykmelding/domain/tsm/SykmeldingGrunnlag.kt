package no.nav.helse.flex.sykmelding.domain.tsm

import no.nav.helse.flex.sykmelding.domain.tsm.values.Behandler
import no.nav.helse.flex.sykmelding.domain.tsm.values.Pasient
import no.nav.helse.flex.sykmelding.domain.tsm.values.SignerendeBehandler
import java.time.LocalDate
import java.time.OffsetDateTime

enum class SykmeldingType {
    SYKMELDING,
    UTENLANDSK_SYKMELDING,
}

sealed interface ISykmeldingGrunnlag {
    val type: SykmeldingType
    val id: String
    val metadata: SykmeldingMetadata
    val pasient: Pasient
    val medisinskVurdering: MedisinskVurdering
    val aktivitet: List<Aktivitet>
}

data class UtenlandskSykmeldingGrunnlag(
    override val id: String,
    override val metadata: SykmeldingMetadata,
    override val pasient: Pasient,
    override val medisinskVurdering: MedisinskVurdering,
    override val aktivitet: List<Aktivitet>,
    val utenlandskInfo: UtenlandskInfo,
) : ISykmeldingGrunnlag {
    override val type = SykmeldingType.UTENLANDSK_SYKMELDING
}

data class SykmeldingGrunnlag(
    override val id: String,
    override val metadata: SykmeldingMetadata,
    override val pasient: Pasient,
    override val medisinskVurdering: MedisinskVurdering,
    override val aktivitet: List<Aktivitet>,
    val behandler: Behandler,
    val arbeidsgiver: ArbeidsgiverInfo,
    val signerendeBehandler: SignerendeBehandler,
    val prognose: Prognose?,
    val tiltak: Tiltak?,
    val bistandNav: BistandNav?,
    val tilbakedatering: Tilbakedatering?,
    val utdypendeOpplysninger: Map<String, Map<String, SporsmalSvar>>?,
) : ISykmeldingGrunnlag {
    override val type = SykmeldingType.SYKMELDING
}

data class AvsenderSystem(
    val navn: String,
    val versjon: String,
)

data class SykmeldingMetadata(
    val mottattDato: OffsetDateTime,
    val genDate: OffsetDateTime,
    val behandletTidspunkt: OffsetDateTime,
    val regelsettVersjon: String?,
    val avsenderSystem: AvsenderSystem,
    val strekkode: String?,
)

data class BistandNav(
    val bistandUmiddelbart: Boolean,
    val beskrivBistand: String?,
)

data class Tiltak(
    val tiltakNAV: String?,
    val andreTiltak: String?,
)

data class Prognose(
    val arbeidsforEtterPeriode: Boolean,
    val hensynArbeidsplassen: String?,
    val arbeid: IArbeid?,
)

data class Tilbakedatering(
    val kontaktDato: LocalDate?,
    val begrunnelse: String?,
)

data class UtenlandskInfo(
    val land: String,
    val folkeRegistertAdresseErBrakkeEllerTilsvarende: Boolean,
    val erAdresseUtland: Boolean?,
)

data class SporsmalSvar(
    val sporsmal: String?,
    val svar: String,
    val restriksjoner: List<SvarRestriksjon>,
)

enum class SvarRestriksjon {
    SKJERMET_FOR_ARBEIDSGIVER,
    SKJERMET_FOR_PASIENT,
    SKJERMET_FOR_NAV,
}
