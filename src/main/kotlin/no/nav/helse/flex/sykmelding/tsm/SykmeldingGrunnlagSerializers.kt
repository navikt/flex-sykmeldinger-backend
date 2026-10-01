package no.nav.helse.flex.sykmelding.tsm

import tools.jackson.databind.module.SimpleModule

val SYKMELDING_GRUNNLAG_SERIALIZER =
    SimpleModule().addSerializer(
        AvsenderSystem::class.java,
        AvsenderSystem.AvsenderSystemSerializer(),
    )
