UPDATE SYKMELDINGHENDELSE
SET tilleggsinfo =
    jsonb_build_object(
      'type', tilleggsinfo->'arbeidssituasjon'
    ) || (tilleggsinfo - 'arbeidssituasjon')
WHERE tilleggsinfo IS NOT NULL;