package no.nav.helse.flex.arbeidsforhold.innhenting

import no.nav.helse.flex.arbeidsforhold.Arbeidsforhold
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.logger
import java.time.Instant

class ArebeidsforholdInnhentingService(
    private val eksternArbeidsforholdHenter: EksternArbeidsforholdHenter,
    private val arbeidsforholdRepository: ArbeidsforholdRepository,
    private val nowFactory: () -> Instant = Instant::now,
) {
    val log = logger()

    fun synkroniserArbeidsforhold(arbeidsforholdId: String) {
        val eksterntArbeidsforhold = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson(arbeidsforholdId).first()
        val interntArbeidsforhold = arbeidsforholdRepository.findByArbeidsforholdId(arbeidsforholdId)
        if (interntArbeidsforhold == null) {
            arbeidsforholdRepository.save(
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
                ),
            )
        } else {
            val oppdatertArbeidsforhold =
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
            arbeidsforholdRepository.save(
                oppdatertArbeidsforhold,
            )
        }
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
