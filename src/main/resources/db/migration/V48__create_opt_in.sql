CREATE TABLE opt_in
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sykmelding_id TEXT                     NOT NULL REFERENCES sykmelding (sykmelding_id) ON DELETE CASCADE,
    opprettet     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX OPT_IN_SYKMELDING_ID_IDX ON opt_in (sykmelding_id);
