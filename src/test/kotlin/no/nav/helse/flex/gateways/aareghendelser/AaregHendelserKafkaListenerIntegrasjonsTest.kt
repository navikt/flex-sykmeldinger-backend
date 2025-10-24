package no.nav.helse.flex.gateways.aareghendelser

import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversiktResponse
import no.nav.helse.flex.gateways.EKSEMPEL_RESPONSE_FRA_EREG
import no.nav.helse.flex.gateways.pdl.lagGetPersonResponseData
import no.nav.helse.flex.gateways.pdl.lagGraphQlResponse
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testconfig.MockWebServereConfig.Companion.pdlMockWebServer
import no.nav.helse.flex.testconfig.defaultAaregDispatcher
import no.nav.helse.flex.testconfig.defaultEregDispatcher
import no.nav.helse.flex.testconfig.simpleDispatcher
import no.nav.helse.flex.testdata.lagPasient
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import no.nav.helse.flex.utils.serialisertTilString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldThrow
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.time.Duration

class AaregHendelserKafkaListenerIntegrasjonsTest : IntegrasjonTestOppsett() {
    @Value("\${AAREG_HENDELSE_TOPIC}")
    lateinit var aaregTopic: String

    @Autowired
    private lateinit var aaregMockWebServer: MockWebServer

    @Autowired
    private lateinit var eregMockWebServer: MockWebServer

    @Autowired
    private lateinit var aaregHendelserConsumer: AaregHendelserConsumer

    @BeforeAll
    fun beforeAll() {
        aaregMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .setBody(
                        lagArbeidsforholdOversiktResponse().serialisertTilString(),
                    ).addHeader("Content-Type", "application/json")
            }

        eregMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .setBody(EKSEMPEL_RESPONSE_FRA_EREG.serialisertTilString())
                    .addHeader("Content-Type", "application/json")
            }
    }

    @AfterAll
    fun afterAll() {
        aaregMockWebServer.dispatcher = defaultAaregDispatcher
        eregMockWebServer.dispatcher = defaultEregDispatcher
        pdlMockWebServer.dispatcher = defaultAaregDispatcher
    }

    @AfterEach
    fun afterEach() {
        slettDatabase()
    }

    @Test
    fun `burde lese arbeidsforhold hendelse, og lagre endret arbeidsforhold fra aareg + ereg`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(pasient = lagPasient(fnr = "fnr"))))
        pdlMockWebServer.dispatcher =
            simpleDispatcher { _ ->
                lagGraphQlResponse(lagGetPersonResponseData())
            }

        val record: ProducerRecord<String, String> =
            ProducerRecord(
                aaregTopic,
                null,
                "key",
                lagArbeidsforholdHendelse(fnr = "fnr").serialisertTilString(),
            )
        kafkaProducer.send(record).get()

        await().atMost(Duration.ofSeconds(20)).until {
            arbeidsforholdRepository.findAll().count() >= 1
        }
    }

    @Test
    fun `burde ignorere hendelse uten person ident, pga midlertidig Aareg bug`() {
        val aaregHendelseUtenArbeidstaker =
            """
            {
              "id": 0,
              "endringstype": "Sletting",
              "entitetsendringer": [
                "Permittering",
                "Ansettelsesdetaljer",
                "Permisjon",
                "Ansettelsesperiode"
              ],
              "arbeidsforhold": {
                "navArbeidsforholdId": 1,
                "navUuid": "test",
                "type": {
                  "kode": "ordinaertArbeidsforhold",
                  "beskrivelse": "Ordinært arbeidsforhold"
                },
                "arbeidstaker": {},
                "arbeidssted": {
                  "type": "Underenhet",
                  "identer": [
                    {
                      "type": "ORGANISASJONSNUMMER",
                      "ident": "org-nummer",
                      "gjeldende": true
                    }
                  ]
                },
                "opplysningspliktig": {
                  "type": "Hovedenhet",
                  "identer": [
                    {
                      "type": "ORGANISASJONSNUMMER",
                      "ident": "org-nummer",
                      "gjeldende": true
                    }
                  ]
                }
              },
              "tidsstempel": "2025-05-21T00:00:00"
            }
            """.trimIndent()

        aaregHendelserConsumer.handterHendelser(
            listOf(
                RawHendelse(value = aaregHendelseUtenArbeidstaker),
            ),
        )

        arbeidsforholdRepository.count() `shouldBeEqualTo` 0
    }

    @Test
    fun `burde kaste feil ved annen deserialisering feil, pga midlertidig Aareg bug`() {
        val aaregHendelseUtenArbeidstaker = "{}"

        invoking {
            aaregHendelserConsumer.handterHendelser(
                listOf(
                    RawHendelse(value = aaregHendelseUtenArbeidstaker),
                ),
            )
        }.shouldThrow(RuntimeException::class)
    }
}
