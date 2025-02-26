package no.nav.helse.flex.clients.pdl

import java.io.Serializable

data class PersonIdenter(
    val originalIdent: String,
    val andreIdenter: List<String> = emptyList(),
) : Serializable {
    fun alle(): List<String> = mutableListOf(originalIdent).also { it.addAll(andreIdenter) }
}
