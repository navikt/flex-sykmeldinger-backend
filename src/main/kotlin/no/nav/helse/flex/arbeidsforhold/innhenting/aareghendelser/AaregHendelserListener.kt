package no.nav.helse.flex.arbeidsforhold.innhenting.aareghendelser

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.arbeidsforhold.innhenting.ArbeidsforholdInnhentingService
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment

//
// import com.fasterxml.jackson.module.kotlin.readValue
// import no.nav.helse.flex.objectMapper
// import org.apache.kafka.clients.consumer.ConsumerRecord
// import org.apache.kafka.clients.consumer.ConsumerRecords
// import org.springframework.kafka.annotation.KafkaListener
// import org.springframework.kafka.support.Acknowledgment
// import kotlin.collections.chunked
// import kotlin.collections.set
//
// class AaregHendelserListener {
//
//    @KafkaListener(
//        topics = [ARBEIDSFORHOLD_TOPIC],
//        containerFactory = "aivenKafkaListenerContainerFactory",
//        properties = ["auto.offset.reset = earliest"],
//    )
//    fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
//        val record = cr.value()
//        val hendelse: ArbeidsforholdHendelse = objectMapper.readValue(record)
//        handleHendelse(hendelse)
//    }
//
//    fun handleHendelse(hendelse: ArbeidsforholdHendelse) {
//        // Handle non-deletion events with valid change types
//        if (hendelse.endringstype != Endringstype.Sletting && hasValidEndringstype(hendelse)) {
//            val fnr = hendelse.arbeidsforhold.arbeidstaker.getFnr()
//            updateArbeidsforholdFor(listOf(fnr))
//        }
//
//        // Handle deletion events
//        if (hendelse.endringstype == Endringstype.Sletting) {
//            val deletedId = listOf(hendelse.arbeidsforhold.navArbeidsforholdId)
//            deleteArbeidsforhold(deletedId)
//            hendelsesTyper["Slettet"] = hendelsesTyper.getOrDefault("Slettet", 0) + 1
//        }
//
//        // Update statistics for entity changes
//        val entitetsendringer = hendelse.entitetsendringer
//        val hendelsesTypesCount = entitetsendringer.groupingBy { it.name }.eachCount()
//
//        for ((type, count) in hendelsesTypesCount) {
//            hendelsesTyper[type] = hendelsesTyper.getOrDefault(type, 0) + count
//        }
//    }
//
//    private fun hasValidEndringstype(arbeidsforholdHendelse: ArbeidsforholdHendelse) =
//        arbeidsforholdHendelse.entitetsendringer.any { endring ->
//            endring == Entitetsendring.Ansettelsesdetaljer ||
//                endring == Entitetsendring.Ansettelsesperiode
//        }
//
//    private fun updateArbeidsforholdFor(newhendelserByFnr: List<String>) {
//        newhendelserByFnr.map { arbeidsforholdService.updateArbeidsforhold(it) }
//    }
//
// }
//

class AaregHendelserListener(
    private val arbeidsforholdRepository: ArbeidsforholdRepository,
    private val arbeidsforholdInnhentingService: ArbeidsforholdInnhentingService,
    @Value("\${AAREG_HENDELSE_TOPIC}") private val aaregHendelseTopic: String,
) {
    val log = logger()

    @KafkaListener(
        topics = ["\$aaregHendelseTopic"],
        containerFactory = "aivenKafkaListenerContainerFactory",
        properties = ["auto.offset.reset = earliest"],
    )
    fun listen(
        cr: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        val record = cr.value()
        val hendelse: ArbeidsforholdHendelse = objectMapper.readValue(record)
        handterHendelse(hendelse)
        acknowledgment.acknowledge()
    }

    fun handterHendelse(hendelse: ArbeidsforholdHendelse) {
        val fnr = hendelse.arbeidsforhold.arbeidstaker.getFnr()
        if (skalSynkroniseres(fnr)) {
            val resultat = arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson(fnr)
            log.info(
                "Opprettet ${resultat.skalOpprettes.count()}. " +
                    "Oppdaterte ${resultat.skalOppdateres.count()}. " +
                    "Slettet ${resultat.skalSlettes.count()}.",
            )
        }
    }

    fun skalSynkroniseres(fnr: String): Boolean {
        return arbeidsforholdRepository.getAllByFnr(fnr).isNotEmpty()
    }
}
