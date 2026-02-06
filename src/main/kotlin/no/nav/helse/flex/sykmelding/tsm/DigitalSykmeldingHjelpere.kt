package no.nav.helse.flex.sykmelding.tsm

object DigitalSykmeldingHjelpere {
    fun tilBakoverkompatibelUtdypendeOpplysninger(utdypendeSporsmal: List<UtdypendeSporsmal>?): Map<String, Map<String, SporsmalSvar>> {
        if (utdypendeSporsmal.isNullOrEmpty()) {
            return emptyMap()
        }

        val prioritertSporsmalgruppe = finnPrioritertSporsmalgruppe(utdypendeSporsmal)

        val strukturertUtdypendeOpplysninger =
            utdypendeSporsmal.map {
                konverterTilStrukturertUtdypendeOpplysning(sporsmal = it, prioritertSporsmalgruppe = prioritertSporsmalgruppe)
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

private enum class Sporsmalgruppe(
    val notasjon: String,
) {
    UKE_7("6.3"),
    UKE_17("6.4"),
    UKE_39("6.5"),
}

private data class StrukturertUtdypendeOpplysning(
    val sporsmalgruppe: Sporsmalgruppe,
    val sporsmalnummer: Int,
    val sporsmal: String,
    val svar: String,
) {
    val sporsmalgruppeNotasjon
        get() = sporsmalgruppe.notasjon
    val sporsmalNotasjon
        get() = "$sporsmalgruppeNotasjon.$sporsmalnummer"

    init {
        val tillattSporsmalnummer = (0 until 10)
        require(sporsmalnummer in tillattSporsmalnummer) {
            "Spørsmålnummer må være i $tillattSporsmalnummer"
        }
    }
}

private fun finnPrioritertSporsmalgruppe(sporsmal: List<UtdypendeSporsmal>): Sporsmalgruppe =
    when {
        sporsmal.any { it.type == Sporsmalstype.MEDISINSKE_HENSYN } -> Sporsmalgruppe.UKE_39
        sporsmal.any { it.type == Sporsmalstype.BEHANDLING_OG_FREMTIDIG_ARBEID } -> Sporsmalgruppe.UKE_17
        sporsmal.any { it.type == Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID } -> Sporsmalgruppe.UKE_7
        else -> throw IllegalArgumentException(
            "Liste med utdypende sporsmal mangler nødvendig type for konvertering. Eksisterende typer: ${sporsmal.map { it.type }}",
        )
    }

private fun konverterTilStrukturertUtdypendeOpplysning(
    sporsmal: UtdypendeSporsmal,
    prioritertSporsmalgruppe: Sporsmalgruppe,
): StrukturertUtdypendeOpplysning =
    when (sporsmal.type) {
        Sporsmalstype.MEDISINSK_OPPSUMMERING ->
            StrukturertUtdypendeOpplysning(
                sporsmalgruppe = prioritertSporsmalgruppe,
                sporsmalnummer = 1,
                sporsmal =
                    sporsmal.sporsmal
                        ?: "Gi en kort medisinsk oppsummering av tilstanden (sykehistorie, hovedsymptomer, behandling)",
                svar = sporsmal.svar,
            )
        Sporsmalstype.UTFORDRINGER_MED_ARBEID ->
            StrukturertUtdypendeOpplysning(
                sporsmalgruppe = prioritertSporsmalgruppe,
                sporsmalnummer = 2,
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
                sporsmalgruppe = Sporsmalgruppe.UKE_7,
                sporsmalnummer = 2,
                sporsmal = sporsmal.sporsmal ?: "Beskriv kort hvilke helsemessige begrensninger som gjør det vanskelig å jobbe gradert",
                svar = sporsmal.svar,
            )
        Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN ->
            StrukturertUtdypendeOpplysning(
                sporsmalgruppe = Sporsmalgruppe.UKE_7,
                sporsmalnummer = 3,
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
                sporsmalgruppe = Sporsmalgruppe.UKE_17,
                sporsmalnummer = 3,
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
                sporsmalgruppe = Sporsmalgruppe.UKE_17,
                sporsmalnummer = 4,
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
                sporsmalgruppe = Sporsmalgruppe.UKE_39,
                sporsmalnummer = 3,
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
                sporsmalgruppe = Sporsmalgruppe.UKE_39,
                sporsmalnummer = 4,
                sporsmal = sporsmal.sporsmal ?: "Er det medisinske hensyn eller avklaringsbehov Nav bør kjenne til i videre oppfølging?",
                svar = sporsmal.svar,
            )
    }
