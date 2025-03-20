package no.nav.helse.flex.sykmelding.domain

object SporsmalMaler {
    val ARBEIDSGIVER_ORGNUMMER =
        SporsmalMal(
            tag = SporsmalTag.ARBEIDSGIVER_ORGNUMMER,
            svarType = FritekstSvartype(),
        )
    val ARBEIDSSITUASJON =
        SporsmalMal(
            tag = SporsmalTag.ARBEIDSSITUASJON,
            svarType = RadioSvartype(),
        )

    val ARBEIDSLEDIG_FRA_ORGNUMMER =
        SporsmalMal(
            tag = SporsmalTag.ARBEIDSLEDIG_FRA_ORGNUMMER,
            svarType = FritekstSvartype(),
        )

    val ER_OPPLYSNINGENE_RIKTIGE =
        SporsmalMal(
            tag = SporsmalTag.ER_OPPLYSNINGENE_RIKTIGE,
            svarType = JaNeiSvartype(),
        )
}

fun <S> List<Sporsmal>.findWithMal(mal: SporsmalMal<S>): SporsmalMal<S> =
    findWithMalOrNull(mal) ?: throw IllegalArgumentException("Fant ikke spørsmål med tag ${mal.tag}")

fun <S> List<Sporsmal>.findWithMalOrNull(mal: SporsmalMal<S>): SporsmalMal<S>? {
    val sporsmal = this.find { it.tag == mal.tag }
    if (sporsmal == null) {
        return null
    } else {
        return mal.medSporsmal(sporsmal)
    }
}

class SporsmalMal<S>(
    val tag: SporsmalTag,
    private val svarType: SvartypeMal<S>,
    sporsmalstekst: String? = null,
    sporsmal: Sporsmal? = null,
) {
    val sporsmal =
        sporsmal ?: Sporsmal(
            tag = tag,
            svartype = svarType.svartype,
            sporsmalstekst = sporsmalstekst,
        )

    init {
        if (sporsmal != null) {
            require(sporsmal.tag == tag) { "Spørsmålet har feil tag" }
        }
    }

    fun harSvar(): Boolean = konverterSvar() != null

    fun svar(): S =
        svarOrNull()
            ?: throw IllegalStateException("Spørsmål $tag har ikke blitt besvart")

    fun svarOrNull(): S? = konverterSvar()

    fun medSvar(verdi: S): SporsmalMal<S> {
        val svar = svarType.konverterTilSvarliste(verdi)
        return medSporsmal(sporsmal.copy(svar = svar))
    }

    fun medSporsmal(sporsmal: Sporsmal): SporsmalMal<S> =
        SporsmalMal(
            tag = tag,
            svarType = svarType,
            sporsmal = sporsmal,
        )

    private fun konverterSvar(): S? = svarType.konverterFraSvarliste(sporsmal.svar)
}

interface SvartypeMal<S> {
    val svartype: Svartype
    val undersporsmal: List<Sporsmal>

    fun konverterFraSvarliste(svar: List<Svar>): S?

    fun konverterTilSvarliste(verdi: S): List<Svar>
}

class JaNeiSvartype : SvartypeMal<Boolean> {
    override val svartype: Svartype = Svartype.JA_NEI
    override val undersporsmal: List<Sporsmal> = emptyList()

    override fun konverterFraSvarliste(svar: List<Svar>): Boolean? {
        val forsteSvar = svar.firstOrNull()?.verdi ?: return null
        return when (forsteSvar) {
            "JA" -> true
            "NEI" -> false
            else -> throw IllegalArgumentException("Ukjent svar: $forsteSvar")
        }
    }

    override fun konverterTilSvarliste(verdi: Boolean): List<Svar> {
        val svar =
            Svar(
                verdi = if (verdi) "JA" else "NEI",
            )
        return listOf(svar)
    }
}

class FritekstSvartype : SvartypeMal<String> {
    override val svartype: Svartype = Svartype.FRITEKST
    override val undersporsmal: List<Sporsmal> = emptyList()

    override fun konverterFraSvarliste(svar: List<Svar>): String? {
        val forsteSvarVerdi = svar.firstOrNull()?.verdi ?: return null
        return forsteSvarVerdi
    }

    override fun konverterTilSvarliste(verdi: String): List<Svar> {
        val svar =
            Svar(
                verdi = verdi,
            )
        return listOf(svar)
    }
}

class RadioSvartype : SvartypeMal<String> {
    override val svartype: Svartype = Svartype.RADIO
    override val undersporsmal: List<Sporsmal> = emptyList()

    override fun konverterFraSvarliste(svar: List<Svar>): String? {
        val forsteSvarVerdi = svar.firstOrNull()?.verdi ?: return null
        return forsteSvarVerdi
    }

    override fun konverterTilSvarliste(verdi: String): List<Svar> {
        val svar =
            Svar(
                verdi = verdi,
            )
        return listOf(svar)
    }
}
