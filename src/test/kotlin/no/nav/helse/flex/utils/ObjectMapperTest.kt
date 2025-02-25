package no.nav.helse.flex.utils

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.module.kotlin.readValue
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test

class ObjectMapperTest {
    data class TestDataClass(
        val defaultStringField: String = "",
    )

    @Test
    fun `burde ignorere ukjente verdier`() {
        objectMapper.readValue<TestDataClass>("{\"ukjentVerdi\": 1}")
    }

    @Test
    fun `burde bruke default verdier ved manglende felt`() {
        objectMapper
            .readValue<TestDataClass>("{}")
            .defaultStringField `should be equal to` ""
    }

    @Test
    fun `burde ikke konvertere snake-case`() {
        objectMapper
            .readValue<TestDataClass>("{\"default_string_field\": \"value\"}")
            .defaultStringField `should be equal to` ""
    }

    enum class TestEnum {
        A,

        @JsonEnumDefaultValue
        DEFAULT,
    }

    @Test
    fun `burde mappe enum verdier`() {
        val value: TestEnum = objectMapper.readValue("\"A\"")
        value `should be equal to` TestEnum.A
    }

    @Test
    fun `burde konvertere enum lower case`() {
        val value: TestEnum = objectMapper.readValue("\"a\"")
        value `should be equal to` TestEnum.A
    }

    @Test
    fun `burde håndtere default enum verdi ved annotasjon`() {
        val value: TestEnum = objectMapper.readValue("\"SOMETHING_ELSE\"")
        value `should be equal to` TestEnum.DEFAULT
    }
}
