package no.nav.helse.flex.config

import no.nav.helse.flex.gateways.pdl.PdlClient
import no.nav.helse.flex.gateways.pdl.PdlIdent
import no.nav.helse.flex.utils.logger
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.io.Serializable
import java.time.LocalDate

@Component
class IdentService(
    private val pdlClient: PdlClient,
) {
    private val logger = logger()

    companion object {
        const val FOLKEREGISTERIDENT = "FOLKEREGISTERIDENT"
    }

    @Cacheable("flex-folkeregister-identer-med-historikk")
    fun hentFolkeregisterIdenterMedHistorikkForFnr(fnr: String): PersonIdenter {
        logger.info("Fant ikke verdi i cache 'flex-folkeregister-identer-med-historikk'")
        val identer = pdlClient.hentIdenterMedHistorikk(fnr)
        return PersonIdenter(
            originalIdent = fnr,
            andreIdenter = identer.folkeregisteridenter().filterNot { it == fnr },
        )
    }

    @Cacheable("flex-foedselsdato")
    fun hentFoedselsdato(fnr: String): LocalDate {
        logger.info("Fant ikke verdi i cache 'flex-foedselsdato'")
        return pdlClient.hentFoedselsdato(fnr)
    }

    private fun List<PdlIdent>.folkeregisteridenter(): List<String> = this.filter { it.gruppe == FOLKEREGISTERIDENT }.map { it.ident }
}

data class PersonIdenter(
    val originalIdent: String,
    val andreIdenter: List<String> = emptyList(),
) : Serializable {
    fun alle(): List<String> = mutableListOf(originalIdent).also { it.addAll(andreIdenter) }
}
