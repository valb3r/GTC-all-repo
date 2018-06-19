package com.gtc.opportunity.trader.config;

import org.hibernate.dialect.MySQL55Dialect;

/**
 * Created by Valentyn Berezin on 25.02.18.
 */
public class MySqlDialect extends MySQL55Dialect {

    public MySqlDialect() {
        // hibernate thinks there is no such keyword in mysql
        registerKeyword("second");
    }
}
