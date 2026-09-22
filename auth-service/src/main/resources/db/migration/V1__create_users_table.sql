CREATE TABLE auth.users (
                            id UUID PRIMARY KEY,
                            username VARCHAR(100) NOT NULL UNIQUE,
                            email VARCHAR(255) NOT NULL UNIQUE,
                            mobile_number VARCHAR(20) UNIQUE,
                            password VARCHAR(255) NOT NULL,
                            first_name VARCHAR(100),
                            last_name VARCHAR(100),
                            enabled BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at TIMESTAMP NOT NULL,
                            updated_at TIMESTAMP NOT NULL
);