package no.nav.helse.flex.utils

import com.fasterxml.jackson.module.kotlin.readValue
import org.postgresql.util.PGobject

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
