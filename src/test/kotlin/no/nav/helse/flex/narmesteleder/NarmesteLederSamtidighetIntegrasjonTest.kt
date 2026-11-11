package no.nav.helse.flex.narmesteleder

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.utils.serialisertTilString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.util.UUID

class NarmesteLederSamtidighetIntegrasjonTest : IntegrasjonTestOppsett() {
    @Autowired lateinit var oppdatering: OppdateringAvNarmesteLeder

    @Autowired lateinit var transactionManager: PlatformTransactionManager

    @AfterEach
    fun afterEach() {
        narmesteLederRepository.deleteAll()
    }

    @Test
    fun `Meldinger fra team sykmelding sletter avkreftet rad uten tidsstempelfiltrering`() {
        val melding = lagNarmesteLederLeesah(UUID.randomUUID())
        val transaksjon = TransactionTemplate(transactionManager)
        transaksjon.executeWithoutResult { oppdatering.behandleMeldingFraKafka(melding.serialisertTilString()) }
        narmesteLederRepository.findByNarmesteLederId(melding.narmesteLederId).shouldNotBeNull()

        val avkreftet = melding.copy(aktivTom = LocalDate.now(), timestamp = melding.timestamp.minusSeconds(1))
        transaksjon.executeWithoutResult { oppdatering.behandleMeldingFraKafka(avkreftet.serialisertTilString()) }

        narmesteLederRepository.findByNarmesteLederId(melding.narmesteLederId).shouldBeNull()
        narmesteLederRepository.count() shouldBe 0
        transaksjon.executeWithoutResult { oppdatering.behandleMeldingFraKafka(avkreftet.serialisertTilString()) }
        narmesteLederRepository.count() shouldBe 0
    }

    @Test
    fun `Meldinger fra team sykmelding oppdaterer aktiv rad også ved eldre tidsstempel`() {
        val melding = lagNarmesteLederLeesah(UUID.randomUUID())
        val transaksjon = TransactionTemplate(transactionManager)
        transaksjon.executeWithoutResult { oppdatering.behandleMeldingFraKafka(melding.serialisertTilString()) }
        val opprinnelig = narmesteLederRepository.findByNarmesteLederId(melding.narmesteLederId).shouldNotBeNull()

        val eldre = melding.copy(orgnummer = "888888", timestamp = melding.timestamp.minusSeconds(1))
        transaksjon.executeWithoutResult { oppdatering.behandleMeldingFraKafka(eldre.serialisertTilString()) }

        val oppdatert = narmesteLederRepository.findByNarmesteLederId(melding.narmesteLederId).shouldNotBeNull()
        oppdatert.id shouldBe opprinnelig.id
        oppdatert.orgnummer shouldBe "888888"
    }
}
