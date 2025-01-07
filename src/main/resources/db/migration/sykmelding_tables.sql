-- Drop existing tables if they exist (they were created in V3 but we want to modify them)
DROP TABLE IF EXISTS SYKMELDING CASCADE;
DROP TABLE IF EXISTS SYKMELDINGSTATUS CASCADE;

-- Create table for sykmeldingstatus
CREATE TABLE SYKMELDINGSTATUS
(
    ID                     VARCHAR(36) DEFAULT UUID_GENERATE_V4() PRIMARY KEY,
    SYKMELDING_ID         VARCHAR(64)                NOT NULL,
    TIMESTAMP             TIMESTAMP WITH TIME ZONE   NOT NULL,
    STATUS                VARCHAR(50)                NOT NULL,
    ARBEIDSGIVER          JSONB                     NULL,
    SPORSMAL              JSONB                     NULL,
    OPPRETTET             TIMESTAMP WITH TIME ZONE   NOT NULL
);

CREATE INDEX SYKMELDINGSTATUS_SYKMELDING_ID_IDX ON SYKMELDINGSTATUS (SYKMELDING_ID);

-- Create table for sykmeldinger
CREATE TABLE SYKMELDING
(
    ID                            VARCHAR(36) DEFAULT UUID_GENERATE_V4() PRIMARY KEY,
    SYKMELDING_ID                 VARCHAR(64)                NOT NULL UNIQUE,
    FNR                           VARCHAR(11)                NOT NULL,
    BEHANDLINGSUTFALL            JSONB                      NOT NULL,
    SYKMELDING                   JSONB                      NOT NULL,
    LATEST_STATUS_ID             VARCHAR(36) REFERENCES SYKMELDINGSTATUS (ID),
    OPPRETTET                    TIMESTAMP WITH TIME ZONE   NOT NULL,
    SENDT                        TIMESTAMP WITH TIME ZONE   NULL,
    BEKREFTET                    TIMESTAMP WITH TIME ZONE   NULL,
    UTGATT                       TIMESTAMP WITH TIME ZONE   NULL,
    AVBRUTT                      TIMESTAMP WITH TIME ZONE   NULL
);

CREATE INDEX SYKMELDING_FNR_IDX ON SYKMELDING (FNR);
CREATE INDEX SYKMELDING_SYKMELDING_ID_IDX ON SYKMELDING (SYKMELDING_ID);

-- Grant required permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON SYKMELDING TO cloudsqliamuser;
GRANT SELECT, INSERT, UPDATE, DELETE ON SYKMELDINGSTATUS TO cloudsqliamuser;