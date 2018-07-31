package com.gtc.opportunity.trader.service.nnopportunity.solver.model;

import com.google.common.primitives.Floats;
import com.gtc.opportunity.trader.service.dto.FlatOrderBook;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
@Component
public class FeatureMapper {

    public static final int CAN_PROCEED_POS = 0;
    private static final int FEATURE_SIZE = 20;
    private static final int LABEL_SIZE = 2;

    INDArray extractFeatures(FlatOrderBook book) {
        return new NDArray(new float[][] {features(book)});
    }

    /**
     * Can proceed one-hot is at 0th element.
     */
    INDArray extractLabels(boolean canProceed) {
        return new NDArray(new float[][] {
                new float[] {map(0, canProceed), map(1, canProceed)}
        });
    }

    DataSet extract(List<FlatOrderBook> canProceed, List<FlatOrderBook> noop) {
        INDArray features = Nd4j.create(canProceed.size() + noop.size(), FEATURE_SIZE);
        INDArray labels = Nd4j.create(canProceed.size() + noop.size(), LABEL_SIZE);

        int row = 0;
        for (FlatOrderBook book : canProceed) {
            features.putRow(row, extractFeatures(book));
            labels.putRow(row, extractLabels(true));
            row++;
        }
        for (FlatOrderBook book : noop) {
            features.putRow(row, extractFeatures(book));
            labels.putRow(row, extractLabels(false));
            row++;
        }

        return new DataSet(features, labels);
    }

    private static float map(int pos, boolean canProceed) {
        if (CAN_PROCEED_POS == pos) {
            return canProceed ? 1.0f : 0.0f;
        }

        return canProceed ? 0.0f : 1.0f;
    }

    private static float[] features(FlatOrderBook book) {
        return Floats.concat(book.getHistogramBuy(), book.getHistogramSell());
    }
}
