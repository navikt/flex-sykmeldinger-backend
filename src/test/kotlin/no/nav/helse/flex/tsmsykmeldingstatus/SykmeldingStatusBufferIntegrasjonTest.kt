package no.nav.helse.flex.tsmsykmeldingstatus

import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import org.amshove.kluent.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class SykmeldingStatusBufferIntegrasjonTest : IntegrasjonTestOppsett() {
    @Autowired
    private lateinit var txTemplate: TransactionTemplate

    @Autowired
    private lateinit var sykmeldingStatusBuffer: SykmeldingStatusBuffer

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `burde vente dersom lås er tatt av annen prosess`() {
        val firstTaskComplete = CountDownLatch(1)
        val firstTaskWaitAfterCompletion = CountDownLatch(1)

        val firstTask =
            Runnable {
                txTemplate.execute {
                    sykmeldingStatusBuffer.taLaasFor(sykmeldingId = "1")
                    firstTaskComplete.countDown()
                    firstTaskWaitAfterCompletion.await()
                }
            }

        val secondTask =
            Runnable {
                txTemplate.execute {
                    sykmeldingStatusBuffer.taLaasFor(sykmeldingId = "1")
                }
            }

        val executor = Executors.newFixedThreadPool(2)

        val firstTaskFuture = executor.submit(firstTask)
        firstTaskComplete.await()
        val secondTaskFuture = executor.submit(secondTask)
        invoking {
            secondTaskFuture.get(500, TimeUnit.MILLISECONDS)
        }.shouldThrow(TimeoutException::class)

        firstTaskWaitAfterCompletion.countDown()

        firstTaskFuture.get()
        secondTaskFuture.get()
    }
}
