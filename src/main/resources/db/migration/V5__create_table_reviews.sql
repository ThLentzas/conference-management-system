
CREATE TABLE IF NOT EXISTS reviews (
    id SERIAL,
    paper_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    reviewed_date DATE NOT NULL,
    comment TEXT,
    score DOUBLE PRECISION,
    CONSTRAINT pk_reviews PRIMARY KEY (id),
    CONSTRAINT fk_reviews_papers_id FOREIGN KEY (paper_id) REFERENCES papers (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_users_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);