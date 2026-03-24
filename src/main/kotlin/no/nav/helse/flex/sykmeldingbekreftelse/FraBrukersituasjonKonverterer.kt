package no.nav.helse.flex.sykmeldingbekreftelse

import no.nav.helse.flex.sykmeldinghendelse.*

object FraBrukersituasjonKonverterer {
    fun konverterTilBrukerSvar(brukersituasjon: BrukersituasjonDto): BrukerSvar =
        when (brukersituasjon) {
            is ArbeidstakerDto -> {
                val erOpplysningeneRiktige = sporsmalSvarUtenTekst(true)
                val arbeidsgiverOrgnummer = sporsmalSvarUtenTekst(brukersituasjon.arbeidsgiver.orgnummer)
                val riktigNarmesteLeder = sporsmalSvarUtenTekst(brukersituasjon.harRiktigNarmesteLeder)
                val harEgenmeldingsdager = sporsmalSvarUtenTekst(brukersituasjon.egenmeldingsdager.isNotEmpty())
                val egenmeldingsdager = sporsmalSvarUtenTekst(brukersituasjon.egenmeldingsdager)
                when (brukersituasjon.arbeidssituasjon) {
                    ArbeidstakerArbeidssituasjonDto.ARBEIDSTAKER -> {
                        ArbeidstakerBrukerSvar(
                            arbeidssituasjon = sporsmalSvarUtenTekst(Arbeidssituasjon.ARBEIDSTAKER),
                            erOpplysningeneRiktige = erOpplysningeneRiktige,
                            arbeidsgiverOrgnummer = arbeidsgiverOrgnummer,
                            riktigNarmesteLeder = riktigNarmesteLeder,
                            harEgenmeldingsdager = harEgenmeldingsdager,
                            egenmeldingsdager = egenmeldingsdager,
                        )
                    }

                    ArbeidstakerArbeidssituasjonDto.FISKER_HYRE -> {
                        requireNotNull(brukersituasjon.fiskerSituasjon)
                        FiskerBrukerSvar(
                            arbeidssituasjon = sporsmalSvarUtenTekst(Arbeidssituasjon.FISKER),
                            lottOgHyre = sporsmalSvarUtenTekst(FiskerLottOgHyre.HYRE),
                            erOpplysningeneRiktige = erOpplysningeneRiktige,
                            blad = sporsmalSvarUtenTekst(konverterFiskerBlad(brukersituasjon.fiskerSituasjon.blad)),
                            arbeidsgiverOrgnummer = arbeidsgiverOrgnummer,
                            riktigNarmesteLeder = riktigNarmesteLeder,
                            harEgenmeldingsdager = harEgenmeldingsdager,
                            egenmeldingsdager = egenmeldingsdager,
                        )
                    }
                }
            }

            is NaringsdrivendeDto -> {
                val erOpplysningeneRiktige = sporsmalSvarUtenTekst(true)
                val harBruktEgenmelding = sporsmalSvarUtenTekst(brukersituasjon.sykForSykmeldingPerioder.isNotEmpty())
                val egenmeldingsperioder = sporsmalSvarUtenTekst(konverterEgenmeldingsperiode(brukersituasjon.sykForSykmeldingPerioder))
                val harForsikring = sporsmalSvarUtenTekst(brukersituasjon.harForsikringForste16Dager)

                when (brukersituasjon.arbeidssituasjon) {
                    NaringsdrivendeArbeidssituasjonDto.NARINGSDRIVENDE -> {
                        NaringsdrivendeBrukerSvar(
                            arbeidssituasjon = sporsmalSvarUtenTekst(Arbeidssituasjon.NAERINGSDRIVENDE),
                            erOpplysningeneRiktige = erOpplysningeneRiktige,
                            harBruktEgenmelding = harBruktEgenmelding,
                            egenmeldingsperioder = egenmeldingsperioder,
                            harForsikring = harForsikring,
                        )
                    }

                    NaringsdrivendeArbeidssituasjonDto.JORDBRUKER -> {
                        JordbrukerBrukerSvar(
                            arbeidssituasjon = sporsmalSvarUtenTekst(Arbeidssituasjon.JORDBRUKER),
                            erOpplysningeneRiktige = erOpplysningeneRiktige,
                            harBruktEgenmelding = harBruktEgenmelding,
                            egenmeldingsperioder = egenmeldingsperioder,
                            harForsikring = harForsikring,
                        )
                    }

                    NaringsdrivendeArbeidssituasjonDto.FISKER_LOTT -> {
                        requireNotNull(brukersituasjon.fiskerSituasjon)
                        FiskerBrukerSvar(
                            arbeidssituasjon = sporsmalSvarUtenTekst(Arbeidssituasjon.FISKER),
                            lottOgHyre = sporsmalSvarUtenTekst(FiskerLottOgHyre.LOTT),
                            blad = sporsmalSvarUtenTekst(konverterFiskerBlad(brukersituasjon.fiskerSituasjon.blad)),
                            erOpplysningeneRiktige = erOpplysningeneRiktige,
                            harBruktEgenmelding = harBruktEgenmelding,
                            egenmeldingsperioder = egenmeldingsperioder,
                            harForsikring = harForsikring,
                        )
                    }
                }
            }

            is FrilanserDto -> {
                val erOpplysningeneRiktige = sporsmalSvarUtenTekst(true)
                val harBruktEgenmelding = sporsmalSvarUtenTekst(brukersituasjon.sykForSykmeldingPerioder.isNotEmpty())
                val egenmeldingsperioder = sporsmalSvarUtenTekst(konverterEgenmeldingsperiode(brukersituasjon.sykForSykmeldingPerioder))
                val harForsikring = brukersituasjon.harForsikringForste16Dager?. let { sporsmalSvarUtenTekst(it) }
                when (brukersituasjon.arbeidssituasjon) {
                    FrilanserArbeidssituasjonDto.FRILANSER -> {
                        FrilanserBrukerSvar(
                            arbeidssituasjon = sporsmalSvarUtenTekst(Arbeidssituasjon.FRILANSER),
                            erOpplysningeneRiktige = erOpplysningeneRiktige,
                            harBruktEgenmelding = harBruktEgenmelding,
                            egenmeldingsperioder = egenmeldingsperioder,
                            harForsikring = harForsikring,
                        )
                    }
                }
            }

            is ArbeidsledigDto -> {
                val erOpplysningeneRiktige = sporsmalSvarUtenTekst(true)
                val arbeidsledigFraOrgnummer = brukersituasjon.tidligereArbeidsgiver?.let { sporsmalSvarUtenTekst(it.orgnummer) }
                when (brukersituasjon.arbeidssituasjon) {
                    ArbeidsledigArbeidssituasjonDto.ARBEIDSLEDIG -> {
                        ArbeidsledigBrukerSvar(
                            arbeidssituasjon = sporsmalSvarUtenTekst(Arbeidssituasjon.ARBEIDSLEDIG),
                            erOpplysningeneRiktige = erOpplysningeneRiktige,
                            arbeidsledigFraOrgnummer = arbeidsledigFraOrgnummer,
                        )
                    }

                    ArbeidsledigArbeidssituasjonDto.PERMITTERT -> {
                        PermittertBrukerSvar(
                            arbeidssituasjon = sporsmalSvarUtenTekst(Arbeidssituasjon.PERMITTERT),
                            erOpplysningeneRiktige = erOpplysningeneRiktige,
                            arbeidsledigFraOrgnummer = arbeidsledigFraOrgnummer,
                        )
                    }
                }
            }

            is FiskerLottOgHyreDto -> {
                when (brukersituasjon.arbeidssituasjon) {
                    FiskerLottOgHyreArbeidssituasjonDto.FISKER_LOTT_OG_HYRE -> {
                        FiskerBrukerSvar(
                            arbeidssituasjon = sporsmalSvarUtenTekst(Arbeidssituasjon.FISKER),
                            lottOgHyre = sporsmalSvarUtenTekst(FiskerLottOgHyre.BEGGE),
                            blad = sporsmalSvarUtenTekst(konverterFiskerBlad(brukersituasjon.fiskerSituasjon.blad)),
                            erOpplysningeneRiktige = sporsmalSvarUtenTekst(true),
                            arbeidsgiverOrgnummer = sporsmalSvarUtenTekst(brukersituasjon.arbeidsgiver.orgnummer),
                            riktigNarmesteLeder = sporsmalSvarUtenTekst(brukersituasjon.harRiktigNarmesteLeder),
                            harEgenmeldingsdager = sporsmalSvarUtenTekst(brukersituasjon.egenmeldingsdager.isNotEmpty()),
                            egenmeldingsdager = sporsmalSvarUtenTekst(brukersituasjon.egenmeldingsdager),
                        )
                    }
                }
            }

            is UkjentYrkesgruppeDto -> {
                when (brukersituasjon.arbeidssituasjon) {
                    UkjentYrkesgruppeArbeidssituasjonDto.ANNET -> {
                        AnnetArbeidssituasjonBrukerSvar(
                            arbeidssituasjon = sporsmalSvarUtenTekst(Arbeidssituasjon.ANNET),
                            erOpplysningeneRiktige = sporsmalSvarUtenTekst(true),
                        )
                    }

                    UkjentYrkesgruppeArbeidssituasjonDto.UTDATERT -> {
                        val skalViStotteHistoriskData = true // TODO: Helst ikke
                        if (skalViStotteHistoriskData) {
                            requireNotNull(brukersituasjon.antattArbeidssituasjon)
                            UtdatertFormatBrukerSvar(
                                erOpplysningeneRiktige = sporsmalSvarUtenTekst(true),
                                arbeidssituasjon = sporsmalSvarUtenTekst(konverterArbeidssituasjon(brukersituasjon.antattArbeidssituasjon)),
                            )
                        } else {
                            throw IllegalArgumentException("Arbeidssituasjon UTDATERT støttes ikke")
                        }
                    }
                }
            }
        }

    fun konverterTilTilleggsinfo(brukersituasjon: BrukersituasjonDto): Tilleggsinfo =
        when (brukersituasjon) {
            is ArbeidstakerDto -> {
                val arbeidsgiver = konverterArbeidsgiver(brukersituasjon.arbeidsgiver)
                when (brukersituasjon.arbeidssituasjon) {
                    ArbeidstakerArbeidssituasjonDto.ARBEIDSTAKER -> {
                        ArbeidstakerTilleggsinfo(arbeidsgiver = arbeidsgiver)
                    }

                    ArbeidstakerArbeidssituasjonDto.FISKER_HYRE -> {
                        FiskerTilleggsinfo(arbeidsgiver = arbeidsgiver)
                    }
                }
            }

            is NaringsdrivendeDto -> {
                when (brukersituasjon.arbeidssituasjon) {
                    NaringsdrivendeArbeidssituasjonDto.NARINGSDRIVENDE -> {
                        NaringsdrivendeTilleggsinfo
                    }

                    NaringsdrivendeArbeidssituasjonDto.JORDBRUKER -> {
                        JordbrukerTilleggsinfo
                    }

                    NaringsdrivendeArbeidssituasjonDto.FISKER_LOTT -> {
                        FiskerTilleggsinfo(arbeidsgiver = null)
                    }
                }
            }

            is FrilanserDto -> {
                when (brukersituasjon.arbeidssituasjon) {
                    FrilanserArbeidssituasjonDto.FRILANSER -> FrilanserTilleggsinfo
                }
            }

            is ArbeidsledigDto -> {
                val tidligereArbeidsgiver = brukersituasjon.tidligereArbeidsgiver?.let { konverterTidligereArbeidsgiver(it) }
                when (brukersituasjon.arbeidssituasjon) {
                    ArbeidsledigArbeidssituasjonDto.ARBEIDSLEDIG -> {
                        ArbeidsledigTilleggsinfo(
                            tidligereArbeidsgiver = tidligereArbeidsgiver,
                        )
                    }

                    ArbeidsledigArbeidssituasjonDto.PERMITTERT -> {
                        PermittertTilleggsinfo(
                            tidligereArbeidsgiver = tidligereArbeidsgiver,
                        )
                    }
                }
            }

            is FiskerLottOgHyreDto -> {
                val arbeidsgiver = konverterArbeidsgiver(brukersituasjon.arbeidsgiver)
                when (brukersituasjon.arbeidssituasjon) {
                    FiskerLottOgHyreArbeidssituasjonDto.FISKER_LOTT_OG_HYRE -> {
                        FiskerTilleggsinfo(arbeidsgiver = arbeidsgiver)
                    }
                }
            }

            is UkjentYrkesgruppeDto -> {
                when (brukersituasjon.arbeidssituasjon) {
                    UkjentYrkesgruppeArbeidssituasjonDto.ANNET -> {
                        AnnetArbeidssituasjonTilleggsinfo
                    }

                    UkjentYrkesgruppeArbeidssituasjonDto.UTDATERT -> {
                        UtdatertFormatTilleggsinfo()
                    }
                }
            }
        }

    fun <T> sporsmalSvarUtenTekst(svar: T): SporsmalSvar<T> =
        SporsmalSvar(
            sporsmaltekst = "",
            svar = svar,
        )

    fun konverterFiskerBlad(bladDto: FiskerBladDto): FiskerBlad =
        when (bladDto) {
            FiskerBladDto.A -> FiskerBlad.A
            FiskerBladDto.B -> FiskerBlad.B
        }

    fun konverterEgenmeldingsperiode(sykForSykmeldingPerioder: List<SykForSykmeldingPeriodeDto>): List<Egenmeldingsperiode> =
        sykForSykmeldingPerioder.map {
            Egenmeldingsperiode(
                fom = it.fom,
                tom = it.tom,
            )
        }

    fun konverterArbeidsgiver(arbeidsgiverDto: ArbeidsgiverDto): Arbeidsgiver =
        Arbeidsgiver(
            orgnummer = arbeidsgiverDto.orgnummer,
            juridiskOrgnummer = arbeidsgiverDto.juridiskOrgnummer,
            orgnavn = arbeidsgiverDto.orgnavn,
            erAktivtArbeidsforhold = true,
            narmesteLeder = null,
        )

    fun konverterTidligereArbeidsgiver(tidligereArbeidsgiverDto: TidligereArbeidsgiverDto): TidligereArbeidsgiver =
        TidligereArbeidsgiver(
            orgNavn = tidligereArbeidsgiverDto.orgnavn,
            orgnummer = tidligereArbeidsgiverDto.orgnummer,
        )

    fun konverterArbeidssituasjon(arbeidssituasjon: ArbeidssituasjonDto): Arbeidssituasjon =
        when (arbeidssituasjon) {
            ArbeidsledigArbeidssituasjonDto.ARBEIDSLEDIG -> Arbeidssituasjon.ARBEIDSLEDIG

            ArbeidsledigArbeidssituasjonDto.PERMITTERT -> Arbeidssituasjon.PERMITTERT

            ArbeidstakerArbeidssituasjonDto.ARBEIDSTAKER -> Arbeidssituasjon.ARBEIDSTAKER

            NaringsdrivendeArbeidssituasjonDto.FISKER_LOTT,
            ArbeidstakerArbeidssituasjonDto.FISKER_HYRE,
            FiskerLottOgHyreArbeidssituasjonDto.FISKER_LOTT_OG_HYRE,
            -> Arbeidssituasjon.FISKER

            FrilanserArbeidssituasjonDto.FRILANSER -> Arbeidssituasjon.FRILANSER

            NaringsdrivendeArbeidssituasjonDto.NARINGSDRIVENDE -> Arbeidssituasjon.NAERINGSDRIVENDE

            NaringsdrivendeArbeidssituasjonDto.JORDBRUKER -> Arbeidssituasjon.JORDBRUKER

            UkjentYrkesgruppeArbeidssituasjonDto.ANNET -> Arbeidssituasjon.ANNET

            UkjentYrkesgruppeArbeidssituasjonDto.UTDATERT -> error("Kan ikke konvertere UTDATERT arbeidssituasjon")
        }
}
