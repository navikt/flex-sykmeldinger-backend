package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.clients.pdl.PdlClient
import no.nav.helse.flex.clients.pdl.PdlIdent
import java.time.LocalDate

class PdlClientFake : PdlClient {
    private val identerMedHistorikk: MutableMap<String, Result<List<PdlIdent>>> = mutableMapOf()
    private val formaterteNavn: MutableMap<String, Result<String>> = mutableMapOf()
    private val foedselsdato: MutableMap<String, Result<LocalDate>> = mutableMapOf()

    companion object {
        val defaultIdenterMedHistorikk = listOf(PdlIdent(gruppe = "default-gruppe", ident = "default-ident"))
        val defaultFormatertNavn = "default-navn"
        val defaultFoedselsdato = LocalDate.parse("2000-01-01")
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
        foedselsdato.clear()
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

    override fun hentFoedselsdato(fnr: String): LocalDate =
        if (foedselsdato.isEmpty()) {
            defaultFoedselsdato
        } else {
            val foedselsdatoResult =
                foedselsdato[fnr] ?: foedselsdato["__accept_any_fnr"] ?: throw IllegalStateException("No response found for $fnr")
            foedselsdatoResult.getOrThrow()
        }

    fun setFoedselsdato(
        foedselsdato: LocalDate,
        fnr: String = "__accept_any_fnr",
    ) {
        this.foedselsdato[fnr] = Result.success(foedselsdato)
    }
}
