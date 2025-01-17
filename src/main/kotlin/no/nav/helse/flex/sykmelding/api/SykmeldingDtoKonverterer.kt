package no.nav.helse.flex.sykmelding.api

import no.nav.helse.flex.sykmelding.api.dto.PasientDTO
import no.nav.helse.flex.sykmelding.api.dto.SykmeldingDTO
import no.nav.helse.flex.sykmelding.domain.Pasient
import no.nav.helse.flex.sykmelding.domain.Sykmelding

class SykmeldingDtoKonverterer {
    fun konverter(sykmelding: Sykmelding): SykmeldingDTO =
        SykmeldingDTO(
            id = sykmelding.sykmeldingId,
            pasient = konverterPasient(sykmelding.sykmeldingGrunnlag.pasient),
            mottattTidspunkt = TODO(),
            behandlingsutfall = TODO(),
            legekontorOrgnummer = TODO(),
            arbeidsgiver = TODO(),
            sykmeldingsperioder = TODO(),
            sykmeldingStatus = TODO(),
            medisinskVurdering = TODO(),
            skjermesForPasient = TODO(),
            prognose = TODO(),
            utdypendeOpplysninger = TODO(),
            tiltakArbeidsplassen = TODO(),
            tiltakNAV = TODO(),
            andreTiltak = TODO(),
            meldingTilNAV = TODO(),
            meldingTilArbeidsgiver = TODO(),
            kontaktMedPasient = TODO(),
            behandletTidspunkt = TODO(),
            behandler = TODO(),
            syketilfelleStartDato = TODO(),
            navnFastlege = TODO(),
            egenmeldt = TODO(),
            papirsykmelding = TODO(),
            harRedusertArbeidsgiverperiode = TODO(),
            merknader = TODO(),
            rulesetVersion = TODO(),
            utenlandskSykmelding = TODO(),
        )

    internal fun konverterPasient(pasient: Pasient): PasientDTO =
        PasientDTO(
            fnr = pasient.fnr,
            fornavn = pasient.navn?.fornavn,
            mellomnavn = pasient.navn?.mellomnavn,
            etternavn = pasient.navn?.etternavn,
            overSyttiAar = null,
        )
}
