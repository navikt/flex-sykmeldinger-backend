package no.nav.helse.flex.utils

import no.nav.helse.flex.sykmelding.tsm.SYKMELDING_GRUNNLAG_DESERIALIZER_MODULE
import no.nav.helse.flex.sykmelding.tsm.SYKMELDING_GRUNNLAG_SERIALIZER
import no.nav.helse.flex.sykmeldinghendelse.BrukerSvar
import no.nav.helse.flex.sykmeldinghendelse.Tilleggsinfo
import tools.jackson.databind.JsonNode
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

val objectMapper: ObjectMapper =
    JsonMapper
        .builder()
        .addModule(KotlinModule.Builder().build())
        .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
        .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
        .addModule(SYKMELDING_GRUNNLAG_DESERIALIZER_MODULE)
        .addModule(SYKMELDING_GRUNNLAG_SERIALIZER)
        .addModule(BrukerSvar.deserializerModule)
        .addModule(Tilleggsinfo.deserializerModule)
        .build()

fun Any.serialisertTilString(): String = objectMapper.writeValueAsString(this)

fun Any.toJsonNode(): JsonNode = objectMapper.readTree(objectMapper.writeValueAsString(this))
