package no.nav.helse.flex.sykmelding.service

import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.sykmelding.model.SendSykmeldingValues
import no.nav.helse.flex.sykmelding.repository.SykmeldingDbRecord
import no.nav.helse.flex.sykmelding.repository.SykmeldingRepository
import no.nav.helse.flex.sykmelding.repository.SykmeldingStatusDbRecord
import no.nav.helse.flex.sykmelding.repository.SykmeldingStatusRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.function.Supplier

@Service
class SykmeldingService(
    private val sykmeldingRepository: SykmeldingRepository,
    private val sykmeldingStatusRepository: SykmeldingStatusRepository,
    private val arbeidsforholdRepository: ArbeidsforholdRepository,
    private val nowProvider: Supplier<Instant> = Supplier { Instant.now() },
) {
    fun getSykmeldinger(fnr: String): List<SykmeldingMedBehandlingsutfall> {
        return sykmeldingRepository.findByFnr(fnr).map { it.tilSykmeldingMedBehandlingsutfall() }
    }

    fun getSykmelding(
        fnr: String,
        sykmeldingId: String,
    ): SykmeldingMedBehandlingsutfall? {
        val sykmelding = sykmeldingRepository.findBySykmeldingId(sykmeldingId)
        return if (sykmelding?.fnr == fnr) {
            sykmelding.tilSykmeldingMedBehandlingsutfall()
        } else {
            null
        }
    }

    fun finnTidligereArbeidsgivere(
        fnr: String,
        sykmeldingId: String,
    ): List<TidligereArbeidsgiverDTO> {
        val arbeidsforhold = arbeidsforholdRepository.getAllByFnr(fnr)
        return arbeidsforhold.map {
            TidligereArbeidsgiverDTO(
                orgnummer = it.orgnummer,
                orgNavn = it.orgnavn,
                fom = it.fom,
                tom = it.tom ?: LocalDate.now(),
            )
        }
    }

    fun getBrukerinformasjon(
        fnr: String,
        sykmeldingId: String,
    ): Map<String, Any> {
        val arbeidsforhold = arbeidsforholdRepository.getAllByFnr(fnr)
        val sykmelding = sykmeldingRepository.findBySykmeldingId(sykmeldingId)

        return mapOf(
            "arbeidsgivere" to arbeidsforhold.map { it.orgnummer },
            "erUtenlandsk" to (
                sykmelding?.sykmelding?.let {
                    objectMapper.readValue(it, ISykmelding::class.java).type == SykmeldingType.UTENLANDSK_SYKMELDING
                } ?: false
            ),
        )
    }

    fun erUtenforVentetid(
        fnr: String,
        sykmeldingId: String,
    ): Boolean {
        // TODO: Implementer faktisk logikk for ventetidssjekk
        return true
    }

    fun sendSykmelding(
        fnr: String,
        sykmeldingId: String,
        values: SendSykmeldingValues,
    ): Map<String, String> {
        val sykmelding =
            sykmeldingRepository.findBySykmeldingId(sykmeldingId)
                ?: throw RuntimeException("Fant ikke sykmelding")

        if (sykmelding.fnr != fnr) {
            throw RuntimeException("Feil fnr")
        }

        val now = nowProvider.get()

        // Opprett ny status med korrekte felter
        val statusDbRecord =
            SykmeldingStatusDbRecord(
                id = UUID.randomUUID().toString(),
                sykmeldingUuid = sykmeldingId,
                timestamp = now,
                status = "SENDT",
                tidligereArbeidsgiver = values.arbeidsgiverNavn,
                sporsmal = null,
                opprettet = now,
            )

        val lagretStatus = sykmeldingStatusRepository.save(statusDbRecord)

        // Oppdater sykmelding med ny status
        sykmeldingRepository.save(
            sykmelding.copy(
                sisteSykmeldingstatusId = lagretStatus.id,
                oppdatert = now,
            ),
        )

        return mapOf("status" to "SENDT")
    }

    fun changeStatus(
        fnr: String,
        sykmeldingId: String,
        status: String,
    ): Map<String, String> {
        val sykmelding =
            sykmeldingRepository.findBySykmeldingId(sykmeldingId)
                ?: throw RuntimeException("Fant ikke sykmelding")

        if (sykmelding.fnr != fnr) {
            throw RuntimeException("Feil fnr")
        }

        val now = nowProvider.get()

        // Opprett ny status med korrekte felter
        val statusDbRecord =
            SykmeldingStatusDbRecord(
                id = UUID.randomUUID().toString(),
                sykmeldingUuid = sykmeldingId,
                timestamp = now,
                status = status,
                tidligereArbeidsgiver = null,
                sporsmal = null,
                opprettet = now,
            )

        val lagretStatus = sykmeldingStatusRepository.save(statusDbRecord)

        // Oppdater sykmelding med ny status
        sykmeldingRepository.save(
            sykmelding.copy(
                sisteSykmeldingstatusId = lagretStatus.id,
                oppdatert = now,
            ),
        )

        return mapOf("status" to status)
    }

    private fun SykmeldingDbRecord.tilSykmeldingMedBehandlingsutfall(): SykmeldingMedBehandlingsutfall {
        TODO("ikke implementert")
    }
}
