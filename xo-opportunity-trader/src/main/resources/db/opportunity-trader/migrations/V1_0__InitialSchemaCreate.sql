CREATE TABLE client
(
  name    VARCHAR(64) NOT NULL UNIQUE,
  enabled BOOLEAN
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
CREATE INDEX client_name
  ON client (name${PART_INDEX_SZ});

CREATE TABLE client_config
(
  id                        INT(11) PRIMARY KEY AUTO_INCREMENT NOT NULL,
  client_name               VARCHAR(64)                        NOT NULL,
  currency                  VARCHAR(64)                        NOT NULL,
  min_profitability_pct     NUMERIC(40, 20)                    NOT NULL,
  min_order                 NUMERIC(40, 20),
  max_order                 NUMERIC(40, 20),
  xo_rate_per_sec           DOUBLE                             NOT NULL,
  /* Amount of opportunity reported to use, %*/
  safety_margin_amount_pct  DOUBLE                             NOT NULL,
  /* Maximum safety deviation from price in range best price (0) - non-profit price (100), % */
  safety_margin_price_pct   DOUBLE                             NOT NULL,
  /* Calculated profit after safety measures should be greater than this %*/
  required_profitablity_pct DOUBLE                             NOT NULL,
  /* Fixed charge rate - i.e. trading platform takes 0.1% per Taker trade, so we get 99.9% of amount */
  trade_charge_rate_pct     NUMERIC(40, 20)                    NOT NULL DEFAULT 0,

  CHECK (safety_margin_amount_pct >= 0 AND safety_margin_amount_pct <= 100),
  CHECK (safety_margin_price_pct >= 0 AND safety_margin_price_pct <= 100)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
CREATE INDEX client_config_client_name
  ON client_config (client_name${PART_INDEX_SZ});
CREATE INDEX client_config_currency
  ON client_config (currency${PART_INDEX_SZ});
ALTER TABLE client_config
  ADD CONSTRAINT unique_client_config UNIQUE (client_name, currency);

CREATE TABLE wallet
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
CREATE INDEX wallet_client_name
  ON wallet (client_name${PART_INDEX_SZ});
CREATE INDEX wallet_currency
  ON wallet (currency${PART_INDEX_SZ});
ALTER TABLE wallet
  ADD CONSTRAINT unique_wallet UNIQUE (client_name, currency);

CREATE TABLE accepted_xo_trade
(
  id                          INT(11) PRIMARY KEY AUTO_INCREMENT NOT NULL,
  client_from_name            VARCHAR(64)                        NOT NULL,
  client_to_name              VARCHAR(64)                        NOT NULL,
  currency_from               VARCHAR(64)                        NOT NULL,
  currency_to                 VARCHAR(64)                        NOT NULL,
  amount                      NUMERIC(40, 20)                    NOT NULL,
  price_from_buy              NUMERIC(40, 20)                    NOT NULL,
  price_to_sell               NUMERIC(40, 20)                    NOT NULL,
  expected_profit_pct         NUMERIC(40, 20)                    NOT NULL,
  status                      VARCHAR(64)                        NOT NULL,
  opportunity_opened_on       TIMESTAMP                          NOT NULL,
  opportunity_amount          DOUBLE                             NOT NULL,
  opportunity_best_sell_price DOUBLE                             NOT NULL,
  opportunity_best_buy_price  DOUBLE                             NOT NULL,
  opportunity_profit_pct      DOUBLE                             NOT NULL,
  recorded_on                 TIMESTAMP                          NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_message_id             VARCHAR(36)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
CREATE INDEX trade_order_client_from_name
  ON accepted_xo_trade (client_from_name${PART_INDEX_SZ});
CREATE INDEX trade_order_client_to_name
  ON accepted_xo_trade (client_to_name${PART_INDEX_SZ});
CREATE INDEX trade_order_currency_from_name
  ON accepted_xo_trade (currency_from${PART_INDEX_SZ});
CREATE INDEX trade_order_currency_to_name
  ON accepted_xo_trade (currency_to${PART_INDEX_SZ});
CREATE INDEX trade_order_status
  ON accepted_xo_trade (status${PART_INDEX_SZ});

CREATE TABLE trade
(
  id              VARCHAR(36) PRIMARY KEY NOT NULL,
  client_name     VARCHAR(64)             NOT NULL,
  currency_from   VARCHAR(64)             NOT NULL,
  currency_to     VARCHAR(64)             NOT NULL,
  xo_order_id     INT(11),
  opening_amount  NUMERIC(40, 20)         NOT NULL,
  opening_price   NUMERIC(40, 20)         NOT NULL,
  amount          NUMERIC(40, 20)         NOT NULL,
  price           NUMERIC(40, 20)         NOT NULL,
  closing_amount  NUMERIC(40, 20),
  closing_price   NUMERIC(40, 20),
  is_sell         BOOLEAN                 NOT NULL,
  status          VARCHAR(64)             NOT NULL,
  response_status VARCHAR(64),
  native_status   VARCHAR(64),
  status_updated  TIMESTAMP               NOT NULL,
  version         INT                     NOT NULL,
  wallet_id       INT                     NOT NULL,
  last_message_id VARCHAR(36),
  last_error      VARCHAR(512),
  recorded_on     TIMESTAMP               NOT NULL DEFAULT CURRENT_TIMESTAMP
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
CREATE INDEX trade_client_name
  ON trade (client_name${PART_INDEX_SZ});
CREATE INDEX trade_currency_from
  ON trade (currency_from${PART_INDEX_SZ});
CREATE INDEX trade_currency_to
  ON trade (currency_to${PART_INDEX_SZ});
CREATE INDEX trade_status
  ON trade (status${PART_INDEX_SZ});

CREATE TABLE xo_trade_stat
(
  client_from_name VARCHAR(64)                        NOT NULL,
  client_to_name   VARCHAR(64)                        NOT NULL,
  currency_from    VARCHAR(64)                        NOT NULL,
  currency_to      VARCHAR(64)                        NOT NULL,
  min_amount       DOUBLE                             NOT NULL,
  tot_amount       NUMERIC(40, 20)                    NOT NULL,
  max_amount       DOUBLE                             NOT NULL,
  min_hist_win     DOUBLE                             NOT NULL,
  tot_hist_win     NUMERIC(40, 20)                    NOT NULL,
  max_hist_win     DOUBLE                             NOT NULL,
  tot_ticker_win   NUMERIC(40, 20)                    NOT NULL,
  max_ticker_win   DOUBLE                             NOT NULL,
  min_duration_s   DOUBLE                             NOT NULL,
  tot_duration_s   NUMERIC(40, 20)                    NOT NULL,
  max_duration_s   DOUBLE                             NOT NULL,
  record_count     BIGINT                             NOT NULL,
  version          INT                                NOT NULL,

  since_date       DATE                               NOT NULL,
  profit_group_pct_min NUMERIC(4,2)                        NOT NULL,
  profit_group_pct_max NUMERIC(4,2)                        NOT NULL,
  kind             VARCHAR(64)                        NOT NULL
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
CREATE INDEX xo_trade_stat_client_from_name
  ON xo_trade_stat (client_from_name${PART_INDEX_SZ});
CREATE INDEX xo_trade_stat_client_to_name
  ON xo_trade_stat (client_to_name${PART_INDEX_SZ});
CREATE INDEX xo_trade_stat_currency_from
  ON xo_trade_stat (currency_from${PART_INDEX_SZ});
CREATE INDEX xo_trade_stat_currency_to
  ON xo_trade_stat (currency_to${PART_INDEX_SZ});
CREATE INDEX xo_trade_stat_kind
  ON xo_trade_stat (kind${PART_INDEX_SZ});
CREATE INDEX xo_trade_profit_group_pct_min
  ON xo_trade_stat (profit_group_pct_min);
CREATE INDEX xo_trade_profit_group_pct_max
  ON xo_trade_stat (profit_group_pct_max);
ALTER TABLE xo_trade_stat
  ADD CONSTRAINT unique_xo_trade_stat UNIQUE (client_from_name, client_to_name, currency_from, currency_to, kind,
                                              since_date, profit_group_pct_min, profit_group_pct_max);


ALTER TABLE client_config
  ADD CONSTRAINT client_config_client FOREIGN KEY (client_name) REFERENCES client (name);
ALTER TABLE wallet
  ADD CONSTRAINT wallet_client FOREIGN KEY (client_name) REFERENCES client (name);
ALTER TABLE accepted_xo_trade
  ADD CONSTRAINT accepted_xo_trade_client_from FOREIGN KEY (client_from_name) REFERENCES client (name);
ALTER TABLE accepted_xo_trade
  ADD CONSTRAINT accepted_xo_trade_client_to FOREIGN KEY (client_from_name) REFERENCES client (name);

ALTER TABLE trade
  ADD CONSTRAINT accepted_xo_trade_id FOREIGN KEY (xo_order_id) REFERENCES accepted_xo_trade (id);
