ALTER TABLE xo_trade_stat
  ADD COLUMN min_sell_amount DOUBLE;
ALTER TABLE xo_trade_stat
  ADD COLUMN tot_sell_amount NUMERIC(40, 20);
ALTER TABLE xo_trade_stat
  ADD COLUMN max_sell_amount DOUBLE;

ALTER TABLE xo_trade_stat
  ADD COLUMN min_buy_amount DOUBLE;
ALTER TABLE xo_trade_stat
  ADD COLUMN tot_buy_amount NUMERIC(40, 20);
ALTER TABLE xo_trade_stat
  ADD COLUMN max_buy_amount DOUBLE;


UPDATE xo_trade_stat SET min_sell_amount = min_amount;
UPDATE xo_trade_stat SET max_sell_amount = max_amount;
UPDATE xo_trade_stat SET tot_sell_amount = tot_amount;

UPDATE xo_trade_stat SET min_buy_amount = min_amount;
UPDATE xo_trade_stat SET max_buy_amount = max_amount;
UPDATE xo_trade_stat SET tot_buy_amount = tot_amount;


ALTER TABLE xo_trade_stat MODIFY min_sell_amount DOUBLE NOT NULL;
ALTER TABLE xo_trade_stat MODIFY tot_sell_amount NUMERIC(40, 20) NOT NULL;
ALTER TABLE xo_trade_stat MODIFY max_sell_amount DOUBLE NOT NULL;

ALTER TABLE xo_trade_stat MODIFY min_buy_amount DOUBLE NOT NULL;
ALTER TABLE xo_trade_stat MODIFY tot_buy_amount NUMERIC(40, 20) NOT NULL;
ALTER TABLE xo_trade_stat MODIFY max_buy_amount DOUBLE NOT NULL;


ALTER TABLE xo_trade_stat
  DROP COLUMN min_amount;
ALTER TABLE xo_trade_stat
  DROP COLUMN max_amount;
ALTER TABLE xo_trade_stat
  DROP COLUMN tot_amount;
