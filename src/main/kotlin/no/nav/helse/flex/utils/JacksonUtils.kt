package no.nav.helse.flex.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
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
) : JsonDeserializer<T>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): T {
        val node: ObjectNode = p.codec.readTree(p)
        val typeNode: JsonNode? = node.get(typeField)

        val type: String =
            if (typeNode != null && !typeNode.isNull) {
                typeNode.asText()
            } else {
                throw IllegalArgumentException("JSON is missing the required '$typeField' field or its value is null.")
            }

        if (type.isEmpty()) {
            throw IllegalArgumentException("The '$typeField' field in JSON is empty.")
        }

        val clazz = getClass(type)
        return p.codec.treeToValue(node, clazz.java)
    }
}
