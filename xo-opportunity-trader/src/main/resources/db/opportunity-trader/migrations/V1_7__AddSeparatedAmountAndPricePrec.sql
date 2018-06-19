ALTER TABLE client_config CHANGE scale scale_price INT NOT NULL;
ALTER TABLE client_config ADD COLUMN scale_amount INT NOT NULL DEFAULT 4;
