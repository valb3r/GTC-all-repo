/* Binance-alike MIN_NOTIONAL */
ALTER TABLE client_config ADD COLUMN min_order_in_to_currency NUMERIC(40, 20);
