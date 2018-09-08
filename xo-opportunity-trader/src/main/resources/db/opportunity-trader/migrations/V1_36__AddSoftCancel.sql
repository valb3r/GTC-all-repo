CREATE TABLE soft_cancel_config
(
  id                   INT(11) PRIMARY KEY REFERENCES client_config (id),
  wait_m               INT            NOT NULL DEFAULT 16,
  min_price_loss_pct   NUMERIC(6, 3)  NOT NULL DEFAULT 0.1,
  max_price_loss_pct   NUMERIC(6, 3)  NOT NULL DEFAULT 0.1,
  done_to_cancel_ratio NUMERIC(10, 1) NOT NULL DEFAULT 10,
  enabled              BIT                     DEFAULT 1
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE soft_cancel
(
  id        INT(11) PRIMARY KEY REFERENCES soft_cancel_config (id),
  done      INT NOT NULL DEFAULT 0,
  cancelled INT NOT NULL DEFAULT 0
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
