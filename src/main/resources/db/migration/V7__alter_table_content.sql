ALTER TABLE content
DROP COLUMN paper_id;

ALTER TABLE content
    ADD CONSTRAINT fk_content_papers FOREIGN KEY (id) REFERENCES papers(id) ON DELETE CASCADE;
