package com.gtc.opportunity.trader.service.nnopportunity.global;

import com.gtc.meta.CurrencyPair;
import com.gtc.model.provider.OrderBook;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

/**
 * Created by Valentyn Berezin on 31.07.18.
 */
@Slf4j
@RequiredArgsConstructor
public class HistoryBookReader implements AutoCloseable {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH");

    private final String historyFolder;
    private final String client;
    private final CurrencyPair pair;
    private final LocalDateTime min;
    private final LocalDateTime max;

    private LocalDateTime time;
    private CSVParser parser;
    private Iterator<CSVRecord> recordIterator;

    @SneakyThrows
    public OrderBook read() {
        initializeStreamIfNeeded();
        if (recordIterator.hasNext()) {
            return CsvRowToOrderBookMapper.mapCsvToBook(client, pair, recordIterator.next());
        } else {
            parser.close();
            findNextFile();
            return read();
        }
    }

    @Override
    public void close() throws Exception {
        parser.close();
    }

    private void initializeStreamIfNeeded() {
        if (null != parser) {
            return;
        }

        time = min.minusHours(1);
        findNextFile();
    }

    @SneakyThrows
    private void findNextFile() {
        Path file;
        do {
            time = time.plusHours(1);
            if (time.compareTo(max) > 0) {
                throw new NoSuchElementException();
            }

            file = Paths.get(historyFolder, fileName(client, pair, time));
        } while (!file.toFile().exists());

        parser = new CSVParser(
                new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file.toFile())))),
                CSVFormat.MYSQL.withFirstRecordAsHeader()
        );

        recordIterator = parser.iterator();
    }

    // ZEC-BTC_okex-2018-07-30T04.tsv.gz
    private String fileName(String client, CurrencyPair pair, LocalDateTime time) {
        return String.format("%s-%s_%s-%s.tsv.gz",
                pair.getFrom().getCode(),
                pair.getTo().getCode(),
                client,
                FMT.format(time)
        );
    }
}
