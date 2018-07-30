package com.gtc.opportunity.trader.service.nnopportunity.solver.model;

import com.gtc.opportunity.trader.config.NnConfig;
import com.gtc.opportunity.trader.service.dto.FlatOrderBook;
import com.gtc.opportunity.trader.service.nnopportunity.dto.Snapshot;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.List;

import static com.gtc.opportunity.trader.service.nnopportunity.solver.model.FeatureMapper.CAN_PROCEED_POS;

/**
 * Created by Valentyn Berezin on 29.07.18.
 */
@Slf4j
public class NnModelPredict {

    private static final int N_INPUT_FEATURES = 20;
    private static final int N_CLASSES = 2;

    @Getter
    private final long creationTimestamp;

    private final FeatureMapper featureMapper;
    private final NnConfig nnConfig;
    private final MultiLayerNetwork model;

    NnModelPredict(NnConfig config, Snapshot snapshot, FeatureMapper mapper) throws TrainingFailed {
        this.nnConfig = config;
        this.model = new MultiLayerNetwork(buildModelConfig(config));
        this.model.init();
        this.featureMapper = mapper;
        this.creationTimestamp = System.currentTimeMillis();
        Splitter split = new Splitter(config, snapshot);
        trainModel(split);
        assesModel(split);
    }

    public boolean canProceed(FlatOrderBook book) {
        INDArray results = model.output(featureMapper.extractFeatures(book));
        return results.getFloat(CAN_PROCEED_POS) > nnConfig.getTruthThreshold();
    }

    @SneakyThrows
    private void trainModel(Splitter splitter) {
        log.info("Training model");
        DataSet trainData = featureMapper.extract(splitter.getProceedTrain(), splitter.getNoopTrain());
        for (int i = 0; i < nnConfig.getIterations(); ++i) {
            model.fit(trainData);
        }
        log.info("Done training model");
    }

    private void assesModel(Splitter splitter) throws TrainingFailed {
        Evaluation eval = new Evaluation(N_CLASSES);
        asses(eval, splitter.getNoopTest(), false);
        asses(eval, splitter.getProceedTest(), true);

        if (eval.falsePositiveRate(CAN_PROCEED_POS) > nnConfig.getProceedFalsePositive()) {
            throw new TrainingFailed();
        }
    }

    private void asses(Evaluation eval, List<FlatOrderBook> books, boolean isProceed) {
        books.stream().map(featureMapper::extractFeatures).forEach(features ->
                eval.eval(model.output(features), featureMapper.extractLabels(isProceed))
        );
    }

    private static MultiLayerConfiguration buildModelConfig(NnConfig cfg) {
        NeuralNetConfiguration.ListBuilder builder = new NeuralNetConfiguration.Builder()
                .seed(System.currentTimeMillis())
                .optimizationAlgo(OptimizationAlgorithm.CONJUGATE_GRADIENT)
                .updater(new Nesterovs(cfg.getMomentum()))
                .biasInit(0.0)
                .weightInit(WeightInit.XAVIER)
                .l2(cfg.getL2())
                .list();

        builder.layer(0, new DenseLayer.Builder()
                .nIn(N_INPUT_FEATURES)
                .nOut(cfg.getLayerDim())
                .activation(Activation.SIGMOID)
                .build());

        for (int i = 1; i <= cfg.getLayers(); ++i) {
            builder.layer(i, new DenseLayer.Builder()
                    .nIn(cfg.getLayerDim())
                    .nOut(cfg.getLayerDim())
                    .activation(Activation.SIGMOID)
                    .build());
        }

        builder.layer(cfg.getLayers() + 1, new OutputLayer.Builder(LossFunctions.LossFunction.XENT)
                .nIn(cfg.getLayerDim())
                .nOut(N_CLASSES)
                .activation(Activation.SOFTMAX)
                .build());

        return builder.build();
    }

    @Getter
    private static class Splitter {

        private final List<FlatOrderBook> proceedTrain;
        private final List<FlatOrderBook> noopTrain;
        private final List<FlatOrderBook> proceedTest;
        private final List<FlatOrderBook> noopTest;

        Splitter(NnConfig config, Snapshot snapshot) {
            int splitTrainProceed = (int) (config.getTrainRelativeSize() * snapshot.getProceedLabel().size());
            int splitTrainNoop = (int) (config.getTrainRelativeSize() * snapshot.getNoopLabel().size());
            proceedTrain = snapshot.getProceedLabel().subList(0, splitTrainProceed);
            noopTrain = snapshot.getNoopLabel().subList(0, splitTrainNoop);
            proceedTest = snapshot.getProceedLabel().subList(splitTrainProceed, snapshot.getProceedLabel().size());
            noopTest = snapshot.getNoopLabel().subList(splitTrainNoop, snapshot.getNoopLabel().size());
        }
    }

    public static class TrainingFailed extends Exception {
    }
}
