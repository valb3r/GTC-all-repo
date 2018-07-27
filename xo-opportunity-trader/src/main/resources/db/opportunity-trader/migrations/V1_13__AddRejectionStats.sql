CREATE TABLE xo_trade_rejected_stat
(
  client_from_name     VARCHAR(64)   NOT NULL,
  client_to_name       VARCHAR(64)   NOT NULL,
  currency_from        VARCHAR(64)   NOT NULL,
  currency_to          VARCHAR(64)   NOT NULL,
  version              INT           NOT NULL,

  reason               VARCHAR(256)  NOT NULL,
  last_threshold       DOUBLE,
  total_threshold      DOUBLE,
  last_value           DOUBLE,
  total_value          DOUBLE,
  record_count         BIGINT        NOT NULL,

  since_date           DATE          NOT NULL,
  profit_group_pct_min NUMERIC(4, 2) NOT NULL,
  profit_group_pct_max NUMERIC(4, 2) NOT NULL,
  kind                 VARCHAR(64)   NOT NULL
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
CREATE INDEX xo_trade_rejected_stat_client_from_name
  ON xo_trade_rejected_stat (client_from_name${PART_INDEX_SZ});
CREATE INDEX xo_trade_rejected_stat_client_to_name
  ON xo_trade_rejected_stat (client_to_name${PART_INDEX_SZ});
CREATE INDEX xo_trade_rejected_stat_currency_from
  ON xo_trade_rejected_stat (currency_from${PART_INDEX_SZ});
CREATE INDEX xo_trade_rejected_stat_currency_to
  ON xo_trade_rejected_stat (currency_to${PART_INDEX_SZ});
CREATE INDEX xo_trade_rejected_stat_kind
  ON xo_trade_rejected_stat (kind${PART_INDEX_SZ});
CREATE INDEX xo_trade_rejected_stat_profit_group_pct_min
  ON xo_trade_rejected_stat (profit_group_pct_min);
CREATE INDEX xo_trade_rejected_stat_profit_group_pct_max
  ON xo_trade_rejected_stat (profit_group_pct_max);
ALTER TABLE xo_trade_rejected_stat
  ADD CONSTRAINT unique_xo_trade_rejected_stat UNIQUE (client_from_name, client_to_name, currency_from, currency_to,
                                                       kind, since_date, profit_group_pct_min, profit_group_pct_max);
