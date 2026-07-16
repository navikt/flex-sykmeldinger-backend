package no.nav.helse.flex.testdata

import no.nav.helse.flex.gateways.SykmeldingNotifikasjon
import no.nav.helse.flex.gateways.SykmeldingNotifikasjonStatus
import no.nav.helse.flex.outbox.OutboxDbRecord
import no.nav.helse.flex.outbox.OutboxType
import no.nav.helse.flex.outbox.SykmeldingHendelsePayload
import no.nav.helse.flex.utils.tilPsqlJson
import org.postgresql.util.PGobject
import java.time.Instant
import java.time.LocalDateTime

fun lagOutboxDbRecord(
    id: Long? = null,
    type: OutboxType = OutboxType.SYKMELDING_HENDELSE,
    fnr: String = "fnr-1",
    payload: PGobject = lagSykmeldingHendelsePayload().tilPsqlJson(),
    opprettetTimestamp: Instant = Instant.parse("2024-06-01T12:00:00Z"),
    sendtTimestamp: Instant? = null,
): OutboxDbRecord =
    OutboxDbRecord(
        id = id,
        type = type,
        fnr = fnr,
        payload = payload,
        opprettetTimestamp = opprettetTimestamp,
        sendtTimestamp = sendtTimestamp,
    )

fun lagSykmeldingHendelsePayload(
    sykmeldingId: String = "sykmelding-1",
    sykmeldingHendelseId: String = "hendelse-1",
): SykmeldingHendelsePayload =
    SykmeldingHendelsePayload(
        sykmeldingId = sykmeldingId,
        sykmeldingHendelseId = sykmeldingHendelseId,
    )

fun lagSykmeldingNotifikasjon(
    sykmeldingId: String = "sykmelding-1",
    status: SykmeldingNotifikasjonStatus = SykmeldingNotifikasjonStatus.OK,
    mottattDato: LocalDateTime = LocalDateTime.parse("2024-06-01T12:00:00"),
    fnr: String = "fnr-1",
): SykmeldingNotifikasjon =
    SykmeldingNotifikasjon(
        sykmeldingId = sykmeldingId,
        status = status,
        mottattDato = mottattDato,
        fnr = fnr,
    )
