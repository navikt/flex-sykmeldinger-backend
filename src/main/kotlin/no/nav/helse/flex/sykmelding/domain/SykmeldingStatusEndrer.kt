package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.sykmelding.UgyldigSykmeldingStatusException
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.virksomhet.VirksomhetHenterService
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.function.Supplier

@Service
class SykmeldingStatusEndrer(
    private val nowFactory: Supplier<Instant>,
    private val virksomhetHenterService: VirksomhetHenterService,
) {
    private val log = logger()

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

        if (sykmelding.erAvvist) {
            throw UgyldigSykmeldingStatusException(
                "Kan ikke endre status til ${HendelseStatus.SENDT_TIL_ARBEIDSGIVER} fordi sykmelding er avvist",
            )
        }

        if (sykmelding.erEgenmeldt) {
            throw UgyldigSykmeldingStatusException(
                "Kan ikke endre status til ${HendelseStatus.SENDT_TIL_ARBEIDSGIVER} fordi sykmelding er egenmeldt",
            )
        }

        val arbeidstakerInfo: ArbeidstakerInfo? =
            if (arbeidsgiverOrgnummer != null) {
                val arbeidsforhold =
                    virksomhetHenterService.hentVirksomheterForPersonInnenforPeriode(
                        identer = identer,
                        periode = sykmelding.fom to sykmelding.tom,
                    )
                val valgtArbeidsforhold = arbeidsforhold.find { it.orgnummer == arbeidsgiverOrgnummer }
                if (valgtArbeidsforhold == null) {
                    // TODO: Exceptions should be mapped to a HTTP error code
                    throw IllegalArgumentException("Fant ikke arbeidsgiver med orgnummer $arbeidsgiverOrgnummer")
                }
                ArbeidstakerInfo(
                    arbeidsgiver =
                        Arbeidsgiver(
                            orgnummer = valgtArbeidsforhold.orgnummer,
                            juridiskOrgnummer = valgtArbeidsforhold.juridiskOrgnummer,
                            orgnavn = valgtArbeidsforhold.navn,
                            erAktivtArbeidsforhold = valgtArbeidsforhold.aktivtArbeidsforhold,
                            narmesteLeder =
                                valgtArbeidsforhold.naermesteLeder?.let {
                                    NarmesteLeder(
                                        navn =
                                            it.narmesteLederNavn
                                                // TODO: Exceptions should be mapped to a HTTP error code
                                                ?: throw IllegalArgumentException("Mangler narmeste leder navn"),
                                    )
                                },
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
        if (
            sisteStatus.status !in
            setOf(
                HendelseStatus.APEN,
                HendelseStatus.SENDT_TIL_NAV,
                HendelseStatus.AVBRUTT,
            )
        ) {
            throw UgyldigSykmeldingStatusException(
                "Kan ikke endre status til ${HendelseStatus.SENDT_TIL_NAV} fra ${sisteStatus.status}",
            )
        }

        if (sykmelding.erAvvist) {
            throw UgyldigSykmeldingStatusException(
                "Kan ikke endre status til ${HendelseStatus.SENDT_TIL_NAV} fordi sykmelding er avvist",
            )
        }

        if (sykmelding.erEgenmeldt) {
            throw UgyldigSykmeldingStatusException(
                "Kan ikke endre status til ${HendelseStatus.SENDT_TIL_NAV} fordi sykmelding er egenmeldt",
            )
        }

        // TODO: Hent tidligere arbeidsgivere
        val hendelse =
            SykmeldingHendelse(
                status = HendelseStatus.SENDT_TIL_NAV,
                opprettet = nowFactory.get(),
                sporsmalSvar = sporsmalSvar,
                // tidligereArbeidsgiver = tidligereArbeidsgiver,
            )

        return sykmelding.leggTilStatus(hendelse)
    }

    fun endreStatusTilBekreftetAvvist(sykmelding: Sykmelding): Sykmelding {
        val sisteStatus = sykmelding.sisteStatus()
        if (
            sisteStatus.status !in
            setOf(
                HendelseStatus.APEN,
            )
        ) {
            throw UgyldigSykmeldingStatusException(
                "Kan ikke endre status til ${HendelseStatus.BEKREFTET_AVVIST} fra ${sisteStatus.status}",
            )
        }

        if (!sykmelding.erAvvist) {
            throw UgyldigSykmeldingStatusException(
                "Kan ikke endre status til ${HendelseStatus.BEKREFTET_AVVIST} fordi sykmelding ikke er avvist",
            )
        }

        if (sykmelding.erEgenmeldt) {
            throw UgyldigSykmeldingStatusException(
                "Kan ikke endre status til ${HendelseStatus.BEKREFTET_AVVIST} fordi sykmelding er egenmeldt",
            )
        }

        val hendelse =
            SykmeldingHendelse(
                status = HendelseStatus.BEKREFTET_AVVIST,
                opprettet = nowFactory.get(),
            )

        return sykmelding.leggTilStatus(hendelse)
    }

    fun endreStatusTilAvbrutt(sykmelding: Sykmelding): Sykmelding {
        val sisteHendelse = sykmelding.sisteStatus()
        if (
            sisteHendelse.status !in
            setOf(HendelseStatus.APEN, HendelseStatus.SENDT_TIL_NAV, HendelseStatus.AVBRUTT)
        ) {
            throw UgyldigSykmeldingStatusException(
                "Kan ikke endre status til ${HendelseStatus.AVBRUTT} fra ${sisteHendelse.status}",
            )
        }

        if (sykmelding.erAvvist) {
            throw UgyldigSykmeldingStatusException(
                "Kan ikke endre status til ${HendelseStatus.AVBRUTT} fordi sykmelding er avvist",
            )
        }

        if (sykmelding.erEgenmeldt) {
            throw UgyldigSykmeldingStatusException(
                "Kan ikke endre status til ${HendelseStatus.AVBRUTT} fordi sykmelding er egenmeldt",
            )
        }

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
