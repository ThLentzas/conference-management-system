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
    title VARCHAR(100) UNIQUE NOT NULL,
    abstract_text TEXT NOT NULL,
    authors TEXT NOT NULL,
    state paper_state NOT NULL,
    keywords TEXT,
    conference_id uuid,
    CONSTRAINT pk_papers PRIMARY KEY (id),
    CONSTRAINT unique_papers_title UNIQUE (title)
);

CREATE TABLE IF NOT EXISTS papers_users (
    user_id INTEGER NOT NULL,
    paper_id INTEGER NOT NULL,
    role_type role_type NOT NULL,
    assigned_date DATE NOT NULL,
    CONSTRAINT pk_papers_users PRIMARY KEY (user_id, paper_id),
    CONSTRAINT fk_papers_users_users_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_papers_users_papers_id FOREIGN KEY (paper_id) REFERENCES papers (id) ON DELETE CASCADE
);