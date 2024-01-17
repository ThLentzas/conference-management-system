CREATE TYPE role_type AS ENUM (
    'ROLE_AUTHOR',
    'ROLE_REVIEWER',
    'ROLE_PC_CHAIR'
);

CREATE TABLE IF NOT EXISTS roles (
    id   SERIAL,
    type role_type NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS users (
    id        SERIAL,
    username  VARCHAR(20) UNIQUE NOT NULL,
    password  TEXT               NOT NULL,
    full_name VARCHAR(50)        NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT unique_users_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS users_roles (
    user_id INTEGER NOT NULL,
    role_id INTEGER NOT NULl,
    CONSTRAINT pk_users_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_users_roles_users_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_users_roles_roles_id FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);