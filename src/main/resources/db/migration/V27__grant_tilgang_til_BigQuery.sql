DO
$$
BEGIN
        IF EXISTS
            (SELECT 1 FROM pg_user where usename = 'bigquery-dataprodukt')
        THEN
            GRANT SELECT ON ALL TABLES IN SCHEMA public TO "bigquery-dataprodukt";
        END IF;
END
$$;