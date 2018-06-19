ALTER TABLE client_config MODIFY safety_margin_amount_pct NUMERIC(3);
ALTER TABLE client_config MODIFY safety_margin_price_pct NUMERIC(4, 3);
ALTER TABLE client_config MODIFY required_profitablity_pct NUMERIC(4, 2);
