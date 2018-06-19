ALTER TABLE accepted_xo_trade ADD COLUMN assoc_non_profit_dev DOUBLE;
ALTER TABLE accepted_xo_trade ADD COLUMN opportunity_best_sell_amount DOUBLE;
ALTER TABLE accepted_xo_trade ADD COLUMN opportunity_best_buy_amount DOUBLE;

UPDATE accepted_xo_trade SET opportunity_best_sell_amount = opportunity_amount;
UPDATE accepted_xo_trade SET opportunity_best_buy_amount = opportunity_amount;
UPDATE accepted_xo_trade SET assoc_non_profit_dev = 0.0;

ALTER TABLE accepted_xo_trade MODIFY assoc_non_profit_dev DOUBLE NOT NULL;
ALTER TABLE accepted_xo_trade MODIFY opportunity_best_sell_amount DOUBLE NOT NULL;
ALTER TABLE accepted_xo_trade MODIFY opportunity_best_buy_amount DOUBLE NOT NULL;
ALTER TABLE accepted_xo_trade DROP COLUMN opportunity_amount;
