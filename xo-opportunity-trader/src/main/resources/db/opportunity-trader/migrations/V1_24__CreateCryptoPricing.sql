CREATE TABLE crypto_pricing
(
  currency   VARCHAR(64)     NOT NULL UNIQUE,
  price_usd  NUMERIC(40, 20) NOT NULL,
  price_btc  NUMERIC(40, 20) NOT NULL,
  updated_at TIMESTAMP       NOT NULL
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE INDEX crypto_pricing_currency
  ON crypto_pricing (currency${PART_INDEX_SZ});
