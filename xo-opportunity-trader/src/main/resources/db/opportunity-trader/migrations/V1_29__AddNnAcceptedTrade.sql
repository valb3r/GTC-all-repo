CREATE TABLE accepted_nn_trade
(
  id                       INT(11) PRIMARY KEY AUTO_INCREMENT NOT NULL,
  client                   VARCHAR(64)                        NOT NULL,
  currency_from            VARCHAR(64)                        NOT NULL,
  currency_to              VARCHAR(64)                        NOT NULL,
  amount                   NUMERIC(40, 20)                    NOT NULL,
  price_from_buy           NUMERIC(40, 20)                    NOT NULL,
  price_to_sell            NUMERIC(40, 20)                    NOT NULL,
  expected_delta_from      NUMERIC(40, 20)                    NOT NULL,
  expected_delta_to        NUMERIC(40, 20)                    NOT NULL,
  confidence               DOUBLE                             NOT NULL,
  model_age_s              INT(11)                            NOT NULL,
  average_noop_label_age_s INT(11)                            NOT NULL,
  average_act_label_age_s  INT(11)                            NOT NULL,
  strategy                 VARCHAR(36)                        NOT NULL,
  status                   VARCHAR(36)                        NOT NULL,
  recorded_on              TIMESTAMP                          NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_message_id          VARCHAR(36)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
CREATE INDEX accepted_nn_trade_client
  ON accepted_nn_trade (client${PART_INDEX_SZ});
CREATE INDEX accepted_nn_trade_currency_from_name
  ON accepted_nn_trade (currency_from${PART_INDEX_SZ});
CREATE INDEX accepted_nn_currency_to_name
  ON accepted_nn_trade (currency_to${PART_INDEX_SZ});
CREATE INDEX accepted_nn_status
  ON accepted_nn_trade (status${PART_INDEX_SZ});

CREATE TABLE accepted_nn_trade_aud
(
  id              INT         NOT NULL,
  rev             INT         NOT NULL,
  revtype         TINYINT,
  status          VARCHAR(36) NOT NULL,
  last_message_id VARCHAR(36),
  CONSTRAINT accepted_nn_trade_aud_pkey PRIMARY KEY (id, rev),
  CONSTRAINT accepted_nn_trade_aud_revinfo FOREIGN KEY (rev)
  REFERENCES revinfo (rev)
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

ALTER TABLE trade
  ADD COLUMN nn_order_id INT(11);
ALTER TABLE trade
  ADD CONSTRAINT accepted_nn_trade_id FOREIGN KEY (nn_order_id) REFERENCES accepted_nn_trade (id);
