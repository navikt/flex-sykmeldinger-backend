package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.sykmelding.UgyldigSykmeldingStatusException
import no.nav.helse.flex.sykmelding.domain.tsm.AvsenderSystemNavn
import org.springframework.stereotype.Service

@Service
class SykmeldingStatusEndrer {
    fun sjekkStatusEndring(
        sykmelding: Sykmelding,
        nyStatus: HendelseStatus,
    ) {
        if (sykmelding.sykmeldingGrunnlag.metadata.avsenderSystem.navn == AvsenderSystemNavn.EGENMELDT) {
            throw UgyldigSykmeldingStatusException(
                "Kan ikke endre status til $nyStatus fordi sykmelding er egenmeldt",
            )
        }

        val sisteStatus = sykmelding.sisteHendelse().status

        when (nyStatus) {
            HendelseStatus.APEN -> {
                if (
                    sisteStatus !in
                    setOf(HendelseStatus.APEN, HendelseStatus.SENDT_TIL_NAV, HendelseStatus.AVBRUTT)
                ) {
                    throw UgyldigSykmeldingStatusException(
                        "Kan ikke endre status til ${HendelseStatus.APEN} fra $sisteStatus",
                    )
                }

                if (sykmelding.erAvvist) {
                    throw UgyldigSykmeldingStatusException(
                        "Kan ikke endre status til $nyStatus fordi sykmelding er avvist",
                    )
                }
            }
            HendelseStatus.SENDT_TIL_ARBEIDSGIVER -> {
                if (
                    sisteStatus !in
                    setOf(
                        HendelseStatus.APEN,
                        HendelseStatus.SENDT_TIL_NAV,
                        HendelseStatus.AVBRUTT,
                    )
                ) {
                    throw UgyldigSykmeldingStatusException(
                        "Kan ikke endre status til $nyStatus fra $sisteStatus",
                    )
                }

                if (sykmelding.erAvvist) {
                    throw UgyldigSykmeldingStatusException(
                        "Kan ikke endre status til $nyStatus fordi sykmelding er avvist",
                    )
                }
            }

            HendelseStatus.SENDT_TIL_NAV -> {
                if (
                    sisteStatus !in
                    setOf(
                        HendelseStatus.APEN,
                        HendelseStatus.SENDT_TIL_NAV,
                        HendelseStatus.AVBRUTT,
                    )
                ) {
                    throw UgyldigSykmeldingStatusException(
                        "Kan ikke endre status til ${HendelseStatus.SENDT_TIL_NAV} fra $sisteStatus",
                    )
                }

                if (sykmelding.erAvvist) {
                    throw UgyldigSykmeldingStatusException(
                        "Kan ikke endre status til ${HendelseStatus.SENDT_TIL_NAV} fordi sykmelding er avvist",
                    )
                }
            }
            HendelseStatus.BEKREFTET_AVVIST -> {
                if (
                    sisteStatus !in
                    setOf(
                        HendelseStatus.APEN,
                    )
                ) {
                    throw UgyldigSykmeldingStatusException(
                        "Kan ikke endre status til $nyStatus fra $sisteStatus",
                    )
                }

                if (!sykmelding.erAvvist) {
                    throw UgyldigSykmeldingStatusException(
                        "Kan ikke endre status til $nyStatus fordi sykmelding ikke er avvist",
                    )
                }
            }
            HendelseStatus.AVBRUTT -> {
                if (
                    sisteStatus !in
                    setOf(HendelseStatus.APEN, HendelseStatus.SENDT_TIL_NAV, HendelseStatus.AVBRUTT)
                ) {
                    throw UgyldigSykmeldingStatusException(
                        "Kan ikke endre status til $nyStatus fra $sisteStatus",
                    )
                }

                if (sykmelding.erAvvist) {
                    throw UgyldigSykmeldingStatusException(
                        "Kan ikke endre status til $nyStatus fordi sykmelding er avvist",
                    )
                }
            }

            HendelseStatus.UTGATT -> {
                if (
                    sisteStatus !in
                    setOf(HendelseStatus.APEN, HendelseStatus.AVBRUTT)
                ) {
                    throw UgyldigSykmeldingStatusException(
                        "Kan ikke endre status til $nyStatus fra $sisteStatus",
                    )
                }
            }
        }
    }
}
