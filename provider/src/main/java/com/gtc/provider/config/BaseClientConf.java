package com.gtc.provider.config;

import lombok.Data;

/**
 * Created by Valentyn Berezin on 30.12.17.
 */
@Data
public class BaseClientConf {

    private Ws2 ws2;
    private Symbol symbol;
    private int disconnectIfInactiveS = 10;

    @Data
    public static class Ws2 {

        private String root;
    }
}
