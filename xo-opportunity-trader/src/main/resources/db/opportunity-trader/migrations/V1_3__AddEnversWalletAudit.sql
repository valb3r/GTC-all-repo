CREATE TABLE wallet_aud
(
  id      INT             NOT NULL,
  rev     INT             NOT NULL,
  revtype TINYINT,
  balance NUMERIC(40, 20) NOT NULL,
  CONSTRAINT wallet_aud_pkey PRIMARY KEY (id, rev),
  CONSTRAINT wallet_aud_revinfo FOREIGN KEY (rev)
  REFERENCES revinfo (rev)
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
