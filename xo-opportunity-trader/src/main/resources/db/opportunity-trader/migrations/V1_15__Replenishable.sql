ALTER TABLE client_config ADD COLUMN is_replenishable BIT DEFAULT 0;

ALTER TABLE accepted_xo_trade ADD COLUMN expected_profit NUMERIC(40, 20) DEFAULT 0;
UPDATE accepted_xo_trade SET expected_profit = ABS(amount * expected_profit_pct);
ALTER TABLE accepted_xo_trade MODIFY expected_profit NUMERIC(40, 20) NOT NULL;

ALTER TABLE trade ADD COLUMN expected_reverse_amount NUMERIC(40, 20) DEFAULT 0;
UPDATE trade SET expected_reverse_amount = 0;
ALTER TABLE trade MODIFY expected_reverse_amount NUMERIC(40, 20) NOT NULL;
