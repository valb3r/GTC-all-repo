package com.gtc.opportunity.trader.service.nnopportunity.solver.model;

import com.google.common.primitives.Floats;
import com.gtc.opportunity.trader.service.dto.FlatOrderBook;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
@Component
class FeatureMapper {

    public static final int CAN_PROCEED_POS = 0;

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

    INDArray extract(List<FlatOrderBook> canProceed, List<FlatOrderBook> noop) {
        NDArray result = new NDArray();
        canProceed.forEach(it -> result.addRowVector(extractFeatures(it).addRowVector(extractLabels(true))));
        noop.forEach(it -> result.addRowVector(extractFeatures(it).addRowVector(extractLabels(false))));
        return result;
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
