CREATE TABLE revinfo
(
  rev      INT(11) PRIMARY KEY AUTO_INCREMENT NOT NULL,
  revtstmp BIGINT,
  /* Technically is a duplicate of revtstmp, but there are minor differences - envers uses VM time*/
  db_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE accepted_xo_trade_aud
(
  id              INT NOT NULL,
  rev             INT NOT NULL,
  revtype         TINYINT,
  last_message_id VARCHAR(36),
  status          VARCHAR(64),
  CONSTRAINT accepted_xo_trade_aud_pkey PRIMARY KEY (id, rev),
  CONSTRAINT accepted_xo_trade_aud_revinfo FOREIGN KEY (rev)
  REFERENCES revinfo (rev)
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE trade_aud
(
  id              VARCHAR(36) NOT NULL,
  rev             INT         NOT NULL,
  revtype         TINYINT,
  last_message_id VARCHAR(36),
  last_error      VARCHAR(512),
  status          VARCHAR(64),
  response_status VARCHAR(64),
  native_status   VARCHAR(64),
  amount          NUMERIC(40, 20),
  price           NUMERIC(40, 20),
  CONSTRAINT trade_aud_pkey PRIMARY KEY (id, rev),
  CONSTRAINT aud_revinfo FOREIGN KEY (rev)
  REFERENCES revinfo (rev)
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
