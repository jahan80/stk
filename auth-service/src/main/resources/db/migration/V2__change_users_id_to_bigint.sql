CREATE SEQUENCE auth.users_id_seq
    START WITH 1
    INCREMENT BY 1;

ALTER TABLE auth.users
    ADD COLUMN id_new BIGINT;

UPDATE auth.users
SET id_new = nextval('auth.users_id_seq');

ALTER SEQUENCE auth.users_id_seq
    OWNED BY auth.users.id_new;

ALTER TABLE auth.users
    ALTER COLUMN id_new SET NOT NULL;

ALTER TABLE auth.users
DROP CONSTRAINT users_pkey;

ALTER TABLE auth.users
DROP COLUMN id;

ALTER TABLE auth.users
    RENAME COLUMN id_new TO id;

ALTER SEQUENCE auth.users_id_seq
    OWNED BY auth.users.id;

ALTER TABLE auth.users
    ALTER COLUMN id SET DEFAULT nextval('auth.users_id_seq');

ALTER TABLE auth.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);