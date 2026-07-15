package no.nav.helse.flex.outbox

import no.nav.helse.flex.gateways.SykmeldingNotifikasjon
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.support.GenericApplicationContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class OutboxJobShutdownTest {
    private fun shutdownEvent() = ContextClosedEvent(GenericApplicationContext())

    @Test
    fun `kjørJobb avslutter loopen når shutdown signaleres selv om det alltid er mer å prosessere`() {
        val service = KontrollerbarOutboxService(returverdi = true)
        val job = OutboxJob(service)

        val jobbTraad = thread { job.kjørJobb() }

        ventTil(sekunder = 5) { service.antallKall.get() >= 3 }

        job.onShutdownEvent(shutdownEvent())

        jobbTraad.join(5_000)
        jobbTraad.isAlive shouldBeEqualTo false

        val kallVedShutdown = service.antallKall.get()
        Thread.sleep(200)
        service.antallKall.get() shouldBeEqualTo kallVedShutdown
    }

    @Test
    fun `onShutdownEvent venter til pågående jobb er ferdig før den fullfører`() {
        val service = KontrollerbarOutboxService(returverdi = true, blokkerFørsteKall = true)
        val job = OutboxJob(service)

        thread { job.kjørJobb() }
        service.førsteKallStartet.await(5, TimeUnit.SECONDS) shouldBeEqualTo true

        val shutdownFullført = CountDownLatch(1)
        val shutdownTraad =
            thread {
                job.onShutdownEvent(shutdownEvent())
                shutdownFullført.countDown()
            }

        shutdownFullført.await(500, TimeUnit.MILLISECONDS) shouldBeEqualTo false

        service.slippFørsteKall.countDown()
        shutdownFullført.await(5, TimeUnit.SECONDS) shouldBeEqualTo true
        shutdownTraad.join(1_000)
    }

    private fun ventTil(
        sekunder: Long,
        betingelse: () -> Boolean,
    ) {
        val frist = System.nanoTime() + TimeUnit.SECONDS.toNanos(sekunder)
        while (System.nanoTime() < frist) {
            if (betingelse()) return
            Thread.sleep(10)
        }
        throw AssertionError("Betingelsen ble ikke oppfylt innen $sekunder sekunder")
    }
}

private class KontrollerbarOutboxService(
    private val returverdi: Boolean,
    private val blokkerFørsteKall: Boolean = false,
) : OutboxService {
    val antallKall = AtomicInteger(0)
    val førsteKallStartet = CountDownLatch(1)
    val slippFørsteKall = CountDownLatch(1)

    override fun sendUsendteForEldsteLedigeFnr(): Boolean {
        val kallnummer = antallKall.incrementAndGet()
        if (blokkerFørsteKall && kallnummer == 1) {
            førsteKallStartet.countDown()
            slippFørsteKall.await(10, TimeUnit.SECONDS)
        }
        return returverdi
    }

    override fun outboxSykmeldingHendelse(
        fnr: String,
        sykmeldingId: String,
        sykmeldingHendelseId: String,
    ) = error("Ikke i bruk i denne testen")

    override fun outboxSykmeldingBrukernotifikasjon(sykmeldingNotifikasjon: SykmeldingNotifikasjon) = error("Ikke i bruk i denne testen")
}
