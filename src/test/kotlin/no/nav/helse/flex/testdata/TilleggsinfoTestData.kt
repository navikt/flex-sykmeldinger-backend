package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.domain.*

fun byggArbeidstakerTilleggsinfo(block: ArbeidstakerTilleggsinfoBuilder.() -> Unit = {}): ArbeidstakerTilleggsinfo =
    ArbeidstakerTilleggsinfoBuilder().apply(block).build()

fun byggArbeidsledigTilleggsinfo(block: ArbeidsledigTilleggsinfoBuilder.() -> Unit = {}): ArbeidsledigTilleggsinfo =
    ArbeidsledigTilleggsinfoBuilder().apply(block).build()

fun byggPermittertTilleggsinfo(block: PermittertTilleggsinfoBuilder.() -> Unit = {}): PermittertTilleggsinfo =
    PermittertTilleggsinfoBuilder().apply(block).build()

fun byggFiskerTilleggsinfo(block: FiskerTilleggsinfoBuilder.() -> Unit = {}): FiskerTilleggsinfo =
    FiskerTilleggsinfoBuilder().apply(block).build()

fun byggFrilanserTilleggsinfo(block: FrilanserTilleggsinfoBuilder.() -> Unit = {}): FrilanserTilleggsinfo =
    FrilanserTilleggsinfoBuilder().apply(block).build()

fun byggJordbrukerTilleggsinfo(block: JordbrukerTilleggsinfoBuilder.() -> Unit = {}): JordbrukerTilleggsinfo =
    JordbrukerTilleggsinfoBuilder().apply(block).build()

fun byggNaringsdrivendeTilleggsinfo(block: NaringsdrivendeTilleggsinfoBuilder.() -> Unit = {}): NaringsdrivendeTilleggsinfo =
    NaringsdrivendeTilleggsinfoBuilder().apply(block).build()

fun byggAnnetArbeidssituasjonTilleggsinfo(
    block: AnnetArbeidssituasjonTilleggsinfoBuilder.() -> Unit = {},
): AnnetArbeidssituasjonTilleggsinfo = AnnetArbeidssituasjonTilleggsinfoBuilder().apply(block).build()

@DslMarker
annotation class TestDataBuilderMarker

@TestDataBuilderMarker
class ArbeidstakerTilleggsinfoBuilder {
    var arbeidsgiverBuilder: ArbeidsgiverBuilder = ArbeidsgiverBuilder()

    fun arbeidsgiver(block: ArbeidsgiverBuilder.() -> Unit) {
        arbeidsgiverBuilder.apply(block)
    }

    fun build(): ArbeidstakerTilleggsinfo =
        ArbeidstakerTilleggsinfo(
            arbeidsgiver = arbeidsgiverBuilder.build(),
        )
}

@TestDataBuilderMarker
class ArbeidsledigTilleggsinfoBuilder {
    private var tidligereArbeidsgiverBuilder = ArbeidsgiverBuilder()

    fun tidligereArbeidsgiver(block: ArbeidsgiverBuilder.() -> Unit) {
        tidligereArbeidsgiverBuilder.apply(block)
    }

    fun build(): ArbeidsledigTilleggsinfo =
        ArbeidsledigTilleggsinfo(
            tidligereArbeidsgiver = tidligereArbeidsgiverBuilder.build(),
        )
}

@TestDataBuilderMarker
class PermittertTilleggsinfoBuilder {
    var tidligereArbeidsgiver = ArbeidsgiverBuilder()

    fun tidligereArbeidsgiver(block: ArbeidsgiverBuilder.() -> Unit) {
        tidligereArbeidsgiver.apply(block)
    }

    fun build(): PermittertTilleggsinfo =
        PermittertTilleggsinfo(
            tidligereArbeidsgiver = tidligereArbeidsgiver.build(),
        )
}

@TestDataBuilderMarker
class FiskerTilleggsinfoBuilder {
    var arbeidsgiver: ArbeidsgiverBuilder? = null

    fun arbeidsgiver(block: (ArbeidsgiverBuilder.() -> Unit)) {
        arbeidsgiver = (arbeidsgiver ?: ArbeidsgiverBuilder()).apply(block)
    }

    fun build(): FiskerTilleggsinfo =
        FiskerTilleggsinfo(
            arbeidsgiver = arbeidsgiver?.build(),
        )
}

@TestDataBuilderMarker
class FrilanserTilleggsinfoBuilder {
    fun build(): FrilanserTilleggsinfo = FrilanserTilleggsinfo
}

@TestDataBuilderMarker
class JordbrukerTilleggsinfoBuilder {
    fun build(): JordbrukerTilleggsinfo = JordbrukerTilleggsinfo
}

@TestDataBuilderMarker
class NaringsdrivendeTilleggsinfoBuilder {
    fun build(): NaringsdrivendeTilleggsinfo = NaringsdrivendeTilleggsinfo
}

@TestDataBuilderMarker
class AnnetArbeidssituasjonTilleggsinfoBuilder {
    fun build(): AnnetArbeidssituasjonTilleggsinfo = AnnetArbeidssituasjonTilleggsinfo
}

@TestDataBuilderMarker
class ArbeidsgiverBuilder {
    var orgnummer: String = "test-orgnummer"
    var juridiskOrgnummer: String = "test-juridisk-orgnummer"
    var orgnavn: String = "test-orgnavn"
    var erAktivtArbeidsforhold: Boolean = true
    var narmesteLederBuilder: NarmesteLederBuilder = NarmesteLederBuilder()

    fun narmesteLeder(block: NarmesteLederBuilder.() -> Unit) {
        narmesteLederBuilder.apply(block)
    }

    fun build() =
        Arbeidsgiver(
            orgnummer = orgnummer,
            juridiskOrgnummer = juridiskOrgnummer,
            orgnavn = orgnavn,
            erAktivtArbeidsforhold = erAktivtArbeidsforhold,
            narmesteLeder = narmesteLederBuilder.build(),
        )
}

@TestDataBuilderMarker
class NarmesteLederBuilder {
    var navn: String = "test-navn"

    fun build(): NarmesteLeder =
        NarmesteLeder(
            navn = navn,
        )
}
