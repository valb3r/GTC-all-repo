CREATE TABLE static_wallet
(
  id             INT(11) PRIMARY KEY AUTO_INCREMENT NOT NULL,
  client_name    VARCHAR(64)                        NOT NULL,
  currency       VARCHAR(64)                        NOT NULL,
  balance        NUMERIC(40, 20)                    NOT NULL,
  status_updated TIMESTAMP                          NOT NULL,
  version        INT                                NOT NULL
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
CREATE INDEX static_wallet_client_name
  ON static_wallet (client_name${PART_INDEX_SZ});
CREATE INDEX static_wallet_currency
  ON static_wallet (currency${PART_INDEX_SZ});
ALTER TABLE static_wallet
  ADD CONSTRAINT unique_static_wallet UNIQUE (client_name, currency);
