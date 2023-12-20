/*
    The generate_file_name is a UUID of fixed length(36) so we can use CHAR instead.
 */
CREATE TABLE IF NOT EXISTS content (
    id SERIAL,
    original_file_name VARCHAR(255) NOT NULL,
    generated_file_name CHAR(36) NOT NULL,
    file_extension VARCHAR(10) NOT NULL,
    paper_id INTEGER NOT NULL,
    CONSTRAINT pk_content PRIMARY KEY (id),
    CONSTRAINT fk_content_papers FOREIGN KEY (paper_id) REFERENCES papers(id) ON DELETE CASCADE
);