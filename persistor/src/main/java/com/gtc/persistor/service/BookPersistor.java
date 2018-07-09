package com.gtc.persistor.service;

import com.google.common.io.MoreFiles;
import com.gtc.model.provider.AggregatedOrder;
import com.gtc.model.provider.OrderBook;
import com.gtc.persistor.config.PersistConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
@Service
@RequiredArgsConstructor
public class BookPersistor {

    private static final String TO_ZIP = ".to_zip";
    private static final String GZ = ".gz";
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH");

    private final PersistConfig cfg;
    private final OrderBookRepository bookRepository;

    @Scheduled(fixedDelayString = PERSIST_S)
    public void persist() {
        List<OrderBook> orderBooks = new ArrayList<>(bookRepository.getOrders());
        appendData(orderBooks);
        bookRepository.clear();
        zipFinishedDataIfNecessary();
    }

    private void appendData(List<OrderBook> books) {
        books.sort(Comparator.comparingLong(a -> a.getMeta().getTimestamp()));
        books.forEach(this::writeBook);
    }

    @SneakyThrows
    private void writeBook(OrderBook book) {
        String filename = baseName(book);
        Path dest = Paths.get(cfg.getDir(), filename);
        boolean exists = dest.toFile().exists();
        try (Writer file = MoreFiles.asCharSink(dest, UTF_8, CREATE, APPEND).openBufferedStream()) {
            if (!exists) {
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
    private void zipFinishedDataIfNecessary() {
        for (Path path : listFilesToZip()) {
            Path toZip = path.getParent().resolve(path.getFileName().toString() + TO_ZIP);
            Files.move(path, toZip, REPLACE_EXISTING);
            String origName = path.getFileName().toString();
            try (GZIPOutputStream out = new GZIPOutputStream(
                    new FileOutputStream(path.getParent().resolve(origName + ".gz").toFile()))) {
                Files.copy(toZip, out);
            }
            Files.delete(toZip);
        }
    }

    @SneakyThrows
    private List<Path> listFilesToZip() {
        try (Stream<Path> pathStream = Files.list(Paths.get(cfg.getDir()))) {
            return pathStream
                    .filter(it -> it.toFile().isFile())
                    .filter(it -> {
                        String path = it.toString();
                        return !path.endsWith(suffix()) && !path.endsWith(TO_ZIP) && !path.endsWith(GZ);
                    })
                    .collect(Collectors.toList());
        }
    }

    private LocalDateTime utcDate() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private String baseName(OrderBook book) {
        return String.format("%s-%s_%s%s",
                book.getMeta().getPair().getFrom(),
                book.getMeta().getPair().getTo(),
                book.getMeta().getClient(),
                suffix()
        );
    }

    private String suffix() {
        LocalDateTime date = utcDate();
        return String.format("-%s.tsv", FORMAT.format(date));
    }
}
