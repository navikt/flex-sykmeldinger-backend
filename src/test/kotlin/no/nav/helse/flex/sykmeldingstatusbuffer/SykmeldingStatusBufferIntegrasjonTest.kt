package no.nav.helse.flex.sykmeldingstatusbuffer

import no.nav.helse.flex.producers.sykmeldingstatus.SykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testdata.lagKafkaMetadataDTO
import no.nav.helse.flex.testdata.lagSykmeldingStatusKafkaMessageDTO
import org.amshove.kluent.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class SykmeldingStatusBufferIntegrasjonTest : IntegrasjonTestOppsett() {
    @Autowired
    private lateinit var txManager: PlatformTransactionManager

    @Autowired
    private lateinit var sykmeldingStatusBuffer: SykmeldingStatusBuffer

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `prosesserAlleFor burde vente på leggTil dersom transaksjoner kjører samtidig`() {
        val producerTaskCompleteLatch = CountDownLatch(1)
        val producerTransactionWaitLatch = CountDownLatch(1)

        val producerTask =
            Runnable {
                val txTemplate = TransactionTemplate(txManager)
                txTemplate.execute {
                    sykmeldingStatusBuffer.leggTil(
                        lagSykmeldingStatusKafkaMessageDTO(kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1")),
                    )
                    producerTaskCompleteLatch.countDown()
                    producerTransactionWaitLatch.await()
                }
            }

        val consumerTask =
            Callable {
                val txTemplate = TransactionTemplate(txManager)
                txTemplate.execute {
                    sykmeldingStatusBuffer.prosesserAlleFor("1")
                }
            }

        val executor = Executors.newFixedThreadPool(2)

        val producerTaskFuture = executor.submit(producerTask)
        producerTaskCompleteLatch.await()
        val consumerTaskFuture = executor.submit(consumerTask)
        invoking {
            consumerTaskFuture.get(500, TimeUnit.MILLISECONDS)
        }.shouldThrow(TimeoutException::class)

        producerTransactionWaitLatch.countDown()

        producerTaskFuture.get()
        val prosesserteBufferStatuser: List<SykmeldingStatusKafkaMessageDTO> = consumerTaskFuture.get()

        prosesserteBufferStatuser.shouldNotBeNull().shouldHaveSize(1)
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `leggTil burde vente på prosesserAlleFor dersom transaksjoner kjører samtidig`() {
        val consumerTaskCompleteLatch = CountDownLatch(1)
        val consumerTransactionWaitLatch = CountDownLatch(1)

        val consumerTask =
            Callable {
                val txTemplate = TransactionTemplate(txManager)
                txTemplate.execute {
                    sykmeldingStatusBuffer.prosesserAlleFor("1").also {
                        consumerTaskCompleteLatch.countDown()
                        consumerTransactionWaitLatch.await()
                    }
                }
            }

        val producerTask =
            Runnable {
                val txTemplate = TransactionTemplate(txManager)
                txTemplate.execute {
                    sykmeldingStatusBuffer.leggTil(
                        lagSykmeldingStatusKafkaMessageDTO(kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1")),
                    )
                }
            }

        val executor = Executors.newFixedThreadPool(2)

        val consumerTaskFuture = executor.submit(consumerTask)
        consumerTaskCompleteLatch.await()
        val producerTaskFuture = executor.submit(producerTask)
        invoking {
            producerTaskFuture.get(500, TimeUnit.MILLISECONDS)
        }.shouldThrow(TimeoutException::class)

        consumerTransactionWaitLatch.countDown()
        val prosesserteBufferStatuser = consumerTaskFuture.get()
        producerTaskFuture.get()

        prosesserteBufferStatuser.shouldNotBeNull().shouldHaveSize(0)
    }
}
