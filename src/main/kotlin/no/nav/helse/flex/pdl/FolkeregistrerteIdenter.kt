package no.nav.helse.flex.pdl

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.io.Serializable

@Component
class FolkeregistrerteIdenter(private val pdlClient: PdlClient) {
    private fun List<PdlIdent>.folkeregisteridenter(): List<String> {
        return this.filter { it.gruppe == FOLKEREGISTERIDENT }.map { it.ident }
    }

    @Cacheable("flex-folkeregister-identer-med-historikk")
    fun hentFolkeregisterIdenterMedHistorikkForFnr(fnr: String): FolkeregisterIdenter {
        val identer = pdlClient.hentIdenterMedHistorikk(fnr)
        return FolkeregisterIdenter(
            originalIdent = fnr,
            andreIdenter = identer.folkeregisteridenter().filterNot { it == fnr },
        )
    }
}

data class FolkeregisterIdenter(val originalIdent: String, val andreIdenter: List<String>) : Serializable {
    fun alle(): List<String> {
        return mutableListOf(originalIdent).also { it.addAll(andreIdenter) }
    }
}
