package no.nav.helse.flex.config

import no.nav.helse.flex.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.availability.ApplicationAvailability
import org.springframework.boot.availability.LivenessState
import org.springframework.boot.availability.ReadinessState
import org.springframework.http.HttpStatusCode
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import java.net.InetAddress

interface LeaderElection {
    fun isLeader(): Boolean
}

@Component
class KubernetesLeaderElection(
    private val leaderElectionRestClient: RestClient,
    @param:Value("\${elector.get_url}") private val electorPath: String,
    private val applicationAvailability: ApplicationAvailability,
) : LeaderElection {
    val log = logger()

    @Retryable(retryFor = [ResourceAccessException::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
    override fun isLeader(): Boolean {
        if (applicationAvailability.readinessState == ReadinessState.REFUSING_TRAFFIC ||
            applicationAvailability.livenessState == LivenessState.BROKEN
        ) {
            log.info(
                "Ser ikke etter leader med readiness [ ${applicationAvailability.readinessState} ] og " +
                    "liveness [ ${applicationAvailability.livenessState} ]",
            )
            return false
        }

        if (electorPath == "dont_look_for_leader") {
            log.info("Ser ikke etter leader, returnerer at jeg er leader")
            return true
        }

        return kallElector()
    }

    private fun kallElector(): Boolean {
        val hostname: String = InetAddress.getLocalHost().hostName

        val response =
            leaderElectionRestClient
                .get()
                .retrieve()
                .onStatus(HttpStatusCode::isError) { _, res ->
                    throw RuntimeException("Kall mot elector feiler med HTTP-" + res.statusCode).also {
                        log.error(it.message)
                    }
                }.toEntity<Leader>()
        return when (val leader = response.body) {
            null -> {
                throw RuntimeException("Kall mot elector returnerer ikke data, status code: ${response.statusCode}")
                    .also { log.error(it.message) }
            }
            else -> leader.name == hostname
        }
    }

    private data class Leader(
        val name: String,
    )
}
