ALTER TABLE client_config
  DROP COLUMN min_profitability_pct;
ALTER TABLE client_config
  DROP COLUMN xo_rate_per_sec;
ALTER TABLE client_config
  DROP COLUMN safety_margin_amount_pct;
ALTER TABLE client_config
  DROP COLUMN safety_margin_price_pct;
ALTER TABLE client_config
  DROP COLUMN required_profitablity_pct;
ALTER TABLE client_config
  DROP COLUMN max_solve_time_ms;
ALTER TABLE client_config
  DROP COLUMN is_replenishable;
ALTER TABLE client_config
  DROP COLUMN max_solve_replenish_time_ms;
ALTER TABLE client_config
  DROP COLUMN max_solve_rate_per_s;
ALTER TABLE client_config
  DROP COLUMN stale_book_threshold_ms;
ALTER TABLE client_config
  DROP COLUMN single_side_trade_limit;

CREATE TABLE xo_config
(
  id                          INT(11) PRIMARY KEY REFERENCES client_config (id),

  min_profitability_pct       NUMERIC(40, 20)                                   NOT NULL,
  xo_rate_per_sec             DOUBLE                                            NOT NULL,
  /* Amount of opportunity reported to use, %*/
  safety_margin_amount_pct    NUMERIC(40, 20)                                   NOT NULL,
  /* Maximum safety deviation from price in range best price (0) - non-profit price (100), % */
  safety_margin_price_pct     NUMERIC(40, 20)                                   NOT NULL,
  /* Calculated profit after safety measures should be greater than this %*/
  required_profitablity_pct   NUMERIC(40, 20)                                   NOT NULL,
  max_solve_time_ms           INT                                               NOT NULL DEFAULT 75,
  is_replenishable            BIT                                                        DEFAULT 0,
  max_solve_replenish_time_ms INT                                               NOT NULL DEFAULT 2000,
  max_solve_rate_per_s        INT DEFAULT 1                                     NOT NULL,
  stale_book_threshold_ms     INT                                                        DEFAULT 60000,
  single_side_trade_limit     NUMERIC(40, 20)                                   NOT NULL DEFAULT 0.05,
  enabled                     BIT                                                        DEFAULT 1,

  CHECK (safety_margin_amount_pct >= 0 AND safety_margin_amount_pct <= 100),
  CHECK (safety_margin_price_pct >= 0 AND safety_margin_price_pct <= 100)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

