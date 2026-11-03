package no.nav.helse.flex.utils

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue

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
            .defaultStringField shouldBe ""
    }

    @Test
    fun `burde ikke konvertere snake-case`() {
        objectMapper
            .readValue<TestDataClass>("{\"default_string_field\": \"value\"}")
            .defaultStringField shouldBe ""
    }

    enum class TestEnum {
        A,

        @JsonEnumDefaultValue
        DEFAULT,
    }

    @Test
    fun `burde mappe enum verdier`() {
        val value: TestEnum = objectMapper.readValue("\"A\"")
        value shouldBe TestEnum.A
    }

    @Test
    fun `burde konvertere enum lower case`() {
        val value: TestEnum = objectMapper.readValue("\"a\"")
        value shouldBe TestEnum.A
    }

    @Test
    fun `burde håndtere default enum verdi ved annotasjon`() {
        val value: TestEnum = objectMapper.readValue("\"SOMETHING_ELSE\"")
        value shouldBe TestEnum.DEFAULT
    }
}
