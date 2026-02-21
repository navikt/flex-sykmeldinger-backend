package no.nav.helse.flex.sykmeldingbekreftelse

import no.nav.helse.flex.sykmeldinghendelse.*

object BrukersituasjonKonverterer {
    fun konverterTilBrukersituasjon(
        brukerSvar: BrukerSvar,
        tilleggsinfo: Tilleggsinfo? = null,
    ): BrukersituasjonDto =
        when (brukerSvar) {
            is ArbeidstakerBrukerSvar -> {
                require(tilleggsinfo is ArbeidstakerTilleggsinfo)
                ArbeidstakerDto(
                    arbeidssituasjon = ArbeidstakerArbeidssituasjonDto.ARBEIDSTAKER,
                    arbeidsgiver = konverterArbeidsgiver(tilleggsinfo.arbeidsgiver),
                    harRiktigNarmesteLeder = brukerSvar.riktigNarmesteLeder?.svar ?: true, // TODO: er dette riktig default?
                    egenmeldingsdager = brukerSvar.egenmeldingsdager?.svar ?: emptyList(),
                )
            }

            is NaringsdrivendeBrukerSvar -> {
                NaringsdrivendeDto(
                    arbeidssituasjon = NaringsdrivendeArbeidssituasjonDto.NARINGSDRIVENDE,
                    sykForSykmeldingPerioder = konverterTilSykForSykmeldingPerioder(brukerSvar.egenmeldingsperioder),
                    harForsikringForste16Dager = konverterTilharForsikringForste16Dager(brukerSvar.harForsikring),
                )
            }

            is JordbrukerBrukerSvar -> {
                NaringsdrivendeDto(
                    arbeidssituasjon = NaringsdrivendeArbeidssituasjonDto.JORDBRUKER,
                    sykForSykmeldingPerioder = konverterTilSykForSykmeldingPerioder(brukerSvar.egenmeldingsperioder),
                    harForsikringForste16Dager = konverterTilharForsikringForste16Dager(brukerSvar.harForsikring),
                )
            }

            is FrilanserBrukerSvar -> {
                FrilanserDto(
                    arbeidssituasjon = FrilanserArbeidssituasjonDto.FRILANSER,
                    sykForSykmeldingPerioder = konverterTilSykForSykmeldingPerioder(brukerSvar.egenmeldingsperioder),
                    harForsikringForste16Dager = konverterTilharForsikringForste16Dager(brukerSvar.harForsikring),
                )
            }

            is ArbeidsledigBrukerSvar -> {
                require(tilleggsinfo is ArbeidsledigTilleggsinfo)
                ArbeidsledigDto(
                    arbeidssituasjon = ArbeidsledigArbeidssituasjonDto.ARBEIDSLEDIG,
                    tidligereArbeidsgiver = tilleggsinfo.tidligereArbeidsgiver?.let(::konverterTidligereArbeidsgiver),
                )
            }

            is PermittertBrukerSvar -> {
                require(tilleggsinfo is PermittertTilleggsinfo)
                ArbeidsledigDto(
                    arbeidssituasjon = ArbeidsledigArbeidssituasjonDto.PERMITTERT,
                    tidligereArbeidsgiver = tilleggsinfo.tidligereArbeidsgiver?.let(::konverterTidligereArbeidsgiver),
                )
            }

            is FiskerBrukerSvar -> {
                require(tilleggsinfo is FiskerTilleggsinfo)
                val fiskerSituasjon =
                    FiskerSituasjonDto(
                        blad = konverterFiskerBlad(brukerSvar.blad.svar),
                    )
                when (brukerSvar.lottOgHyre.svar) {
                    FiskerLottOgHyre.HYRE -> {
                        requireNotNull(tilleggsinfo.arbeidsgiver)
                        ArbeidstakerDto(
                            arbeidssituasjon = ArbeidstakerArbeidssituasjonDto.FISKER_HYRE,
                            arbeidsgiver = konverterArbeidsgiver(tilleggsinfo.arbeidsgiver),
                            harRiktigNarmesteLeder = brukerSvar.riktigNarmesteLeder?.svar ?: true, // TODO: er dette riktig default?
                            egenmeldingsdager = brukerSvar.egenmeldingsdager?.svar ?: emptyList(),
                            fiskerSituasjon = fiskerSituasjon,
                        )
                    }

                    FiskerLottOgHyre.LOTT -> {
                        NaringsdrivendeDto(
                            arbeidssituasjon = NaringsdrivendeArbeidssituasjonDto.FISKER_LOTT,
                            sykForSykmeldingPerioder = konverterTilSykForSykmeldingPerioder(brukerSvar.egenmeldingsperioder),
                            harForsikringForste16Dager = konverterTilharForsikringForste16Dager(brukerSvar.harForsikring),
                            fiskerSituasjon = fiskerSituasjon,
                        )
                    }

                    FiskerLottOgHyre.BEGGE -> {
                        requireNotNull(tilleggsinfo.arbeidsgiver)
                        FiskerLottOgHyreDto(
                            arbeidssituasjon = FiskerLottOgHyreArbeidssituasjonDto.FISKER_LOTT_OG_HYRE,
                            arbeidsgiver = konverterArbeidsgiver(tilleggsinfo.arbeidsgiver),
                            harRiktigNarmesteLeder = brukerSvar.riktigNarmesteLeder?.svar ?: true, // TODO: er dette riktig default?
                            egenmeldingsdager = brukerSvar.egenmeldingsdager?.svar ?: emptyList(),
                            fiskerSituasjon = fiskerSituasjon,
                        )
                    }
                }
            }

            is AnnetArbeidssituasjonBrukerSvar -> {
                UkjentYrkesgruppeDto(
                    arbeidssituasjon = UkjentYrkesgruppeArbeidssituasjonDto.ANNET,
                )
            }

            is UtdatertFormatBrukerSvar -> {
                require(tilleggsinfo is UtdatertFormatTilleggsinfo)
                when (val arbeidssituasjon = brukerSvar.arbeidssituasjon.svar) {
                    Arbeidssituasjon.ARBEIDSTAKER -> {
                        if (
                            tilleggsinfo.arbeidsgiver != null &&
                            // TODO: Vi mister 410 sykmeldinger (før 2020-04-20) dersom vi krever juridiskOrgnummer != null.
                            // Alternativt må juridiskOrnummer være nullable
                            tilleggsinfo.arbeidsgiver.juridiskOrgnummer != null
                        ) {
                            ArbeidstakerDto(
                                arbeidssituasjon = ArbeidstakerArbeidssituasjonDto.ARBEIDSTAKER,
                                arbeidsgiver = konverterArbeidsgiver(tilleggsinfo.arbeidsgiver),
                                harRiktigNarmesteLeder = brukerSvar.riktigNarmesteLeder?.svar ?: true,
                                egenmeldingsdager = brukerSvar.egenmeldingsdager?.svar ?: emptyList(),
                            )
                        } else {
                            UkjentYrkesgruppeDto(
                                arbeidssituasjon = UkjentYrkesgruppeArbeidssituasjonDto.UTDATERT,
                                antattArbeidssituasjon = ArbeidstakerArbeidssituasjonDto.ARBEIDSTAKER,
                            )
                        }
                    }

                    Arbeidssituasjon.ARBEIDSLEDIG -> {
                        ArbeidsledigDto(
                            arbeidssituasjon = ArbeidsledigArbeidssituasjonDto.ARBEIDSLEDIG,
                            tidligereArbeidsgiver = tilleggsinfo.tidligereArbeidsgiver?.let(::konverterTidligereArbeidsgiver),
                        )
                    }

                    Arbeidssituasjon.FRILANSER -> {
                        FrilanserDto(
                            arbeidssituasjon = FrilanserArbeidssituasjonDto.FRILANSER,
                            sykForSykmeldingPerioder = konverterTilSykForSykmeldingPerioder(brukerSvar.egenmeldingsperioder),
                            harForsikringForste16Dager = konverterTilharForsikringForste16Dager(brukerSvar.harForsikring),
                        )
                    }

                    Arbeidssituasjon.NAERINGSDRIVENDE -> {
                        NaringsdrivendeDto(
                            arbeidssituasjon = NaringsdrivendeArbeidssituasjonDto.NARINGSDRIVENDE,
                            sykForSykmeldingPerioder = konverterTilSykForSykmeldingPerioder(brukerSvar.egenmeldingsperioder),
                            harForsikringForste16Dager = konverterTilharForsikringForste16Dager(brukerSvar.harForsikring),
                        )
                    }

                    Arbeidssituasjon.ANNET -> {
                        UkjentYrkesgruppeDto(
                            arbeidssituasjon = UkjentYrkesgruppeArbeidssituasjonDto.ANNET,
                        )
                    }

                    Arbeidssituasjon.JORDBRUKER,
                    Arbeidssituasjon.FISKER,
                    Arbeidssituasjon.PERMITTERT,
                    -> {
                        throw IllegalArgumentException("UtdatertFormatBrukerSvar kan ikke ha arbeidssituasjon $arbeidssituasjon")
                    }
                }
            }
        }

    fun konverterArbeidsgiver(arbeidsgiver: Arbeidsgiver): ArbeidsgiverDto =
        ArbeidsgiverDto(
            orgnummer = arbeidsgiver.orgnummer,
            juridiskOrgnummer = arbeidsgiver.juridiskOrgnummer,
            orgnavn = arbeidsgiver.orgnavn,
        )

    fun konverterArbeidsgiver(arbeidsgiver: UtdatertFormatArbeidsgiver): ArbeidsgiverDto =
        ArbeidsgiverDto(
            orgnummer = arbeidsgiver.orgnummer,
            juridiskOrgnummer = requireNotNull(arbeidsgiver.juridiskOrgnummer) { "Må ha juridiskOrgnummer" },
            orgnavn = arbeidsgiver.orgnavn,
        )

    fun konverterTidligereArbeidsgiver(tidligereArbeidsgiver: TidligereArbeidsgiver): TidligereArbeidsgiverDto =
        TidligereArbeidsgiverDto(
            orgnummer = tidligereArbeidsgiver.orgnummer,
            orgnavn = tidligereArbeidsgiver.orgNavn,
        )

    fun konverterTilSykForSykmeldingPerioder(
        egenmeldingsperioderSporsmal: SporsmalSvar<List<Egenmeldingsperiode>>?,
    ): List<SykForSykmeldingPeriodeDto> =
        egenmeldingsperioderSporsmal?.svar?.map {
            SykForSykmeldingPeriodeDto(fom = it.fom, tom = it.tom)
        } ?: emptyList()

    fun konverterTilharForsikringForste16Dager(harForsikringSporsmal: SporsmalSvar<Boolean>?): Boolean =
        harForsikringSporsmal?.svar ?: false

    fun konverterFiskerBlad(fiskerBlad: FiskerBlad): FiskerBladDto =
        when (fiskerBlad) {
            FiskerBlad.A -> FiskerBladDto.A
            FiskerBlad.B -> FiskerBladDto.B
        }
}
