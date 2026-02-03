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
    UTFORDRINGER_MED_ARBEID,
    MEDISINSK_OPPSUMMERING,
    HENSYN_PA_ARBEIDSPLASSEN,
    BEHANDLING_OG_FREMTIDIG_ARBEID,
    UAVKLARTE_FORHOLD,
    FORVENTET_HELSETILSTAND_UTVIKLING,
    MEDISINSKE_HENSYN,
}

data class UtdypendeSporsmal(
    val svar: String,
    val type: Sporsmalstype,
    val skjermetForArbeidsgiver: Boolean = true,
    val sporsmal: String?,
)

enum class UtdypendeOpplysningHovedgruppe(
    val notasjon: String,
) {
    UKE_7("6.3"),
    UKE_17("6.4"),
    UKE_39("6.5"),
}

fun toUtdypendeOpplysninger(sporsmal: List<UtdypendeSporsmal>?): Map<String, Map<String, SporsmalSvar>> {
    if (sporsmal.isNullOrEmpty()) {
        return emptyMap()
    }

    val prioritertHovedgruppe = finnPrioritertUtdypendeOpplysningHovedgruppe(sporsmal)

    val strukturertUtdypendeOpplysninger =
        sporsmal.map {
            konverterTilStrukturertUtdypendeOpplysning(sporsmal = it, prioritertHovedgruppe = prioritertHovedgruppe)
        }

    val grouped: Map<UtdypendeOpplysningHovedgruppe, List<StrukturertUtdypendeOpplysning>> =
        strukturertUtdypendeOpplysninger.groupBy { it.hovedgruppe }

    return grouped
        .map { (hovedgruppe, utdypendeOpplysninger) ->
            val hovedgruppeNotasjon = hovedgruppe.notasjon
            val konverterteSporsmal =
                utdypendeOpplysninger.associate { opplysning ->
                    "$hovedgruppeNotasjon.${opplysning.undergruppe}" to
                        SporsmalSvar(
                            sporsmal = opplysning.sporsmal,
                            svar = opplysning.svar,
                            restriksjoner = listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                        )
                }
            hovedgruppeNotasjon to konverterteSporsmal
        }.toMap()
}

data class StrukturertUtdypendeOpplysning(
    val hovedgruppe: UtdypendeOpplysningHovedgruppe,
    val undergruppe: String,
    val sporsmal: String,
    val svar: String,
) {
    init {
        val tillattUndergruppe = setOf("1", "2", "3", "4")
        require(undergruppe in tillattUndergruppe) {
            "Undergruppe må være en av $tillattUndergruppe"
        }
    }
}

fun finnPrioritertUtdypendeOpplysningHovedgruppe(sporsmal: List<UtdypendeSporsmal>): UtdypendeOpplysningHovedgruppe =
    when {
        sporsmal.any { it.type == Sporsmalstype.MEDISINSKE_HENSYN } -> UtdypendeOpplysningHovedgruppe.UKE_39
        sporsmal.any { it.type == Sporsmalstype.BEHANDLING_OG_FREMTIDIG_ARBEID } -> UtdypendeOpplysningHovedgruppe.UKE_17
        sporsmal.any { it.type == Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID } -> UtdypendeOpplysningHovedgruppe.UKE_7
        else -> throw IllegalArgumentException(
            "Liste med utdypende sporsmal mangler nødvendig type for konvertering. Eksisterende typer: ${sporsmal.map { it.type }}",
        )
    }

private fun konverterTilStrukturertUtdypendeOpplysning(
    sporsmal: UtdypendeSporsmal,
    prioritertHovedgruppe: UtdypendeOpplysningHovedgruppe,
): StrukturertUtdypendeOpplysning =
    when (sporsmal.type) {
        Sporsmalstype.MEDISINSK_OPPSUMMERING ->
            StrukturertUtdypendeOpplysning(
                hovedgruppe = prioritertHovedgruppe,
                undergruppe = "1",
                sporsmal =
                    sporsmal.sporsmal
                        ?: "Gi en kort medisinsk oppsummering av tilstanden (sykehistorie, hovedsymptomer, behandling)",
                svar = sporsmal.svar,
            )
        Sporsmalstype.UTFORDRINGER_MED_ARBEID ->
            StrukturertUtdypendeOpplysning(
                hovedgruppe = prioritertHovedgruppe,
                undergruppe = "2",
                sporsmal =
                    sporsmal.sporsmal
                        ?: (
                            "Beskriv kort hvilke utfordringer helsetilstanden gir i arbeidssituasjonen nå. " +
                                "Oppgi også kort hva pasienten likevel kan mestre"
                        ),
                svar = sporsmal.svar,
            )
        Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID ->
            StrukturertUtdypendeOpplysning(
                hovedgruppe = UtdypendeOpplysningHovedgruppe.UKE_7,
                undergruppe = "2",
                sporsmal = sporsmal.sporsmal ?: "Beskriv kort hvilke helsemessige begrensninger som gjør det vanskelig å jobbe gradert",
                svar = sporsmal.svar,
            )
        Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN ->
            StrukturertUtdypendeOpplysning(
                hovedgruppe = UtdypendeOpplysningHovedgruppe.UKE_7,
                undergruppe = "3",
                sporsmal =
                    sporsmal.sporsmal
                        ?: (
                            "Beskriv eventuelle medisinske forhold som bør ivaretas " +
                                "ved eventuell tilbakeføring til nåværende arbeid (ikke obligatorisk)"
                        ),
                svar = sporsmal.svar,
            )
        Sporsmalstype.BEHANDLING_OG_FREMTIDIG_ARBEID ->
            StrukturertUtdypendeOpplysning(
                hovedgruppe = UtdypendeOpplysningHovedgruppe.UKE_17,
                undergruppe = "3",
                sporsmal =
                    sporsmal.sporsmal
                        ?: (
                            "Beskriv pågående og planlagt utredning/behandling, " +
                                "og om dette forventes å påvirke muligheten for økt arbeidsdeltakelse fremover"
                        ),
                svar = sporsmal.svar,
            )
        Sporsmalstype.UAVKLARTE_FORHOLD ->
            StrukturertUtdypendeOpplysning(
                hovedgruppe = UtdypendeOpplysningHovedgruppe.UKE_17,
                undergruppe = "4",
                sporsmal =
                    sporsmal.sporsmal
                        ?: (
                            "Er det forhold som fortsatt er uavklarte eller hindrer videre arbeidsdeltakelse, " +
                                "som Nav bør være kjent med i sin oppfølging?"
                        ),
                svar = sporsmal.svar,
            )
        Sporsmalstype.FORVENTET_HELSETILSTAND_UTVIKLING ->
            StrukturertUtdypendeOpplysning(
                hovedgruppe = UtdypendeOpplysningHovedgruppe.UKE_39,
                undergruppe = "3",
                sporsmal =
                    sporsmal.sporsmal
                        ?: (
                            "Hvordan forventes helsetilstanden å utvikle seg de neste 3-6 månedene " +
                                "med tanke på mulighet for økt arbeidsdeltakelse?"
                        ),
                svar = sporsmal.svar,
            )
        Sporsmalstype.MEDISINSKE_HENSYN ->
            StrukturertUtdypendeOpplysning(
                hovedgruppe = UtdypendeOpplysningHovedgruppe.UKE_39,
                undergruppe = "4",
                sporsmal = "Er det medisinske hensyn eller avklaringsbehov Nav bør kjenne til i videre oppfølging?",
                svar = sporsmal.svar,
            )
    }
