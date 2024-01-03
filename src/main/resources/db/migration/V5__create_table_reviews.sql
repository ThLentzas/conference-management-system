/*
    Initially the reviewed date, comment and score can be null since we just assigned a reviewer to a paper
 */
CREATE TABLE IF NOT EXISTS reviews (
    id SERIAL,
    user_id INTEGER NOT NULL,
    paper_id INTEGER NOT NULL,
    assigned_date DATE,
    reviewed_date DATE DEFAULT CURRENT_DATE,
    comment TEXT,
    score DOUBLE PRECISION,
    CONSTRAINT pk_reviews PRIMARY KEY (id),
    CONSTRAINT fk_reviews_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_papers FOREIGN KEY (paper_id) REFERENCES papers (id) ON DELETE CASCADE
);