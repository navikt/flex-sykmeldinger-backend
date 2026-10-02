package no.nav.helse.flex.utils

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.module.SimpleModule
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

inline fun <reified T : Any, reified E : Enum<E>> SimpleModule.addPolymorphicDeserializer(
    switchProp: KProperty1<T, E>,
    crossinline classExtractor: (enum: E) -> KClass<out T>,
): SimpleModule {
    val deserializer =
        ClassSwitchDeserializer(
            typeField = switchProp.name,
        ) { type ->
            val enumVal = enumValueOf<E>(type)
            classExtractor(enumVal)
        }
    this.addDeserializer(T::class.java, deserializer)

    return this
}

class ClassSwitchDeserializer<T : Any>(
    private val typeField: String = "type",
    private val getClass: (type: String) -> KClass<out T>,
) : ValueDeserializer<T>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): T {
        val node: JsonNode = ctxt.readTree(p)
        val typeNode: JsonNode? = node.get(typeField)

        val type: String =
            if (typeNode != null && !typeNode.isNull) {
                typeNode.asString()
            } else {
                throw IllegalArgumentException("JSON is missing the required '$typeField' field or its value is null.")
            }

        if (type.isEmpty()) {
            throw IllegalArgumentException("The '$typeField' field in JSON is empty.")
        }

        val clazz = getClass(type)
        return ctxt.readTreeAsValue(node, clazz.java)
    }
}
