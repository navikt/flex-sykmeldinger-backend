CREATE TABLE narmeste_leder_esyfo (
    id VARCHAR(36) PRIMARY KEY DEFAULT UUID_GENERATE_V4(),
    oppdatert TIMESTAMP WITH TIME ZONE NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    narmeste_leder_id UUID NOT NULL UNIQUE,
    orgnummer VARCHAR NOT NULL,
    bruker_fnr VARCHAR NOT NULL,
    narmeste_leder_fnr VARCHAR NOT NULL,
    narmeste_leder_navn VARCHAR,
    aktiv_fom DATE NOT NULL,
    aktiv_tom DATE
);

CREATE INDEX narmeste_leder_esyfo_bruker_fnr_idx ON narmeste_leder_esyfo (bruker_fnr);

CREATE TABLE narmeste_leder_esyfo_checkpoint (
    topic VARCHAR NOT NULL,
    partition INTEGER NOT NULL,
    offset_sist_pafort BIGINT NOT NULL DEFAULT -1,
    PRIMARY KEY (topic, partition)
);
