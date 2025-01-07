CREATE TABLE sykmelding
(
    id                            VARCHAR DEFAULT uuid_generate_v4() PRIMARY KEY,
    sykmelding_id                 VARCHAR                  NOT NULL UNIQUE,
    fnr                           VARCHAR(11)              NOT NULL,
    sendt_dato                    TIMESTAMP WITH TIME ZONE,
    bekreftet_dato               TIMESTAMP WITH TIME ZONE,
    behandlingsutfall            JSONB                    NOT NULL,
    sykmelding                   JSONB                    NOT NULL,
    opprettet                    TIMESTAMP WITH TIME ZONE NOT NULL,
    utgatt                       TIMESTAMP WITH TIME ZONE,
    avbrutt                      TIMESTAMP WITH TIME ZONE
);

CREATE TABLE sykmeldingstatus
(
    id                     VARCHAR DEFAULT uuid_generate_v4() PRIMARY KEY,
    sykmelding_id         VARCHAR                  NOT NULL,
    timestamp             TIMESTAMP WITH TIME ZONE NOT NULL,
    status                VARCHAR                  NOT NULL,
    arbeidsgiver          JSONB,
    sporsmal              JSONB,
    opprettet             TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX sykmelding_fnr_idx ON sykmelding (fnr);
CREATE INDEX sykmelding_id_idx ON sykmelding (sykmelding_id);
CREATE INDEX sykmeldingstatus_sykmelding_id_idx ON sykmeldingstatus (sykmelding_id);

-- Grant required permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON sykmelding TO cloudsqliamuser;
GRANT SELECT, INSERT, UPDATE, DELETE ON sykmeldingstatus TO cloudsqliamuser;