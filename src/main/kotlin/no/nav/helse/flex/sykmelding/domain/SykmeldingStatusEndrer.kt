package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.sykmelding.UgyldigSykmeldingStatusException
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.function.Supplier

@Service
class SykmeldingStatusEndrer(
    private val nowFactory: Supplier<Instant>,
    private val arbeidsforholdRepository: ArbeidsforholdRepository,
) {
    private val logger = logger()

    fun endreStatusTilSendtTilArbeidsgiver(
        sykmelding: Sykmelding,
        identer: PersonIdenter,
        arbeidsgiverOrgnummer: String? = null,
        sporsmalSvar: List<Sporsmal>? = null,
    ): Sykmelding {
        val sisteStatus = sykmelding.sisteStatus()
        if (
            sisteStatus.status !in
            setOf(
                HendelseStatus.APEN,
                HendelseStatus.SENDT_TIL_NAV,
                HendelseStatus.AVBRUTT,
            )
        ) {
            throw UgyldigSykmeldingStatusException(
                "Kan ikke endre status til ${HendelseStatus.SENDT_TIL_ARBEIDSGIVER} fra ${sisteStatus.status}",
            )
        }

        val arbeidstakerInfo: ArbeidstakerInfo? =
            if (arbeidsgiverOrgnummer != null) {
                val arbeidsforhold = arbeidsforholdRepository.getAllByFnrIn(identer.alle())
                val valgtArbeidsforhold = arbeidsforhold.find { it.orgnummer == arbeidsgiverOrgnummer }
                if (valgtArbeidsforhold == null) {
                    throw IllegalArgumentException("Fant ikke arbeidsgiver med orgnummer $arbeidsgiverOrgnummer")
                }
                ArbeidstakerInfo(
                    arbeidsgiver =
                        Arbeidsgiver(
                            orgnummer = valgtArbeidsforhold.orgnummer,
                            juridiskOrgnummer = valgtArbeidsforhold.juridiskOrgnummer,
                            orgnavn = valgtArbeidsforhold.orgnavn,
                        ),
                )
            } else {
                null
            }

        val hendelse =
            SykmeldingHendelse(
                status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                opprettet = nowFactory.get(),
                sporsmalSvar = sporsmalSvar,
                arbeidstakerInfo = arbeidstakerInfo,
            )

        return sykmelding.leggTilStatus(hendelse)
    }

    fun endreStatusTilSendtTilNav(
        sykmelding: Sykmelding,
        identer: PersonIdenter,
        arbeidsledigFraOrgnummer: String? = null,
        sporsmalSvar: List<Sporsmal>? = null,
    ): Sykmelding {
        val sisteStatus = sykmelding.sisteStatus()
        require(
            sisteStatus.status in
                setOf(
                    HendelseStatus.APEN,
                    HendelseStatus.SENDT_TIL_NAV,
                    HendelseStatus.AVBRUTT,
                ),
        )

        // TODO: Hent tidligere arbeidsgivere
        val hendelse =
            SykmeldingHendelse(
                status = HendelseStatus.SENDT_TIL_NAV,
                opprettet = nowFactory.get(),
                // tidligereArbeidsgiver = tidligereArbeidsgiver,
            )

        return sykmelding.leggTilStatus(hendelse)
    }

    fun endreStatusTilBekreftetAvvist(
        sykmelding: Sykmelding,
        identer: PersonIdenter,
    ): Sykmelding {
        val sisteStatus = sykmelding.sisteStatus()
        require(
            sisteStatus.status in
                setOf(
                    HendelseStatus.APEN,
                ),
        )
        val hendelse =
            SykmeldingHendelse(
                status = HendelseStatus.BEKREFTET_AVVIST,
                opprettet = nowFactory.get(),
            )

        return sykmelding.leggTilStatus(hendelse)
    }

    fun endreStatusTilAvbrutt(sykmelding: Sykmelding): Sykmelding {
        val sisteHendelse = sykmelding.sisteStatus()
        require(
            sisteHendelse.status in
                setOf(HendelseStatus.APEN, HendelseStatus.SENDT_TIL_NAV, HendelseStatus.AVBRUTT),
        )
        // TODO: Burde vi kaste exception om vi endrer AVBRUTT til AVBRUTT?
        if (sisteHendelse.status == HendelseStatus.AVBRUTT) {
            return sykmelding
        }

        val hendelse =
            SykmeldingHendelse(
                status = HendelseStatus.AVBRUTT,
                opprettet = nowFactory.get(),
            )

        return sykmelding.leggTilStatus(hendelse)
    }

    fun endreStatusTilApen(sykmelding: Sykmelding): Sykmelding {
        val sisteHendelse = sykmelding.sisteStatus()
        require(
            sisteHendelse.status in
                setOf(HendelseStatus.APEN, HendelseStatus.SENDT_TIL_NAV, HendelseStatus.AVBRUTT),
        )
        if (sisteHendelse.status == HendelseStatus.APEN) {
            return sykmelding
        }
        sykmelding.leggTilStatus(
            SykmeldingHendelse(
                status = HendelseStatus.APEN,
                opprettet = nowFactory.get(),
            ),
        )
        return sykmelding
    }

    fun endreStatusTilUtgatt(sykmelding: Sykmelding): Sykmelding {
        val sisteHendelse = sykmelding.sisteStatus()
        require(
            sisteHendelse.status in
                setOf(HendelseStatus.APEN, HendelseStatus.AVBRUTT),
        )
        sykmelding.leggTilStatus(
            SykmeldingHendelse(
                status = HendelseStatus.UTGATT,
                opprettet = nowFactory.get(),
            ),
        )
        return sykmelding
    }
}
