package no.nav.helse.flex.sykmeldingbekreftelse

import no.nav.helse.flex.sykmeldinghendelse.HendelseStatus
import no.nav.helse.flex.sykmeldinghendelse.SykmeldingHendelse

object BekreftelseKonverterer {
    fun konverterFraSykmeldingHendelse(sykmeldingHendelse: SykmeldingHendelse): BekreftelseDto {
        val brukersituasjon =
            sykmeldingHendelse.brukerSvar?.let {
                TilBrukersituasjonKonverterer.konverterTilBrukersituasjon(
                    brukerSvar = sykmeldingHendelse.brukerSvar,
                    tilleggsinfo = sykmeldingHendelse.tilleggsinfo,
                )
            }

        return BekreftelseDto(
            status = konverterFraHendelseStatus(sykmeldingHendelse.status),
            hendelseOpprettet = sykmeldingHendelse.hendelseOpprettet,
            brukersituasjon = brukersituasjon,
        )
    }

    fun konverterTilSykmeldingHendelse(
        bekreftelse: BekreftelseDto,
        databaseId: String? = null,
        source: String? = null,
    ): SykmeldingHendelse {
        val hendelseStatus =
            konverterTilHendelseStatus(bekreftelseStatus = bekreftelse.status, yrkesgruppe = bekreftelse.brukersituasjon?.yrkesgruppe)

        val brukerSvar =
            bekreftelse.brukersituasjon?.let {
                FraBrukersituasjonKonverterer.konverterTilBrukerSvar(it)
            }

        val tilleggsinfo =
            bekreftelse.brukersituasjon?.let {
                FraBrukersituasjonKonverterer.konverterTilTilleggsinfo(it)
            }

        return SykmeldingHendelse(
            databaseId = databaseId,
            source = source,
            status = hendelseStatus,
            brukerSvar = brukerSvar,
            tilleggsinfo = tilleggsinfo,
            hendelseOpprettet = bekreftelse.hendelseOpprettet,
            lokaltOpprettet = bekreftelse.hendelseOpprettet,
        )
    }

    fun konverterTilHendelseStatus(
        bekreftelseStatus: BekreftelseStatusDto,
        yrkesgruppe: Yrkesgruppe? = null,
    ): HendelseStatus =
        when (bekreftelseStatus) {
            BekreftelseStatusDto.APEN -> {
                HendelseStatus.APEN
            }

            BekreftelseStatusDto.SENDT -> {
                requireNotNull(yrkesgruppe)
                if (yrkesgruppe == Yrkesgruppe.ARBEIDSTAKER) {
                    HendelseStatus.SENDT_TIL_ARBEIDSGIVER
                } else {
                    HendelseStatus.SENDT_TIL_NAV
                }
            }

            BekreftelseStatusDto.BEKREFTET_AVVIST -> {
                HendelseStatus.BEKREFTET_AVVIST
            }

            BekreftelseStatusDto.AVBRUTT -> {
                HendelseStatus.AVBRUTT
            }
        }

    fun konverterFraHendelseStatus(hendelseStatus: HendelseStatus): BekreftelseStatusDto =
        when (hendelseStatus) {
            HendelseStatus.APEN -> BekreftelseStatusDto.APEN

            HendelseStatus.AVBRUTT -> BekreftelseStatusDto.AVBRUTT

            HendelseStatus.SENDT_TIL_NAV,
            HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
            -> BekreftelseStatusDto.SENDT

            HendelseStatus.BEKREFTET_AVVIST -> BekreftelseStatusDto.BEKREFTET_AVVIST

            HendelseStatus.UTGATT -> throw IllegalArgumentException("Kan ikke konvertere status UTGATT")
        }
}
