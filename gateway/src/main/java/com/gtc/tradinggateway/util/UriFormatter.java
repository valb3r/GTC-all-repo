package com.gtc.tradinggateway.util;

/**
 * Created by mikro on 25.01.2018.
 */
public class UriFormatter {

    private StringBuilder builder = new StringBuilder();

    public void addToUri(String name, Object property) {
        if (builder.length() > 0) {
            builder.append("&");
        }
        builder.append(name).append("=").append(property);
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}