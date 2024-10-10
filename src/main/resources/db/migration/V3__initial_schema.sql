CREATE TABLE ARBEIDSFORHOLD
(
    ID                 VARCHAR(36) DEFAULT UUID_GENERATE_V4() PRIMARY KEY,
    ARBEIDSFORHOLD_ID  TEXT                     NOT NULL UNIQUE,
    FNR                TEXT                     NOT NULL,
    ORGNUMMER          TEXT                     NOT NULL,
    JURIDISK_ORGNUMMER TEXT                     NOT NULL,
    ORGNAVN            TEXT                     NOT NULL,
    FOM                DATE                     NOT NULL,
    TOM                DATE                     NULL,
    OPPRETTET          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ARBEIDSFORHOLD_FNR_IDX ON ARBEIDSFORHOLD (FNR);


CREATE TABLE SYKMELDINGSTATUS
(
    ID                     VARCHAR(36) DEFAULT UUID_GENERATE_V4() PRIMARY KEY,
    SYKMELDING_UUID        TEXT                     NOT NULL,
    TIMESTAMP              TIMESTAMP WITH TIME ZONE NOT NULL,
    STATUS                 TEXT                     NOT NULL,
    TIDLIGERE_ARBEIDSGIVER JSONB                    NULL,
    SPORSMAL               JSONB                    NULL,
    OPPRETTET              TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE SYKMELDING
(
    ID                        VARCHAR(36) DEFAULT UUID_GENERATE_V4() PRIMARY KEY,
    SYKMELDING_UUID           TEXT                                  NOT NULL UNIQUE,
    SISTE_SYKMELDINGSTATUS_ID TEXT REFERENCES SYKMELDINGSTATUS (ID) NOT NULL,
    FNR                       TEXT                                  NOT NULL,
    SYKMELDING                JSONB                                 NOT NULL,
    PERSON                    JSONB                                 NOT NULL,
    OPPRETTET                 TIMESTAMP WITH TIME ZONE              NOT NULL,
    OPPDATERT                 TIMESTAMP WITH TIME ZONE              NULL
);

CREATE INDEX SYKMELDING_FNR_IDX ON SYKMELDING (FNR);
