package no.nav.helse.flex.unleash

import io.getunleash.Unleash
import io.getunleash.UnleashContext
import org.springframework.stereotype.Component

const val UNLEASH_CONTEXT_BEGRENS_NAV_DAGER = "flex-sykmeldinger-backend-begrens-nav-dager"

@Component
class UnleashToggles(
    private val unleash: Unleash,
) {
    fun begrensNavDagerEnabled(fnr: String): Boolean =
        unleash.isEnabled(
            UNLEASH_CONTEXT_BEGRENS_NAV_DAGER,
            UnleashContext.builder().userId(fnr).build(),
        )
}
