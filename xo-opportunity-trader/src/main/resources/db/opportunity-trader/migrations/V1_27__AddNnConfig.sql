CREATE TABLE nn_config
(
  id                          INT(11) PRIMARY KEY REFERENCES client_config (id),

  future_n_window             INT             NOT NULL DEFAULT 36000,
  collect_n_labeled           INT             NOT NULL DEFAULT 1000,
  noop_threshold              NUMERIC(40, 20) NOT NULL DEFAULT 1.002,
  truth_threshold             NUMERIC(40, 20) NOT NULL DEFAULT 0.7,
  proceed_false_positive      NUMERIC(40, 20) NOT NULL DEFAULT 0.3,
  average_dt_s_between_labels NUMERIC(40, 20) NOT NULL DEFAULT 0.5,
  book_test_for_open_per_s    NUMERIC(40, 20) NOT NULL DEFAULT 0.5,
  old_threshold_m             INT             NOT NULL DEFAULT 300,
  train_relative_size         NUMERIC(40, 20) NOT NULL DEFAULT 0.7,
  n_train_iterations          INT             NOT NULL,
  future_price_gain_pct       NUMERIC(40, 20) NOT NULL DEFAULT 0.1,
  network_yaml_spec           LONGTEXT        NOT NULL,
  enabled                     BIT                      DEFAULT 1,

  CHECK (future_n_window > 0),
  CHECK (collect_n_labeled > 0),
  CHECK (noop_threshold > 0),
  CHECK (truth_threshold > 0 AND truth_threshold < 1.0),
  CHECK (proceed_false_positive > 0 AND proceed_false_positive < 1.0),
  CHECK (average_dt_s_between_labels > 0),
  CHECK (book_test_for_open_per_s > 0),
  CHECK (old_threshold_m > 0),
  CHECK (train_relative_size > 0 AND train_relative_size < 1.0)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
