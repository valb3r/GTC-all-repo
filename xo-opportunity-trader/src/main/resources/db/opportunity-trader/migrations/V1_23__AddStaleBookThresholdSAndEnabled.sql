ALTER TABLE client_config ADD COLUMN stale_book_threshold_ms INT DEFAULT 60000;
ALTER TABLE client_config ADD COLUMN enabled BIT DEFAULT 1;
