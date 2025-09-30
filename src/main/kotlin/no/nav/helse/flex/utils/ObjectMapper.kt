package no.nav.helse.flex.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.helse.flex.sykmelding.application.BrukerSvar
import no.nav.helse.flex.sykmelding.domain.Tilleggsinfo
import no.nav.helse.flex.sykmelding.tsm.SYKMELDING_GRUNNLAG_DESERIALIZER_MODULE
import no.nav.helse.flex.sykmelding.tsm.SYKMELDING_GRUNNLAG_SERIALIZER

val objectMapper: ObjectMapper =
    JsonMapper
        .builder()
        .addModule(JavaTimeModule())
        .addModule(KotlinModule.Builder().build())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
        .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true)
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
        .addModule(SYKMELDING_GRUNNLAG_DESERIALIZER_MODULE)
        .addModule(SYKMELDING_GRUNNLAG_SERIALIZER)
        .addModule(BrukerSvar.deserializerModule)
        .addModule(Tilleggsinfo.deserializerModule)
        .build()

fun Any.serialisertTilString(): String = objectMapper.writeValueAsString(this)

fun Any.toJsonNode(): JsonNode = objectMapper.readTree(objectMapper.writeValueAsString(this))
