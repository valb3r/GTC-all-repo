package com.gtc.provider.controller.dto.stat;

import com.gtc.meta.CurrencyPair;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Created by Valentyn Berezin on 31.12.17.
 */
@Data
public class StatDto {

    private final long currentTimestamp = System.currentTimeMillis();
    private final Map<String, List<ClientStat>> clients;

    @Data
    public static class ClientStat {

        private final String symbol;
        private final boolean connected;
        private final CurrencyPair pair;
        private final Timestamp timestamp;
        private final long bidCount;

        @Data
        public static class Timestamp {
            private final long ticker;
            private final long oldestBid;
            private final long newestBid;
            private final long avgBid;
        }
    }
}
