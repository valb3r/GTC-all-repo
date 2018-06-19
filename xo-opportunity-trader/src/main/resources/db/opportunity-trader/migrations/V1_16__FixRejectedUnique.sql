ALTER TABLE xo_trade_rejected_stat
  DROP INDEX unique_xo_trade_rejected_stat;

ALTER TABLE xo_trade_rejected_stat
  ADD CONSTRAINT unique_xo_trade_rejected_stat UNIQUE (client_from_name, client_to_name, currency_from, currency_to,
                                                       kind, since_date, profit_group_pct_min, profit_group_pct_max,
                                                       reason)
