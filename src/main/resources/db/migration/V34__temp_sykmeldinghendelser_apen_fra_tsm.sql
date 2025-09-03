CREATE TABLE IF NOT EXISTS
    "public".temp_sykmeldinghendelser_apen_fra_tsm
(
    id                     varchar(36) DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    sykmelding_id          text                                   NOT NULL,
    status                 text                                   NOT NULL,
    tidligere_arbeidsgiver jsonb,
    bruker_svar            jsonb,
    tilleggsinfo           jsonb,
    hendelse_opprettet     timestamp WITH time zone               NOT NULL,
    lokalt_opprettet       timestamp WITH time zone               NOT NULL,
    SOURCE                 varchar(256)
);