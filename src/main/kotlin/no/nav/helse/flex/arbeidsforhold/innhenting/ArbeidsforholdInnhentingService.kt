package no.nav.helse.flex.arbeidsforhold.innhenting

import no.nav.helse.flex.arbeidsforhold.Arbeidsforhold
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.logger
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC

data class SynkroniserteArbeidsforhold(
    val skalOpprettes: List<Arbeidsforhold> = emptyList(),
    val skalOppdateres: List<Arbeidsforhold> = emptyList(),
    val skalSlettes: List<Arbeidsforhold> = emptyList(),
)

class ArbeidsforholdInnhentingService(
    private val eksternArbeidsforholdHenter: EksternArbeidsforholdHenter,
    private val arbeidsforholdRepository: ArbeidsforholdRepository,
    private val nowFactory: () -> Instant = Instant::now,
) {
    val log = logger()

    fun synkroniserArbeidsforhold(fnr: String): SynkroniserteArbeidsforhold {
        val eksterntArbeidsforhold = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson(fnr)
        val interneArbeidsforhold = arbeidsforholdRepository.getAllByFnr(fnr)
        val synkroniserteArbeidsforhold = synkroniserArbeidsforholdBusiness(interneArbeidsforhold, eksterntArbeidsforhold)
        lagreSynkroniserteArbeidsforhold(synkroniserteArbeidsforhold)
        return synkroniserteArbeidsforhold
    }

    internal fun synkroniserArbeidsforholdBusiness(
        interneArbeidsforhold: List<Arbeidsforhold>,
        eksterneArbeidsforhold: List<EksterntArbeidsforhold>,
    ): SynkroniserteArbeidsforhold {
        val eksterneArbeidsforholdVedId = eksterneArbeidsforhold.associateBy { it.arbeidsforholdId }
        val interneArbeidsforholdVedId = interneArbeidsforhold.associateBy { it.arbeidsforholdId }

        val opprettArbeidsforholdId = eksterneArbeidsforholdVedId.keys - interneArbeidsforholdVedId.keys
        val oppdaterArbeidsforholdId = interneArbeidsforholdVedId.keys.intersect(eksterneArbeidsforholdVedId.keys)
        val slettArbeidsforholdId = interneArbeidsforholdVedId.keys - eksterneArbeidsforholdVedId.keys

        val opprettArbeidsforhold = opprettArbeidsforholdId.map { arbeidsforholdId ->
            val eksterntArbeidsforhold = eksterneArbeidsforholdVedId[arbeidsforholdId]!!
            Arbeidsforhold(
                arbeidsforholdId = eksterntArbeidsforhold.arbeidsforholdId,
                fnr = eksterntArbeidsforhold.fnr,
                orgnummer = eksterntArbeidsforhold.orgnummer,
                juridiskOrgnummer = eksterntArbeidsforhold.juridiskOrgnummer,
                orgnavn = eksterntArbeidsforhold.orgnavn,
                fom = eksterntArbeidsforhold.fom,
                tom = eksterntArbeidsforhold.tom,
                arbeidsforholdType = eksterntArbeidsforhold.arbeidsforholdType,
                opprettet = nowFactory(),
            )
        }

        val oppdaterteArbeidsforhold = oppdaterArbeidsforholdId.map { arbeidsforholdId ->
            val interntArbeidsforhold = interneArbeidsforholdVedId[arbeidsforholdId]!!
            val eksterntArbeidsforhold = eksterneArbeidsforholdVedId[arbeidsforholdId]!!
            interntArbeidsforhold.copy(
                arbeidsforholdId = eksterntArbeidsforhold.arbeidsforholdId,
                fnr = eksterntArbeidsforhold.fnr,
                orgnummer = eksterntArbeidsforhold.orgnummer,
                juridiskOrgnummer = eksterntArbeidsforhold.juridiskOrgnummer,
                orgnavn = eksterntArbeidsforhold.orgnavn,
                fom = eksterntArbeidsforhold.fom,
                tom = eksterntArbeidsforhold.tom,
                arbeidsforholdType = eksterntArbeidsforhold.arbeidsforholdType,
            )
        }

        val slettedeArbeidsforhold = slettArbeidsforholdId.map { arbeidsforholdId ->
            interneArbeidsforholdVedId[arbeidsforholdId]!!
        }

        val opprettArbeidsforholdNyeNok = opprettArbeidsforhold.filter { harVaertAnsattSiste4Mnd(it.tom) }

        return SynkroniserteArbeidsforhold(
            skalOpprettes = opprettArbeidsforholdNyeNok,
            skalOppdateres = oppdaterteArbeidsforhold,
            skalSlettes = slettedeArbeidsforhold,
        )
    }

    internal fun lagreSynkroniserteArbeidsforhold(synkroniserteArbeidsforhold: SynkroniserteArbeidsforhold) {
        if (synkroniserteArbeidsforhold.skalOpprettes.isNotEmpty()) {
            arbeidsforholdRepository.saveAll(synkroniserteArbeidsforhold.skalOpprettes)
        }
        if (synkroniserteArbeidsforhold.skalOppdateres.isNotEmpty()) {
            arbeidsforholdRepository.saveAll(synkroniserteArbeidsforhold.skalOppdateres)
        }
        if (synkroniserteArbeidsforhold.skalSlettes.isNotEmpty()) {
            arbeidsforholdRepository.deleteAll(synkroniserteArbeidsforhold.skalSlettes)
        }
    }

    // TODO sjekk logikk for dette
    private fun harVaertAnsattSiste4Mnd(sluttDato: LocalDate?): Boolean {
        val ansettelsesperiodeFom = LocalDate.ofInstant(nowFactory(), UTC).minusMonths(4)
        return sluttDato == null || sluttDato.isAfter(ansettelsesperiodeFom)
    }

//    fun updateArbeidsforhold(fnr: String) {
//        val arbeidsforhold = getAlleArbeidsforhold(fnr)
//        val arbeidsforholdFraDb = arbeidsforholdRepository.getByFnr(fnr)
//
//        val slettesfraDb =
//            getArbeidsforholdSomSkalSlettes(
//                arbeidsforholdRepository = arbeidsforholdFraDb,
//                arbeidsforholdAareg = arbeidsforhold,
//            )
//
//        if (slettesfraDb.isNotEmpty()) {
//            slettesfraDb.forEach {
//                log.info(
//                    "Sletter utdatert arbeidsforhold med id $it",
//                )
//                arbeidsforholdRepository.delete(it)
//            }
//        }
//        arbeidsforhold.forEach { insertOrUpdate(it) }
//    }
//
//    private fun getAlleArbeidsforhold(fnr: String): List<Arbeidsforhold> {
//        val arbeidsgivere = aaregClient.hentArbeidsforholdoversikt(fnr = fnr).arbeidsforholdoversikter
//
//        if (arbeidsgivere.isEmpty()) {
//            return emptyList()
//        }
//
//        val arbeidsgiverList =
//            arbeidsgivere
//                .filter { it.arbeidssted.type == ArbeidsstedType.Underenhet }
//                .filter { arbeidsforholdErGyldig(it.ansettelsesperiode) }
//                .sortedWith(
//                    compareByDescending(nullsLast()) { it.ansettelsesperiode.sluttdato },
//                )
//                .map { aaregArbeidsforhold ->
//                    val organisasjonsinfo =
//                        eregClient.getOrganisasjonsnavn(
//                            aaregArbeidsforhold.arbeidssted.getOrgnummer()
//                        )
//                    val arbeidsforholdType = ArbeidsforholdType.parse(aaregArbeidsforhold.type.kode)
//                    Arbeidsforhold(
//                        id = aaregArbeidsforhold.navArbeidsforholdId,
//                        fnr = fnr,
//                        orgnummer = aaregArbeidsforhold.arbeidssted.getOrgnummer(),
//                        juridiskOrgnummer =
//                        aaregArbeidsforhold.opplysningspliktig.getJuridiskOrgnummer(),
//                        orgNavn = organisasjonsinfo.navn.getNameAsString(),
//                        fom = aaregArbeidsforhold.ansettelsesperiode.startdato,
//                        tom = aaregArbeidsforhold.ansettelsesperiode.sluttdato,
//                        type = arbeidsforholdType,
//                    )
//                }
//        return arbeidsgiverList
//    }
//
//
//    private fun arbeidsforholdErGyldig(ansettelsesperiode: Ansettelsesperiode): Boolean {
//        val ansettelsesperiodeFom = LocalDate.now().minusMonths(4)
//        return ansettelsesperiode.sluttdato == null ||
//            ansettelsesperiode.sluttdato.isAfter(ansettelsesperiodeFom)
//    }
}
