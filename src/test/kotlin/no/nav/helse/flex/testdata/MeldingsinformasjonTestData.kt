package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.domain.tsm.Egenmeldt
import no.nav.helse.flex.sykmelding.domain.tsm.MeldingMetadata
import no.nav.helse.flex.sykmelding.domain.tsm.Meldingstype
import java.time.OffsetDateTime

fun lagMeldingsinformasjonEgenmeldt(): Egenmeldt =
    Egenmeldt(
        msgInfo =
            MeldingMetadata(
                type = Meldingstype.SYKMELDING,
                genDate = OffsetDateTime.parse("2021-01-01T00:00:00.00Z"),
                msgId = "0",
                migVersjon = null,
            ),
    )
