package no.nav.helse.flex.sykmelding.logikk

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.sykmelding.domain.ArbeidsgiverType
import no.nav.helse.flex.sykmelding.domain.EnArbeidsgiver
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykmeldingHenterTest : FellesTestOppsett() {
    @Test
    fun `henter liste med sykmeldinger`() {
        val sykmeldingHenter = SykmeldingHenter()
        val sykmeldinger = sykmeldingHenter.getSykmeldinger("12345678910")

        sykmeldinger shouldHaveSize 2

        val forste = sykmeldinger.first()
        with(forste) {
            behandlingsutfall.status `should be equal to` "OK"
            sykmelding.id `should be equal to` "APEN"
            sykmelding.pasient.fnr `should be equal to` "12345678910"
            sykmelding.arbeidsgiver.type `should be equal to` ArbeidsgiverType.EN_ARBEIDSGIVER
        }
    }

    @Test
    fun `henter spesifikk sykmelding ved id`() {
        val sykmeldingHenter = SykmeldingHenter()
        val sykmeldingMedBehandlingsutfall = sykmeldingHenter.getSykmelding(fnr = "12345678910", sykmeldingId = "SENDT")

        sykmeldingMedBehandlingsutfall.shouldNotBeNull()
        with(sykmeldingMedBehandlingsutfall) {
            behandlingsutfall.status `should be equal to` "OK"
            sykmelding.id `should be equal to` "SENDT"
            val arbeidsgiver = sykmelding.arbeidsgiver as EnArbeidsgiver
            arbeidsgiver.tiltakArbeidsplassen `should be equal to` "Redusert arbeidstid"
        }
    }

    @Test
    fun `returnerer null når sykmelding ikke finnes`() {
        val sykmeldingHenter = SykmeldingHenter()
        val sykmelding = sykmeldingHenter.getSykmelding(fnr = "12345678910", sykmeldingId = "FINNES_IKKE")

        sykmelding `should be equal to` null
    }

    @Test
    fun `henter tidligere arbeidsgivere`() {
        val sykmeldingHenter = SykmeldingHenter()
        val arbeidsgivere = sykmeldingHenter.finnTidligereArbeidsgivere("12345678910", "any-uuid")

        arbeidsgivere shouldHaveSize 1

        val arbeidsgiver = arbeidsgivere.first()
        with(arbeidsgiver) {
            orgnummer `should be equal to` "972674818"
            orgNavn `should be equal to` "Hogwarts School of Witchcraft and Wizardry"
            fom `should be equal to` LocalDate.now().minusMonths(12)
            tom `should be equal to` LocalDate.now()
        }
    }
}
