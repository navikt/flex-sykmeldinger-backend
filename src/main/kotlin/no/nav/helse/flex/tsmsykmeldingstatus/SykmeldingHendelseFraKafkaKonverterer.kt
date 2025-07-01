package no.nav.helse.flex.tsmsykmeldingstatus

import no.nav.helse.flex.api.dto.TidligereArbeidsgiver
import no.nav.helse.flex.sykmelding.application.*
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.tsmsykmeldingstatus.dto.*
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.function.Supplier

@Component
class SykmeldingHendelseFraKafkaKonverterer(
    private val nowFactory: Supplier<Instant>,
) {
    fun konverterSykmeldingHendelseFraKafkaDTO(
        status: SykmeldingStatusKafkaDTO,
        erSykmeldingAvvist: Boolean = false,
        source: String? = null,
    ): SykmeldingHendelse {
        val hendelseStatus = konverterStatusTilHendelseStatus(status.statusEvent, erAvvist = erSykmeldingAvvist)
        val brukerSvar =
            when {
                status.brukerSvar != null -> BrukerSvarKafkaDtoKonverterer.tilBrukerSvar(status.brukerSvar)
                status.sporsmals != null ->
                    SporsmalKafkaDtoKonverterer.tilUtdatertFormatBrukerSvar(
                        sporsmal = status.sporsmals,
                        hendelseStatus = hendelseStatus,
                    )
                else -> null
            }
        if (hendelseStatus in setOf(HendelseStatus.SENDT_TIL_NAV, HendelseStatus.SENDT_TIL_ARBEIDSGIVER)) {
            requireNotNull(brukerSvar) {
                "Brukersvar er påkrevd for SENDT_TIL_NAV og SENDT_TIL_ARBEIDSGIVER"
            }
        }

        val tilleggsinfo =
            brukerSvar?.let {
                konverterTilTilleggsinfo(
                    brukerSvarType = it.type,
                    arbeidsgiver = status.arbeidsgiver,
                    tidligereArbeidsgiver = status.tidligereArbeidsgiver,
                )
            }

        return SykmeldingHendelse(
            status = hendelseStatus,
            brukerSvar = brukerSvar,
            tilleggsinfo = tilleggsinfo,
            source = source,
            hendelseOpprettet = status.timestamp.toInstant(),
            lokaltOpprettet = nowFactory.get(),
        )
    }

    internal fun konverterStatusTilHendelseStatus(
        status: String,
        erAvvist: Boolean,
    ): HendelseStatus =
        when (status) {
            "APEN" -> HendelseStatus.APEN
            "AVBRUTT" -> HendelseStatus.AVBRUTT
            "BEKREFTET" -> {
                when (erAvvist) {
                    true -> HendelseStatus.BEKREFTET_AVVIST
                    false -> HendelseStatus.SENDT_TIL_NAV
                }
            }
            "SENDT" -> HendelseStatus.SENDT_TIL_ARBEIDSGIVER
            "UTGATT" -> HendelseStatus.UTGATT
            else -> throw IllegalArgumentException("Ukjent status: $status")
        }

    internal fun konverterTilTilleggsinfo(
        brukerSvarType: BrukerSvarType,
        arbeidsgiver: ArbeidsgiverStatusKafkaDTO? = null,
        tidligereArbeidsgiver: TidligereArbeidsgiverKafkaDTO? = null,
    ): Tilleggsinfo =
        when (brukerSvarType) {
            BrukerSvarType.ARBEIDSTAKER -> {
                requireNotNull(arbeidsgiver) { "Arbeidsgiver er påkrevd for arbeidstaker" }
                ArbeidstakerTilleggsinfo(arbeidsgiver = konverterArbeidsgiver(arbeidsgiver))
            }
            BrukerSvarType.ARBEIDSLEDIG -> {
                ArbeidsledigTilleggsinfo(
                    tidligereArbeidsgiver = tidligereArbeidsgiver?.let { konverterTidligereArbeidsgiver(it) },
                )
            }
            BrukerSvarType.PERMITTERT -> {
                PermittertTilleggsinfo(
                    tidligereArbeidsgiver = tidligereArbeidsgiver?.let { konverterTidligereArbeidsgiver(it) },
                )
            }
            BrukerSvarType.FISKER ->
                FiskerTilleggsinfo(
                    arbeidsgiver = arbeidsgiver?.let { konverterArbeidsgiver(it) },
                )
            BrukerSvarType.FRILANSER -> FrilanserTilleggsinfo
            BrukerSvarType.NAERINGSDRIVENDE -> NaringsdrivendeTilleggsinfo
            BrukerSvarType.JORDBRUKER -> JordbrukerTilleggsinfo
            BrukerSvarType.ANNET -> AnnetArbeidssituasjonTilleggsinfo
            BrukerSvarType.UTDATERT_FORMAT ->
                UtdatertFormatTilleggsinfo(
                    arbeidsgiver = arbeidsgiver?.let { konverterArbeidsgiverTilUtdatertFormatArbeidsgiver(it) },
                    tidligereArbeidsgiver = tidligereArbeidsgiver?.let { konverterTidligereArbeidsgiver(it) },
                )
        }

    internal fun konverterArbeidsgiver(arbeidsgiver: ArbeidsgiverStatusKafkaDTO): Arbeidsgiver {
        requireNotNull(arbeidsgiver.juridiskOrgnummer) { "Arbeidsgiver juridiskOrgnummer er påkrevd" }
        return Arbeidsgiver(
            orgnummer = arbeidsgiver.orgnummer,
            juridiskOrgnummer = arbeidsgiver.juridiskOrgnummer,
            orgnavn = arbeidsgiver.orgNavn,
            // TODO: Hvordan finner vi ut av dette?
            erAktivtArbeidsforhold = true,
            // TODO: Trenger vi dette?
            narmesteLeder = null,
        )
    }

    internal fun konverterArbeidsgiverTilUtdatertFormatArbeidsgiver(arbeidsgiver: ArbeidsgiverStatusKafkaDTO): UtdatertFormatArbeidsgiver =
        UtdatertFormatArbeidsgiver(
            orgnummer = arbeidsgiver.orgnummer,
            juridiskOrgnummer = arbeidsgiver.juridiskOrgnummer,
            orgnavn = arbeidsgiver.orgNavn,
        )

    internal fun konverterTidligereArbeidsgiver(tidligereArbeidsgiver: TidligereArbeidsgiverKafkaDTO): TidligereArbeidsgiver {
        requireNotNull(tidligereArbeidsgiver.orgnummer) { "Tidligere arbeidsgiver orgnummer er påkrevd" }
        requireNotNull(tidligereArbeidsgiver.orgNavn) { "Tidligere arbeidsgiver orgnavn er påkrevd" }
        return TidligereArbeidsgiver(
            orgnummer = tidligereArbeidsgiver.orgnummer,
            orgNavn = tidligereArbeidsgiver.orgNavn,
        )
    }
}
