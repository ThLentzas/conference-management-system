/*
    The generate_file_name is a UUID of fixed length(36) so we can use CHAR instead.
 */
CREATE TABLE IF NOT EXISTS content (
    id INTEGER NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    generated_file_name CHAR(36) NOT NULL,
    file_extension VARCHAR(10) NOT NULL,
    CONSTRAINT pk_content PRIMARY KEY (id),
    CONSTRAINT fk_content_paper_id FOREIGN KEY (id) REFERENCES papers(id) ON DELETE CASCADE
);