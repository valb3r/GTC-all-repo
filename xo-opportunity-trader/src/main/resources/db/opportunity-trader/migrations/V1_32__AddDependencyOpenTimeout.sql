/* Adding timeout that specifies maximum allowable delay for slave to be opened after master */
ALTER TABLE nn_config ADD max_slave_delay_m INT NOT NULL DEFAULT 60;
