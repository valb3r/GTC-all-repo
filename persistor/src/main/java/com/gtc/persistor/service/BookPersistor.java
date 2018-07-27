package com.gtc.persistor.service;

import com.google.common.io.MoreFiles;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.gtc.model.provider.AggregatedOrder;
import com.gtc.model.provider.OrderBook;
import com.gtc.persistor.config.PersistConfig;
import com.newrelic.api.agent.Trace;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import static com.gtc.persistor.config.Const.Persist.PERSIST_S;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * Created by Valentyn Berezin on 01.07.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookPersistor {

    private static final String TO_ZIP = ".to_zip";
    private static final String GZ = ".gz";
    private static final long MILLIS_IN_HOUR = 3600 * 1000L;
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH");

    private final PersistConfig cfg;
    private final OrderBookRepository bookRepository;

    @Value(PERSIST_S)
    private Integer persistS;

    @Trace(dispatcher = true)
    @Scheduled(fixedDelayString = PERSIST_S)
    @SneakyThrows
    public void persist() {
        List<OrderBook> orderBooks = new ArrayList<>(bookRepository.getOrders());
        bookRepository.clear();
        new SimpleTimeLimiter().callWithTimeout(
                () -> appendData(orderBooks),
                persistS,
                TimeUnit.SECONDS,
                true
        );
    }

    @Trace(dispatcher = true)
    @Scheduled(fixedDelayString = PERSIST_S)
    public void zipIfNecessary() {
        zipFinishedDataAndMoveToStorageIfNecessary();
    }

    // Void for prettier call using SimpleTimeLimiter
    private Void appendData(List<OrderBook> books) {
        Map<String, List<OrderBook>> booksToFile = new HashMap<>();
        String suffix = getSuffixAndLockDate();

        books.forEach(it -> booksToFile
                .computeIfAbsent(baseName(it, suffix), id -> new ArrayList<>())
                .add(it)
        );

        booksToFile.forEach(this::writeBooks);
        return null;
    }

    @SneakyThrows
    private void writeBooks(String filename, List<OrderBook> books) {
        if (books.isEmpty()) {
            return;
        }

        books.sort(Comparator.comparingLong(a -> a.getMeta().getTimestamp()));
        Path dest = Paths.get(cfg.getLocalDir(), filename);
        boolean exists = dest.toFile().exists();

        try (Writer file = MoreFiles.asCharSink(dest, UTF_8, CREATE, APPEND).openBufferedStream()) {
            if (!exists) {
                writeHeader(file, books.get(0).getHistogramBuy().length, books.get(0).getHistogramSell().length);
            }

            writeBooksInterruptably(filename, books, file);
        }
    }

    private void writeBooksInterruptably(String filename, List<OrderBook> books, Writer file)
            throws InterruptedException {
        for (OrderBook book : books) {
            writeBook(file, book);

            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Interrupted on " + filename);
            }
        }
    }

    @SneakyThrows
    private void writeHeader(Writer file, int histogramBuyPrecision, int histogramSellPrecision) {
        file.write("Time\t");
        file.write("Best buy\t");
        file.write("Best sell\t");
        file.write("Histogram price sell step\t");
        file.write("Histogram price buy step\t");

        writeAmounts(file, "Buy amount at ", histogramBuyPrecision);
        writeAmounts(file, "Sell amount at ", histogramSellPrecision);
        file.write(System.lineSeparator());
    }

    @SneakyThrows
    private void writeAmounts(Writer file, String name, int precision) {
        for (int i = 0; i < precision; ++i) {
            file.write(name + i + "\t");
        }
    }

    @SneakyThrows
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
        file.write("\t");
        writeHistogram.accept(book.getHistogramSell());
        file.write(System.lineSeparator());
    }

    @SneakyThrows
    private <T> void writeField(Writer file, T value) {
        writeField(file, value, true);
    }

    @SneakyThrows
    private <T> void writeField(Writer file, T value, boolean hasSeparator) {
        file.write(String.valueOf(value) + (hasSeparator ? "\t" : ""));
    }

    @SneakyThrows
    private void zipFinishedDataAndMoveToStorageIfNecessary() {
        for (Path path : listFilesToZip()) {
            Path toZip = path.getParent().resolve(path.getFileName().toString() + TO_ZIP);
            Files.move(path, toZip, REPLACE_EXISTING);

            String origName = path.getFileName().toString();
            Path zipLocal = path.getParent().resolve(origName + ".gz");
            try (GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(zipLocal.toFile()))) {
                Files.copy(toZip, out);
            }
            Files.delete(toZip);

            Files.move(zipLocal, Paths.get(cfg.getStorageDir(), zipLocal.getFileName().toString()), REPLACE_EXISTING);
        }
    }

    @SneakyThrows
    private List<Path> listFilesToZip() {
        try (Stream<Path> pathStream = Files.list(Paths.get(cfg.getLocalDir()))) {
            return pathStream
                    .filter(it -> it.toFile().isFile())
                    .filter(it -> System.currentTimeMillis() - it.toFile().lastModified() > MILLIS_IN_HOUR)
                    .filter(it -> {
                        String path = it.toString();
                        return !path.endsWith(TO_ZIP) && !path.endsWith(GZ);
                    })
                    .collect(Collectors.toList());
        }
    }

    private LocalDateTime utcDate() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private String baseName(OrderBook book, String suffix) {
        return String.format("%s-%s_%s%s",
                book.getMeta().getPair().getFrom(),
                book.getMeta().getPair().getTo(),
                book.getMeta().getClient(),
                suffix
        );
    }

    private String getSuffixAndLockDate() {
        LocalDateTime date = utcDate();
        return String.format("-%s.tsv", FORMAT.format(date));
    }
}
