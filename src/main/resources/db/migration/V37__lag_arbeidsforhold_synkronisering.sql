CREATE TABLE temp_synkroniser_arbeidsforhold (
    fnr TEXT NOT NULL,
    arbeidsforhold_type TEXT,
    nav_arbeidsforhold_id TEXT,
    id TEXT PRIMARY KEY default UUID_GENERATE_V4(),
    lest BOOLEAN NOT NULL DEFAULT FALSE
)