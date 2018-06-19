/* Dropping configs since they are not compatible */
DELETE FROM client_config;

ALTER TABLE client_config
  ADD COLUMN currency_to VARCHAR(64) NOT NULL;
ALTER TABLE client_config
  ADD COLUMN use_average_for_trade BIT DEFAULT 0;


ALTER TABLE client_config
  DROP FOREIGN KEY client_config_client;
ALTER TABLE client_config
  DROP INDEX unique_client_config;

ALTER TABLE client_config
  ADD CONSTRAINT unique_client_config UNIQUE (client_name, currency, currency_to);
ALTER TABLE client_config
  ADD CONSTRAINT client_config_client FOREIGN KEY (client_name) REFERENCES client (name);
