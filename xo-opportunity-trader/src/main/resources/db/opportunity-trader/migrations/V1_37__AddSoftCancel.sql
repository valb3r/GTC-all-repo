CREATE TABLE soft_cancel
(
  id                   INT(11) PRIMARY KEY REFERENCES client_config (id),
  wait_m               INT            NOT NULL DEFAULT 16,
  min_price_loss_pct   NUMERIC(6, 3)  NOT NULL DEFAULT 0.1,
  max_price_loss_pct   NUMERIC(6, 3)  NOT NULL DEFAULT 0.1,
  loss_to_profit_ratio NUMERIC(10, 5) NOT NULL DEFAULT 0.1,
  enabled              BIT                     DEFAULT 1
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
