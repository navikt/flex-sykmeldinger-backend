package no.nav.helse.flex.sykmelding.tsm

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import no.nav.helse.flex.sykmelding.tsm.values.Behandler
import no.nav.helse.flex.sykmelding.tsm.values.Pasient
import no.nav.helse.flex.sykmelding.tsm.values.Sykmelder
import java.time.LocalDate
import java.time.OffsetDateTime

enum class SykmeldingType {
    XML,
    PAPIR,
    UTENLANDSK,
    DIGITAL,
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
    override val medisinskVurdering: IkkeDigitalMedisinskVurdering,
    override val aktivitet: List<Aktivitet>,
    val utenlandskInfo: UtenlandskInfo,
) : ISykmeldingGrunnlag {
    override val type = SykmeldingType.UTENLANDSK
}

sealed interface NorskSykmeldingGrunnlag : ISykmeldingGrunnlag {
    override val id: String
    override val metadata: SykmeldingMetadata
    override val pasient: Pasient
    override val medisinskVurdering: MedisinskVurdering
    override val aktivitet: List<Aktivitet>
    val behandler: Behandler
    val arbeidsgiver: ArbeidsgiverInfo
    val sykmelder: Sykmelder
    val prognose: Prognose?
    val tiltak: Tiltak?
    val bistandNav: BistandNav?
    val tilbakedatering: Tilbakedatering?
    val utdypendeOpplysninger: Map<String, Map<String, SporsmalSvar>>?
}

data class XMLSykmeldingGrunnlag(
    override val id: String,
    override val metadata: SykmeldingMetadata,
    override val pasient: Pasient,
    override val medisinskVurdering: IkkeDigitalMedisinskVurdering,
    override val aktivitet: List<Aktivitet>,
    override val behandler: Behandler,
    override val arbeidsgiver: ArbeidsgiverInfo,
    override val sykmelder: Sykmelder,
    override val prognose: Prognose?,
    override val tiltak: Tiltak?,
    override val bistandNav: BistandNav?,
    override val tilbakedatering: Tilbakedatering?,
    override val utdypendeOpplysninger: Map<String, Map<String, SporsmalSvar>>?,
) : NorskSykmeldingGrunnlag {
    override val type = SykmeldingType.XML
}

data class DigitalSykmeldingGrunnlag(
    override val id: String,
    override val metadata: SykmeldingMetadata,
    override val pasient: Pasient,
    override val medisinskVurdering: DigitalMedisinskVurdering,
    override val aktivitet: List<Aktivitet>,
    override val behandler: Behandler,
    override val arbeidsgiver: ArbeidsgiverInfo,
    override val sykmelder: Sykmelder,
    override val bistandNav: BistandNav?,
    override val tilbakedatering: Tilbakedatering?,
    val utdypendeSporsmal: List<UtdypendeSporsmal>? = null,
) : NorskSykmeldingGrunnlag {
    override val prognose: Prognose? = null
    override val tiltak: Tiltak? = null
    override val type = SykmeldingType.DIGITAL

    @get:JsonIgnore
    override val utdypendeOpplysninger: Map<String, Map<String, SporsmalSvar>>?
        get() = toUtdypendeOpplysninger(utdypendeSporsmal)
}

data class PapirSykmeldingGrunnlag(
    override val id: String,
    override val metadata: SykmeldingMetadata,
    override val pasient: Pasient,
    override val medisinskVurdering: IkkeDigitalMedisinskVurdering,
    override val aktivitet: List<Aktivitet>,
    override val behandler: Behandler,
    override val arbeidsgiver: ArbeidsgiverInfo,
    override val sykmelder: Sykmelder,
    override val prognose: Prognose?,
    override val tiltak: Tiltak?,
    override val bistandNav: BistandNav?,
    override val tilbakedatering: Tilbakedatering?,
    override val utdypendeOpplysninger: Map<String, Map<String, SporsmalSvar>>?,
) : NorskSykmeldingGrunnlag {
    override val type = SykmeldingType.PAPIR
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
    val avsenderSystem: AvsenderSystem,
    val behandletTidspunkt: OffsetDateTime?,
    val regelsettVersjon: String?,
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

enum class Sporsmalstype {
    UTFORDRINGER_MED_GRADERT_ARBEID,
    MEDISINSK_OPPSUMMERING,
    HENSYN_PA_ARBEIDSPLASSEN,
}

data class UtdypendeSporsmal(
    val svar: String,
    val type: Sporsmalstype,
    val skjermetForArbeidsgiver: Boolean = true,
)

const val UKE_7_PREFIX = "6.3"

fun sporsmalUke7Mapping(sporsmalType: Sporsmalstype): Pair<String, String> =
    when (sporsmalType) {
        Sporsmalstype.MEDISINSK_OPPSUMMERING -> {
            Pair(
                "$UKE_7_PREFIX.1",
                "Gi en kort medisinsk oppsummering av tilstanden (sykehistorie, hovedsymptomer, pågående/planlagt behandling)",
            )
        }

        Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID -> {
            Pair(
                "$UKE_7_PREFIX.2",
                "Hvilke utfordringer har pasienten med å utføre gradert arbeid?",
            )
        }

        Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN -> {
            Pair(
                "$UKE_7_PREFIX.3",
                "Hvilke hensyn må være på plass for at pasienten kan prøves i det nåværende arbeidet? (ikke obligatorisk)",
            )
        }
    }

fun toUtdypendeOpplysninger(sporsmal: List<UtdypendeSporsmal>?): Map<String, Map<String, SporsmalSvar>>? {
    if (sporsmal == null) {
        return null
    }

    val uke7 =
        sporsmal
            .asSequence()
            .map { sporsmal ->
                val (key, sporsmalTekst) = sporsmalUke7Mapping(sporsmal.type)
                key to
                    SporsmalSvar(
                        sporsmal = sporsmalTekst,
                        restriksjoner = listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                        svar = sporsmal.svar,
                    )
            }.toMap()

    return mapOf(UKE_7_PREFIX to uke7)
}
