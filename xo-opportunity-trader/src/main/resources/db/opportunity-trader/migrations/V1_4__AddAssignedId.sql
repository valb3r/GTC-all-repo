ALTER TABLE trade
  ADD COLUMN assigned_id VARCHAR(64) NOT NULL;

CREATE INDEX trade_assigned_id
  ON trade (assigned_id${PART_INDEX_SZ});

ALTER TABLE trade
  ADD CONSTRAINT unique_trade_assigned_id UNIQUE (assigned_id, client_name);

ALTER TABLE trade_aud ADD COLUMN assigned_id VARCHAR(64);
