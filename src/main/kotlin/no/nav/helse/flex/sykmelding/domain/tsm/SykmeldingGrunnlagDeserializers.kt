package no.nav.helse.flex.sykmelding.domain.tsm

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlin.reflect.KClass

class SykmeldingDeserializerModule : SimpleModule() {
    init {
        addDeserializer(ISykmeldingGrunnlag::class.java, SykmeldingGrunnlagDeserializer())
        addDeserializer(Aktivitet::class.java, AktivitetDeserializer())
        addDeserializer(ArbeidsgiverInfo::class.java, ArbeidsgiverInfoDeserializer())
        addDeserializer(IArbeid::class.java, IArbeidDeserializer())
        addDeserializer(Rule::class.java, RuleDeserializer())
        addDeserializer(Meldingsinformasjon::class.java, MeldingsinformasjonDeserializer())
    }
}

abstract class ClassSwitchDeserializer<T : Any> : JsonDeserializer<T>() {
    abstract fun getClass(type: String): KClass<out T>

    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): T {
        val node: ObjectNode = p.codec.readTree(p)
        val type = node.get("type").asText()
        val clazz = getClass(type)
        return p.codec.treeToValue(node, clazz.java)
    }
}

class SykmeldingGrunnlagDeserializer : ClassSwitchDeserializer<ISykmeldingGrunnlag>() {
    override fun getClass(type: String): KClass<out ISykmeldingGrunnlag> =
        when (SykmeldingType.valueOf(type)) {
            SykmeldingType.SYKMELDING -> SykmeldingGrunnlag::class
            SykmeldingType.UTENLANDSK_SYKMELDING -> UtenlandskSykmeldingGrunnlag::class
        }
}

class MeldingsinformasjonDeserializer : ClassSwitchDeserializer<Meldingsinformasjon>() {
    override fun getClass(type: String): KClass<out Meldingsinformasjon> =
        when (MetadataType.valueOf(type)) {
            MetadataType.ENKEL -> EmottakEnkel::class
            MetadataType.EMOTTAK -> EDIEmottak::class
            MetadataType.UTENLANDSK_SYKMELDING -> Utenlandsk::class
            MetadataType.PAPIRSYKMELDING -> Papirsykmelding::class
            MetadataType.EGENMELDT -> Egenmeldt::class
        }
}

class RuleDeserializer : ClassSwitchDeserializer<Rule>() {
    override fun getClass(type: String): KClass<out Rule> =
        when (RuleType.valueOf(type)) {
            RuleType.INVALID -> InvalidRule::class
            RuleType.PENDING -> PendingRule::class
            RuleType.OK -> OKRule::class
        }
}

class IArbeidDeserializer : ClassSwitchDeserializer<IArbeid>() {
    override fun getClass(type: String): KClass<out IArbeid> =
        when (IArbeidType.valueOf(type)) {
            IArbeidType.ER_I_ARBEID -> ErIArbeid::class
            IArbeidType.ER_IKKE_I_ARBEID -> ErIkkeIArbeid::class
        }
}

class ArbeidsgiverInfoDeserializer : ClassSwitchDeserializer<ArbeidsgiverInfo>() {
    override fun getClass(type: String): KClass<out ArbeidsgiverInfo> =
        when (ArbeidsgiverType.valueOf(type)) {
            ArbeidsgiverType.EN_ARBEIDSGIVER -> EnArbeidsgiver::class
            ArbeidsgiverType.FLERE_ARBEIDSGIVERE -> FlereArbeidsgivere::class
            ArbeidsgiverType.INGEN_ARBEIDSGIVER -> IngenArbeidsgiver::class
        }
}

class AktivitetDeserializer : ClassSwitchDeserializer<Aktivitet>() {
    override fun getClass(type: String): KClass<out Aktivitet> =
        when (AktivitetType.valueOf(type)) {
            AktivitetType.AKTIVITET_IKKE_MULIG -> AktivitetIkkeMulig::class
            AktivitetType.AVVENTENDE -> Avventende::class
            AktivitetType.BEHANDLINGSDAGER -> Behandlingsdager::class
            AktivitetType.GRADERT -> Gradert::class
            AktivitetType.REISETILSKUDD -> Reisetilskudd::class
        }
}
