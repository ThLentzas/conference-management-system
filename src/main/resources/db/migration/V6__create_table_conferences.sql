CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS conferences (
    id uuid DEFAULT uuid_generate_v4(),
    created_date DATE NOT NULL,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT NOT NULL,
    CONSTRAINT pk_conferences PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS conferences_users (
    conference_id uuid NOT NULL,
    user_id INTEGER NOT NULL,
    CONSTRAINT pk_conferences_users PRIMARY KEY (conference_id, user_iD),
    CONSTRAINT fk_conferences_users_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_conferences_users_conferences FOREIGN KEY (conference_id) REFERENCES conferences ON DELETE CASCADE
);