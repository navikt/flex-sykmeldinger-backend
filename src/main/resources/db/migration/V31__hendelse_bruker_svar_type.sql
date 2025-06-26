UPDATE SYKMELDINGHENDELSE
SET bruker_svar = jsonb_build_object(
                          'type', bruker_svar->'arbeidssituasjon',
                          'arbeidssituasjon', bruker_svar->'arbeidssituasjonSporsmal'
                  ) || (bruker_svar - 'arbeidssituasjon' - 'arbeidssituasjonSporsmal')
WHERE bruker_svar IS NOT NULL;