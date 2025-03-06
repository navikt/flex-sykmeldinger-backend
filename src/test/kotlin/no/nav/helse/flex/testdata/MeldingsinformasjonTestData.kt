package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.domain.tsm.Egenmeldt
import no.nav.helse.flex.sykmelding.domain.tsm.EmottakEnkel
import no.nav.helse.flex.sykmelding.domain.tsm.MeldingMetadata
import no.nav.helse.flex.sykmelding.domain.tsm.Meldingstype
import no.nav.helse.flex.sykmelding.domain.tsm.values.OrgId
import no.nav.helse.flex.sykmelding.domain.tsm.values.OrgIdType
import no.nav.helse.flex.sykmelding.domain.tsm.values.Organisasjon
import no.nav.helse.flex.sykmelding.domain.tsm.values.OrganisasjonsType
import java.time.OffsetDateTime

fun lagMeldingsinformasjonEnkel(): EmottakEnkel =
    EmottakEnkel(
        msgInfo =
            MeldingMetadata(
                msgId = "1",
                type = Meldingstype.SYKMELDING,
                genDate = OffsetDateTime.parse("2021-01-01T00:00:00.00Z"),
                migVersjon = null,
            ),
        sender =
            Organisasjon(
                navn = null,
                type = OrganisasjonsType.IKKE_OPPGITT,
                ids = listOf(OrgId("org-id-sender-default", OrgIdType.AKO)),
                adresse = null,
                kontaktinfo = null,
                underOrganisasjon = null,
                helsepersonell = null,
            ),
        receiver =
            Organisasjon(
                navn = null,
                type = OrganisasjonsType.IKKE_OPPGITT,
                ids = listOf(OrgId("org-id-receiver-default", OrgIdType.AKO)),
                adresse = null,
                kontaktinfo = null,
                underOrganisasjon = null,
                helsepersonell = null,
            ),
        vedlegg = null,
    )

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
