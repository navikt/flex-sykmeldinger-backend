package no.nav.helse.flex.outbox

import no.nav.helse.flex.utils.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class OutboxJob(
    private val outboxService: OutboxService,
) {
    val log = logger()

    @Scheduled(initialDelay = 2_000, fixedDelay = 100, timeUnit = TimeUnit.MILLISECONDS)
    fun kjørJobb() {
        try {
            outboxService.sendUsendteForEldsteLedigeFnr()
        } catch (e: Exception) {
            log.error("Feil under kjøring av OutboxJob", e)
        }
    }
}
