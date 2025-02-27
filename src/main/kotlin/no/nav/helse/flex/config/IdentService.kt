package no.nav.helse.flex.config

import no.nav.helse.flex.clients.pdl.PdlClient
import no.nav.helse.flex.clients.pdl.PdlIdent
import org.springframework.stereotype.Component
import java.io.Serializable

@Component
class IdentService(
    private val pdlClient: PdlClient,
) {
    companion object {
        const val FOLKEREGISTERIDENT = "FOLKEREGISTERIDENT"
    }

    fun hentFolkeregisterIdenterMedHistorikkForFnr(fnr: String): PersonIdenter {
        val identer = pdlClient.hentIdenterMedHistorikk(fnr)
        return PersonIdenter(
            originalIdent = fnr,
            andreIdenter = identer.folkeregisteridenter().filterNot { it == fnr },
        )
    }

    private fun List<PdlIdent>.folkeregisteridenter(): List<String> = this.filter { it.gruppe == FOLKEREGISTERIDENT }.map { it.ident }
}

data class PersonIdenter(
    val originalIdent: String,
    val andreIdenter: List<String> = emptyList(),
) : Serializable {
    fun alle(): List<String> = mutableListOf(originalIdent).also { it.addAll(andreIdenter) }
}
