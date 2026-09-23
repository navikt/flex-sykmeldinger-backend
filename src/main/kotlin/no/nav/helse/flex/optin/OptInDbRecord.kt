package no.nav.helse.flex.optin

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("opt_in")
data class OptInDbRecord(
    @Id
    val id: Long? = null,
    val sykmeldingId: String,
    val opprettet: Instant,
)
