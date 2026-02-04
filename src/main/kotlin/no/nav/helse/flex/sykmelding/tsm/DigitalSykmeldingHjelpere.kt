package no.nav.helse.flex.sykmelding.tsm

object DigitalSykmeldingHjelpere {
    fun toUtdypendeOpplysninger(sporsmal: List<UtdypendeSporsmal>?): Map<String, Map<String, SporsmalSvar>> {
        if (sporsmal.isNullOrEmpty()) {
            return emptyMap()
        }

        val prioritertHovedgruppe = finnPrioritertUtdypendeOpplysningHovedgruppe(sporsmal)

        val strukturertUtdypendeOpplysninger =
            sporsmal.map {
                konverterTilStrukturertUtdypendeOpplysning(sporsmal = it, prioritertHovedgruppe = prioritertHovedgruppe)
            }

        return strukturertUtdypendeOpplysninger
            .groupBy { it.sporsmalgruppeNotasjon }
            .mapValues { (_, sporsmals) ->
                sporsmals.associate {
                    it.sporsmalNotasjon to
                        SporsmalSvar(
                            sporsmal = it.sporsmal,
                            svar = it.svar,
                            restriksjoner = listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                        )
                }
            }
    }
}

private enum class UtdypendeOpplysningHovedgruppe(
    val notasjon: String,
) {
    UKE_7("6.3"),
    UKE_17("6.4"),
    UKE_39("6.5"),
}

private data class StrukturertUtdypendeOpplysning(
    val hovedgruppe: UtdypendeOpplysningHovedgruppe,
    val undergruppe: String,
    val sporsmal: String,
    val svar: String,
) {
    val sporsmalgruppeNotasjon
        get() = hovedgruppe.notasjon
    val sporsmalNotasjon
        get() = "$sporsmalgruppeNotasjon.$undergruppe"

    init {
        val tillattUndergruppe = setOf("1", "2", "3", "4")
        require(undergruppe in tillattUndergruppe) {
            "Undergruppe må være en av $tillattUndergruppe"
        }
    }
}

private fun finnPrioritertUtdypendeOpplysningHovedgruppe(sporsmal: List<UtdypendeSporsmal>): UtdypendeOpplysningHovedgruppe =
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
