package com.gtc.persistor.service;

import com.google.common.io.MoreFiles;
import com.gtc.model.provider.AggregatedOrder;
import com.gtc.model.provider.OrderBook;
import com.gtc.persistor.config.PersistConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.gtc.persistor.config.Const.Persist.PERSIST_S;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * Created by Valentyn Berezin on 01.07.18.
 */
@Service
@RequiredArgsConstructor
public class BookPersistor {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH");

    private final PersistConfig cfg;
    private final OrderBookRepository bookRepository;

    @Scheduled(fixedDelayString = PERSIST_S)
    public void persist() {
        LocalDateTime date = LocalDateTime.now(ZoneOffset.UTC);
        List<OrderBook> orderBooks = new ArrayList<>(bookRepository.getOrders());
        appendData(date, orderBooks);
        bookRepository.clear();
        zipFinishedDataIfNecessary();
    }

    private void appendData(LocalDateTime time, List<OrderBook> books) {
        books.sort(Comparator.comparingLong(a -> a.getMeta().getTimestamp()));
        books.forEach(it -> writeBook(time, it));
    }

    @SneakyThrows
    private void writeBook(LocalDateTime time, OrderBook book) {
        String filename = baseName(book) + "-" + FORMAT.format(time);
        Path dest = Paths.get(cfg.getDir(), filename);
        try (Writer file = MoreFiles.asCharSink(dest, UTF_8, CREATE, APPEND).openBufferedStream()) {
            if (!dest.toFile().exists()) {
                writeHeader(file, book.getHistogramBuy().length, book.getHistogramSell().length);
            }

            writeBook(file, book);
        }
    }

    @SneakyThrows
    private void writeHeader(Writer file, int histogramBuyPrecision, int histogramSellPrecision) {
        file.write("Time\t");
        file.write("Best buy\t");
        file.write("Best sell\t");
        file.write("Histogram price sell step\t");
        file.write("Histogram price buy step\t");
        BiConsumer<String, Integer> writeAmounts = (name, precision) -> {
            for (int i = 0; i < precision; ++i) {
                file.write(name + i);

                if (i != precision - 1) {
                    file.write("\t");
                }
            }
        };

        writeAmounts.accept("Buy amount at ", histogramBuyPrecision);
        writeAmounts.accept("Sell amount at ", histogramSellPrecision);
    }

    private void writeBook(Writer file, OrderBook book) {
        writeField(file, book.getMeta().getTimestamp());
        writeField(file, book.getBestBuy());
        writeField(file, book.getBestSell());
        writeField(file, book.getHistogramBuy()[0].getMaxPrice() - book.getHistogramBuy()[0].getMinPrice());
        writeField(file, book.getHistogramSell()[0].getMaxPrice() - book.getHistogramSell()[0].getMinPrice());

        Consumer<AggregatedOrder[]> writeHistogram = histogram -> {
            for (int i = 0; i < histogram.length; ++i) {
                writeField(file, histogram[i].getAmount(), i != histogram.length - 1);
            }
        };

        writeHistogram.accept(book.getHistogramBuy());
        writeHistogram.accept(book.getHistogramSell());
    }

    @SneakyThrows
    private <T> void writeField(Writer file, T value) {
        writeField(file, value, true);
    }

    @SneakyThrows
    private <T> void writeField(Writer file, T value, boolean hasSeparator) {
        file.write(String.valueOf(value) + (hasSeparator ? "\t" : ""));
    }

    private void zipFinishedDataIfNecessary() {
    }

    private String baseName(OrderBook book) {
        return String.format("%s-%s_%s",
                book.getMeta().getPair().getFrom(),
                book.getMeta().getPair().getTo(),
                book.getMeta().getClient()
        );
    }
}
