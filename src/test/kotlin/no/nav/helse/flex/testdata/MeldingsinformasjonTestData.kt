package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.domain.tsm.*
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

fun lagMeldingsinformasjonEDIEmottak(): EDIEmottak =
    EDIEmottak(
        mottakenhetBlokk =
            MottakenhetBlokk(
                ediLogid = "logid",
                avsender = "avsender",
                ebXMLSamtaleId = "samtaleId",
                mottaksId = "mottaksId",
                meldingsType = "meldingsType",
                avsenderRef = "avsenderRef",
                avsenderFnrFraDigSignatur = "avsenderFnr",
                mottattDato = OffsetDateTime.parse("2021-01-01T00:00:00.00Z"),
                orgnummer = "orgnummer",
                avsenderOrgNrFraDigSignatur = "avsenderOrgNr",
                partnerReferanse = "partnerRef",
                herIdentifikator = "herId",
                ebRole = "role",
                ebService = "service",
                ebAction = "action",
            ),
        ack =
            Ack(
                ackType = AckType.JA,
            ),
        msgInfo =
            MeldingMetadata(
                type = Meldingstype.SYKMELDING,
                genDate = OffsetDateTime.parse("2021-01-01T00:00:00.00Z"),
                msgId = "msgId",
                migVersjon = "1.0",
            ),
        sender =
            Organisasjon(
                navn = "sender",
                type = OrganisasjonsType.IKKE_OPPGITT,
                ids = listOf(OrgId("senderId", OrgIdType.AKO)),
                adresse = null,
                kontaktinfo = null,
                underOrganisasjon = null,
                helsepersonell = null,
            ),
        receiver =
            Organisasjon(
                navn = "receiver",
                type = OrganisasjonsType.IKKE_OPPGITT,
                ids = listOf(OrgId("receiverId", OrgIdType.AKO)),
                adresse = null,
                kontaktinfo = null,
                underOrganisasjon = null,
                helsepersonell = null,
            ),
        pasient =
            MetadataPasient(
                ids = listOf(MetadataId("id", "type")),
                navn =
                    MetadataNavn(
                        fornavn = "fornavn",
                        mellomnavn = "mellomnavn",
                        etternavn = "etternavn",
                    ),
                fodselsdato = "1990-01-01",
                kjonn = "M",
                nasjonalitet = "NO",
                adresse =
                    MetadataAdresse(
                        type = "type",
                        gateadresse = "gateadresse",
                        postnummer = "postnummer",
                        poststed = "poststed",
                        postboks = "postboks",
                        kommune = "kommune",
                        land = "NO",
                    ),
                kontaktinfo = listOf(MetadataKontaktinfo("type", "value")),
            ),
        vedlegg = listOf("vedlegg1", "vedlegg2"),
    )
