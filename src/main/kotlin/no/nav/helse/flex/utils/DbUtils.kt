package no.nav.helse.flex.utils

import org.postgresql.util.PGobject
import tools.jackson.module.kotlin.readValue

fun Any.tilPsqlJson(): PGobject {
    val pgObject = PGobject()
    pgObject.type = "json"
    pgObject.value = this.serialisertTilString()
    return pgObject
}

inline fun <reified T> PGobject.fraPsqlJson(): T? {
    val json: String = this.value ?: return null
    return objectMapper.readValue(json)
}
