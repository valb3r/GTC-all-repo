ALTER TABLE xo_trade_stat ADD last_opportunity_id VARCHAR(64);
ALTER TABLE xo_trade_stat ADD last_opport_created_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE xo_trade_stat ADD started_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE xo_trade_stat ADD max_separation_s DOUBLE NOT NULL DEFAULT 0;

ALTER TABLE xo_trade_rejected_stat ADD last_opportunity_id VARCHAR(64);
ALTER TABLE xo_trade_rejected_stat ADD last_opport_created_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE xo_trade_rejected_stat ADD started_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE xo_trade_rejected_stat ADD max_separation_s DOUBLE NOT NULL DEFAULT 0;
