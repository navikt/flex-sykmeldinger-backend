package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.serialisertTilString
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import com.fasterxml.jackson.module.kotlin.readValue
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be instance of`


class AktivitetTest {

    @ParameterizedTest
    @EnumSource(AktivitetMedAktivitetstype::class)
    fun `burde deserialisere aktivitet`(aktivitetMedType: AktivitetMedAktivitetstype) {
        val aktivitetType = aktivitetMedType.aktivitetType
        val aktivitet = aktivitetMedType.aktivitet

        val serialisertAktivitet = aktivitet.serialisertTilString()
        val deserialisertAktivitet: Aktivitet = objectMapper.readValue(serialisertAktivitet)
        deserialisertAktivitet.type `should be equal to` aktivitetType
        deserialisertAktivitet `should be instance of` aktivitet::class
    }

    enum class AktivitetMedAktivitetstype(
        val aktivitetType: AktivitetType,
        val aktivitet: Aktivitet
    ) {
        AKTIVITET_IKKE_MULIG(AktivitetType.AKTIVITET_IKKE_MULIG, lagAktivitetIkkeMulig()),
        AVVENTENDE(AktivitetType.AVVENTENDE, lagAktivitetAvventende()),
        BEHANDLINGSDAGER(AktivitetType.BEHANDLINGSDAGER, lagAktivitetBehandlingsdager()),
        GRADERT(AktivitetType.GRADERT, lagAktivitetGradert()),
        REISETILSKUDD(AktivitetType.REISETILSKUDD, lagAktivitetReisetilskudd()),
    }
}
