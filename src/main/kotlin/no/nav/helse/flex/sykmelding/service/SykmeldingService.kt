package no.nav.helse.flex.sykmelding.service

import no.nav.helse.flex.sykmelding.model.SendSykmeldingValues
import org.springframework.stereotype.Service

@Service
class SykmeldingService {
    fun getBrukerinformasjon(
        fnr: String,
        sykmeldingId: String,
    ): Map<String, Any> {
        // TODO: Implement actual logic to fetch brukerinformasjon
        return mapOf(
            "arbeidsgivere" to emptyList<String>(),
            "erUtenlandsk" to false,
        )
    }

    fun erUtenforVentetid(
        fnr: String,
        sykmeldingId: String,
    ): Boolean {
        // TODO: Implement actual logic to check ventetid
        return true
    }

    fun sendSykmelding(
        fnr: String,
        sykmeldingId: String,
        values: SendSykmeldingValues,
    ): Map<String, String> {
        // TODO: Implement actual logic to send sykmelding
        return mapOf("status" to "SENDT")
    }

    fun changeStatus(
        fnr: String,
        sykmeldingId: String,
        status: String,
    ): Map<String, String> {
        // TODO: Implement actual logic to change status
        return mapOf("status" to status)
    }
}

data class ChangeStatusRequest(
    val status: String,
)
