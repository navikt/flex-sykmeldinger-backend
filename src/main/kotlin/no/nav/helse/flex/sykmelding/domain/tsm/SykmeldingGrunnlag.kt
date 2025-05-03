package no.nav.helse.flex.sykmelding.domain.tsm

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
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
    val navn: AvsenderSystemNavn,
    val versjon: String,
) {
    internal class AvsenderSystemSerializer : JsonSerializer<AvsenderSystem>() {
        override fun serialize(
            value: AvsenderSystem?,
            gen: JsonGenerator?,
            serializers: SerializerProvider?,
        ) {
            if (value != null) {
                gen?.writeStartObject()
                gen?.writeStringField("navn", value.navn.displayName)
                gen?.writeStringField("versjon", value.versjon)
                gen?.writeEndObject()
            }
        }
    }

    internal class AvsenderSystemDeserializer : JsonDeserializer<AvsenderSystem>() {
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext,
        ): AvsenderSystem {
            val node: JsonNode = p.codec.readTree(p)
            val navnString = node.get("navn")?.asText()
            val versjon = node.get("versjon")?.asText() ?: ""

            val navn = navnString.let { AvsenderSystemNavn.fraDisplayName(it) }

            return AvsenderSystem(navn, versjon)
        }
    }
}

enum class AvsenderSystemNavn(
    val displayName: String,
) {
    AD_CURIS("Ad Curis"),
    ANITA_EPJ("Anita EPJ"),
    APERTURA_EYE("Apertura-Eye"),
    CGM_JOURNAL("CGM Journal"),
    CGM_VISION("CGM Vision"),
    DIPS_ARENA("DIPS Arena"),
    DIPS_CLASSIC("DIPS Classic"),
    EGENMELDT("Egenmeldt"),
    EIA("EIA"),
    EPIC("Epic"),
    EXTENSOR_V2("Extensor V2"),
    HANO("HANO"),
    INFODOC_PLENARIO("Infodoc Plenario"),
    METODIKA_EPM("Metodika EPM"),
    OPUS_DENTAL("Opus Dental"),
    PAPIRSYKMELDING("Papirsykmelding"),
    PASIENT_SKY("PasientSky"),
    PHYSICA("Physica"),
    PRIDOK_EPJ("Pridok EPJ"),
    PROMED("ProMed"),
    PSYKBASE("PsykBase"),
    SKALPELL("Skalpell"),
    SOLV_IT_JOURNAL("SolvIT Journal"),
    SYFOSERVICE("SYFOSERVICE"),
    SYK_DIG("syk-dig"),
    SYSTEM_X("System X"),
    VELFERD("Velferd"),
    WEBMED("WebMed"),

    UKJENT("Ukjent"),
    ;

    companion object {
        fun fraDisplayName(displayName: String?): AvsenderSystemNavn =
            entries.find { it.displayName.equals(displayName, ignoreCase = true) } ?: UKJENT
    }
}

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
    val tiltakNav: String?,
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
