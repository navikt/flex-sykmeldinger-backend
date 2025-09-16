CREATE TABLE IF NOT EXISTS
    "public".temp_resterende_sykmeldingstatuser_fra_tsm
(
    id                     varchar(36) DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    sykmelding_id          text                                   NOT NULL,
    event                  text                                   NOT NULL,
    timestamp              timestamp WITH time zone               NOT NULL,
    bruker_svar            jsonb,
    sporsmal_liste         jsonb,
    arbeidsgiver           jsonb,
    tidligere_arbeidsgiver jsonb,
    lokalt_opprettet       timestamp WITH time zone               NOT NULL,
    source                 varchar(256)
);