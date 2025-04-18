package no.nav.helse.flex.sykmelding.domain.tsm

import com.fasterxml.jackson.databind.module.SimpleModule
import no.nav.helse.flex.utils.addPolymorphicDeserializer

val SYKMELDING_GRUNNLAG_DESERIALIZER_MODULE =
    SimpleModule()
        .addPolymorphicDeserializer(ISykmeldingGrunnlag::type) {
            when (it) {
                SykmeldingType.SYKMELDING -> SykmeldingGrunnlag::class
                SykmeldingType.UTENLANDSK_SYKMELDING -> UtenlandskSykmeldingGrunnlag::class
            }
        }.addPolymorphicDeserializer(Aktivitet::type) {
            when (it) {
                AktivitetType.AKTIVITET_IKKE_MULIG -> AktivitetIkkeMulig::class
                AktivitetType.AVVENTENDE -> Avventende::class
                AktivitetType.BEHANDLINGSDAGER -> Behandlingsdager::class
                AktivitetType.GRADERT -> Gradert::class
                AktivitetType.REISETILSKUDD -> Reisetilskudd::class
            }
        }.addPolymorphicDeserializer(ArbeidsgiverInfo::type) {
            when (it) {
                ArbeidsgiverType.EN_ARBEIDSGIVER -> EnArbeidsgiver::class
                ArbeidsgiverType.FLERE_ARBEIDSGIVERE -> FlereArbeidsgivere::class
                ArbeidsgiverType.INGEN_ARBEIDSGIVER -> IngenArbeidsgiver::class
            }
        }.addPolymorphicDeserializer(IArbeid::type) {
            when (it) {
                IArbeidType.ER_I_ARBEID -> ErIArbeid::class
                IArbeidType.ER_IKKE_I_ARBEID -> ErIkkeIArbeid::class
            }
        }.addPolymorphicDeserializer(Rule::type) {
            when (it) {
                RuleType.INVALID -> InvalidRule::class
                RuleType.PENDING -> PendingRule::class
                RuleType.OK -> OKRule::class
            }
        }.addPolymorphicDeserializer(Meldingsinformasjon::type) {
            when (it) {
                MetadataType.ENKEL -> EmottakEnkel::class
                MetadataType.EMOTTAK -> EDIEmottak::class
                MetadataType.UTENLANDSK_SYKMELDING -> Utenlandsk::class
                MetadataType.PAPIRSYKMELDING -> Papirsykmelding::class
                MetadataType.EGENMELDT -> Egenmeldt::class
            }
        }
