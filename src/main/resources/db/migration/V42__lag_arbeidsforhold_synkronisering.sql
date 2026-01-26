CREATE TABLE temp_synkroniser_arbeidsforhold (
    fnr TEXT NOT NULL,
    id TEXT PRIMARY KEY default UUID_GENERATE_V4(),
    lest BOOLEAN NOT NULL DEFAULT FALSE
)