/* Uncompatible changes */
UPDATE trade SET nn_order_id = NULL;
DELETE FROM accepted_nn_trade;

ALTER TABLE trade ADD COLUMN depends_on VARCHAR(36) NULL;
ALTER TABLE trade ADD CONSTRAINT trade_depends_on FOREIGN KEY (depends_on) REFERENCES trade (id);
