package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.clients.pdl.PdlClient
import no.nav.helse.flex.clients.pdl.PdlIdent

class PdlClientFake : PdlClient {
    private val identerMedHistorikk: MutableMap<String, Result<List<PdlIdent>>> = mutableMapOf()
    private val formaterteNavn: MutableMap<String, Result<String>> = mutableMapOf()

    companion object {
        val defaultIdenterMedHistorikk = listOf(PdlIdent(gruppe = "default-gruppe", ident = "default-ident"))
        val defaultFormatertNavn = "default-navn"
    }

    fun setIdentMedHistorikk(
        identer: List<PdlIdent>,
        ident: String = "__accept_any_ident",
    ) {
        this.identerMedHistorikk[ident] = Result.success(identer)
    }

    fun setIdentMedHistorikk(
        failure: Exception,
        ident: String = "__accept_any_ident",
    ) {
        this.identerMedHistorikk[ident] = Result.failure(failure)
    }

    fun setFormatertNavn(
        navn: String,
        fnr: String = "__accept_any_fnr",
    ) {
        this.formaterteNavn[fnr] = Result.success(navn)
    }

    fun setFormatertNavn(
        failure: Exception,
        fnr: String = "__accept_any_fnr",
    ) {
        this.formaterteNavn[fnr] = Result.failure(failure)
    }

    fun reset() {
        identerMedHistorikk.clear()
        formaterteNavn.clear()
    }

    override fun hentIdenterMedHistorikk(ident: String): List<PdlIdent> {
        if (identerMedHistorikk.isEmpty()) {
            return defaultIdenterMedHistorikk
        }

        val identHistorikk =
            identerMedHistorikk[ident] ?: identerMedHistorikk["__accept_any_ident"]
                ?: throw IllegalStateException("No response found for $ident")
        return identHistorikk.getOrThrow()
    }

    override fun hentFormattertNavn(fnr: String): String {
        if (formaterteNavn.isEmpty()) {
            return defaultFormatertNavn
        }
        val formattertNavn =
            formaterteNavn[fnr] ?: formaterteNavn["__accept_any_fnr"] ?: throw IllegalStateException("No response found for $fnr")
        return formattertNavn.getOrThrow()
    }
}
