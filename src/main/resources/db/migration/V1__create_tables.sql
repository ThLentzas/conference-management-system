CREATE TYPE role_type AS ENUM (
    'AUTHOR',
    'PC_MEMBER',
    'PC_CHAIR'
);

CREATE TABLE IF NOT EXISTS roles (
    id   SERIAL,
    type role_type NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS users (
    id       SERIAL,
    username VARCHAR(20) NOT NULL,
    password TEXT        NOT NULL,
    fullName VARCHAR(50) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT unique_users_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS users_roles (
    users_id INTEGER NOT NULL,
    roles_id INTEGER NOT NULl,
    CONSTRAINT pk_users_roles PRIMARY KEY (users_id, roles_id),
    CONSTRAINT fk_users_roles_users FOREIGN KEY (users_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_users_roles_roles FOREIGN KEY (roles_id) REFERENCES roles (id) ON DELETE CASCADE
);

CREATE TYPE paper_state AS ENUM (
    'CREATED',
    'SUBMITTED',
    'REVIEWED',
    'REJECTED',
    'APPROVED',
    'ACCEPTED'
);

CREATE TABLE IF NOT EXISTS papers (
    id SERIAL,
    created_date DATE NOT NULL,
    title VARCHAR(50) NOT NULL,
    abstract_text TEXT NOT NULL,
    state paper_state NOT NULL,
    fileName TEXT,
    CONSTRAINT pk_papers PRIMARY KEY (id),
    CONSTRAINT unique_papers_title UNIQUE (title)
);

CREATE TABLE IF NOT EXISTS users_papers (
    users_id INTEGER NOT NULL,
    papers_id INTEGER NOT NULl,
    CONSTRAINT pk_users_papers PRIMARY KEY (users_id, papers_id),
    CONSTRAINT fk_users_papers_users FOREIGN KEY (users_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_users_papers_papers FOREIGN KEY (papers_id) REFERENCES papers (id) ON DELETE CASCADE
);