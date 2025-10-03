package no.nav.helse.flex.sykmelding.tsm

import com.fasterxml.jackson.databind.module.SimpleModule

val SYKMELDING_GRUNNLAG_SERIALIZER =
    SimpleModule().addSerializer(
        AvsenderSystem::class.java,
        AvsenderSystem.AvsenderSystemSerializer(),
    )
