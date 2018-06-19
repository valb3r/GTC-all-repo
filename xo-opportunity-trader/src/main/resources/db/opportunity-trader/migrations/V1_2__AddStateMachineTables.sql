CREATE TABLE statemachine_action
(
  id   BIGINT AUTO_INCREMENT
    PRIMARY KEY,
  name VARCHAR(255) NULL,
  spel VARCHAR(255) NULL
)
  ENGINE = InnoDB;

CREATE TABLE statemachine_deferred_events
(
  jpa_repository_state_id BIGINT       NOT NULL,
  deferred_events       VARCHAR(255) NULL
)
  ENGINE = InnoDB;

CREATE INDEX fk_state_deferred_events
  ON statemachine_deferred_events (jpa_repository_state_id);

CREATE TABLE statemachine_guard
(
  id   BIGINT AUTO_INCREMENT
    PRIMARY KEY,
  name VARCHAR(255) NULL,
  spel VARCHAR(255) NULL
)
  ENGINE = InnoDB;

CREATE TABLE statemachine_state_machine
(
  machine_id            VARCHAR(255) NOT NULL
    PRIMARY KEY,
  state                 VARCHAR(255) NULL,
  state_machine_context LONGBLOB     NULL
)
  ENGINE = InnoDB;

CREATE TABLE statemachine_state
(
  id                BIGINT AUTO_INCREMENT
    PRIMARY KEY,
  initial_state     BIT          NULL,
  kind              INT          NULL,
  machine_id        VARCHAR(255) NULL,
  region            VARCHAR(255) NULL,
  state             VARCHAR(255) NULL,
  submachine_id     VARCHAR(255) NULL,
  initial_action_id BIGINT       NULL,
  parent_state_id   BIGINT       NULL,
  CONSTRAINT fk_state_initial_action
  FOREIGN KEY (initial_action_id) REFERENCES statemachine_action (id),
  CONSTRAINT fk_state_parent_state
  FOREIGN KEY (parent_state_id) REFERENCES statemachine_state (id)
)
  ENGINE = InnoDB;

CREATE INDEX fk_state_initial_action
  ON statemachine_state (initial_action_id);

CREATE INDEX fk_state_parent_state
  ON statemachine_state (parent_state_id);

ALTER TABLE statemachine_deferred_events
  ADD CONSTRAINT fk_state_deferred_events
FOREIGN KEY (jpa_repository_state_id) REFERENCES statemachine_state (id);

CREATE TABLE statemachine_state_state_actions
(
  jpa_repository_state_id BIGINT NOT NULL,
  state_actions_id      BIGINT NOT NULL,
  PRIMARY KEY (jpa_repository_state_id, state_actions_id),
  CONSTRAINT fk_state_state_actions_s
  FOREIGN KEY (jpa_repository_state_id) REFERENCES statemachine_state (id),
  CONSTRAINT fk_state_state_actions_a
  FOREIGN KEY (state_actions_id) REFERENCES statemachine_action (id)
)
  ENGINE = InnoDB;

CREATE INDEX fk_state_state_actions_a
  ON statemachine_state_state_actions (state_actions_id);

CREATE TABLE statemachine_state_entry_actions
(
  jpa_repository_state_id BIGINT NOT NULL,
  entry_actions_id      BIGINT NOT NULL,
  PRIMARY KEY (jpa_repository_state_id, entry_actions_id),
  CONSTRAINT fk_state_entry_actions_s
  FOREIGN KEY (jpa_repository_state_id) REFERENCES statemachine_state (id),
  CONSTRAINT fk_state_entry_actions_a
  FOREIGN KEY (entry_actions_id) REFERENCES statemachine_action (id)
)
  ENGINE = InnoDB;

CREATE INDEX fk_state_entry_actions_a
  ON statemachine_state_entry_actions (entry_actions_id);

CREATE TABLE statemachine_state_exit_actions
(
  jpa_repository_state_id BIGINT NOT NULL,
  exit_actions_id       BIGINT NOT NULL,
  PRIMARY KEY (jpa_repository_state_id, exit_actions_id),
  CONSTRAINT fk_state_exit_actions_s
  FOREIGN KEY (jpa_repository_state_id) REFERENCES statemachine_state (id),
  CONSTRAINT fk_state_exit_actions_a
  FOREIGN KEY (exit_actions_id) REFERENCES statemachine_action (id)
)
  ENGINE = InnoDB;

CREATE INDEX fk_state_exit_actions_a
  ON statemachine_state_exit_actions (exit_actions_id);

CREATE TABLE statemachine_transition
(
  id         BIGINT AUTO_INCREMENT
    PRIMARY KEY,
  event      VARCHAR(255) NULL,
  kind       INT          NULL,
  machine_id VARCHAR(255) NULL,
  guard_id   BIGINT       NULL,
  source_id  BIGINT       NULL,
  target_id  BIGINT       NULL,
  CONSTRAINT fk_transition_guard
  FOREIGN KEY (guard_id) REFERENCES statemachine_guard (id),
  CONSTRAINT fk_transition_source
  FOREIGN KEY (source_id) REFERENCES statemachine_state (id),
  CONSTRAINT fk_transition_target
  FOREIGN KEY (target_id) REFERENCES statemachine_state (id)
)
  ENGINE = InnoDB;

CREATE INDEX fk_transition_guard
  ON statemachine_transition (guard_id);

CREATE INDEX fk_transition_source
  ON statemachine_transition (source_id);

CREATE INDEX fk_transition_target
  ON statemachine_transition (target_id);

CREATE TABLE statemachine_transition_actions
(
  jpa_repository_transition_id BIGINT NOT NULL,
  actions_id                 BIGINT NOT NULL,
  PRIMARY KEY (jpa_repository_transition_id, actions_id),
  CONSTRAINT fk_transition_actions_t
  FOREIGN KEY (jpa_repository_transition_id) REFERENCES statemachine_transition (id),
  CONSTRAINT fk_transition_actions_a
  FOREIGN KEY (actions_id) REFERENCES statemachine_action (id)
)
  ENGINE = InnoDB;

CREATE INDEX fk_transition_actions_a
  ON statemachine_transition_actions (actions_id);

