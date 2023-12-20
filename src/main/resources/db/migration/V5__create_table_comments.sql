CREATE TABLE IF NOT EXISTS comments (
    id SERIAL,
    user_id INTEGER NOT NULL,
    paper_id INTEGER NOT NULL,
    created_date DATE NOT NULL,
    content TEXT NOT NULL,
    CONSTRAINT pk_comments PRIMARY KEY (id),
    CONSTRAINT fk_comments_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_papers FOREIGN KEY (paper_id) REFERENCES papers (id) ON DELETE CASCADE
);