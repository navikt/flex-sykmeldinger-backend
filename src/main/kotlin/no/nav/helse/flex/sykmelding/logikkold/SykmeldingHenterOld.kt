
package no.nav.helse.flex.sykmelding.logikk

import no.nav.helse.flex.sykmelding.domain.*
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.OffsetDateTime

@Component
class SykmeldingHenter {
    fun getSykmeldinger(fnr: String): List<SykmeldingMedBehandlingsutfall> {
        return listOf(
            SykmeldingMedBehandlingsutfall(
                sykmelding = nySykmelding(),
                behandlingsutfall = Behandlingsutfall(status = "OK"),
            ),
            SykmeldingMedBehandlingsutfall(
                sykmelding = sendtSykmelding(),
                behandlingsutfall = Behandlingsutfall(status = "OK"),
            ),
        )
    }

    fun getSykmelding(
        fnr: String,
        sykmeldingId: String,
    ): SykmeldingMedBehandlingsutfall? {
        return when (sykmeldingId) {
            "APEN" ->
                SykmeldingMedBehandlingsutfall(
                    sykmelding = nySykmelding(),
                    behandlingsutfall = Behandlingsutfall(status = "OK"),
                )
            "SENDT" ->
                SykmeldingMedBehandlingsutfall(
                    sykmelding = sendtSykmelding(),
                    behandlingsutfall = Behandlingsutfall(status = "OK"),
                )
            else -> null
        }
    }

    fun finnTidligereArbeidsgivere(
        fnr: String,
        sykmeldingUuid: String,
    ): List<TidligereArbeidsgiverDTO> {
        return listOf(
            TidligereArbeidsgiverDTO(
                orgnummer = "972674818",
                orgNavn = "Hogwarts School of Witchcraft and Wizardry",
                fom = LocalDate.now().minusMonths(12),
                tom = LocalDate.now(),
            ),
        )
    }

    private fun nySykmelding(): Sykmelding {
        return Sykmelding(
            id = "APEN",
            metadata =
                SykmeldingMetadata(
                    mottattDato = OffsetDateTime.now(),
                    genDate = OffsetDateTime.now(),
                    behandletTidspunkt = OffsetDateTime.now(),
                    regelsettVersjon = "1.0",
                    avsenderSystem = AvsenderSystem("EPJ", "1.0"),
                    strekkode = "123",
                ),
            pasient =
                Pasient(
                    fnr = "12345678910",
                    navn = Navn("Test", null, "Testesen"),
                    kontaktinfo = emptyList(),
                    navKontor = null,
                    navnFastlege = null,
                ),
            medisinskVurdering =
                MedisinskVurdering(
                    hovedDiagnose = DiagnoseInfo(system = DiagnoseSystem.ICPC2, kode = "R51"),
                    biDiagnoser = emptyList(),
                    svangerskap = false,
                    yrkesskade = null,
                    skjermetForPasient = false,
                    syketilfelletStartDato = LocalDate.now(),
                    annenFraversArsak = null,
                ),
            aktivitet =
                listOf(
                    AktivitetIkkeMulig(
                        medisinskArsak =
                            MedisinskArsak(
                                beskrivelse = "Pasient må avlastes",
                                arsak = MedisinskArsakType.TILSTAND_HINDRER_AKTIVITET,
                            ),
                        arbeidsrelatertArsak = null,
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(7),
                    ),
                ),
            behandler = mockBehandler(),
            arbeidsgiver =
                EnArbeidsgiver(
                    meldingTilArbeidsgiver = null,
                    tiltakArbeidsplassen = null,
                ),
            signerendeBehandler =
                SignerendeBehandler(
                    ids = listOf(PersonId("123", PersonIdType.HPR)),
                    helsepersonellKategori = HelsepersonellKategori.LEGE,
                ),
            prognose = null,
            tiltak = null,
            bistandNav = null,
            tilbakedatering = null,
            utdypendeOpplysninger = null,
        )
    }

    private fun sendtSykmelding(): Sykmelding {
        return nySykmelding().copy(
            id = "SENDT",
            arbeidsgiver =
                EnArbeidsgiver(
                    meldingTilArbeidsgiver = "Tilrettelegging nødvendig",
                    tiltakArbeidsplassen = "Redusert arbeidstid",
                ),
        )
    }

    private fun mockBehandler() =
        Behandler(
            navn = Navn("Lege", null, "Legesen"),
            kontaktinfo = emptyList(),
            adresse = null,
            ids = listOf(PersonId("123", PersonIdType.HPR)),
        )
}
