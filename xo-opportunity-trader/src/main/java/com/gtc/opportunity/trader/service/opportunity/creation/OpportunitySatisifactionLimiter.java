package com.gtc.opportunity.trader.service.opportunity.creation;

import com.gtc.opportunity.trader.cqe.domain.FullCrossMarketOpportunity;
import com.gtc.opportunity.trader.domain.ClientConfig;
import com.gtc.opportunity.trader.domain.Wallet;
import com.gtc.opportunity.trader.repository.WalletRepository;
import com.gtc.opportunity.trader.service.opportunity.creation.precision.dto.XoTradeCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.stream.Stream;

import static com.gtc.opportunity.trader.service.opportunity.creation.fastexception.Reason.LOW_BAL;
import static com.gtc.opportunity.trader.service.opportunity.creation.fastexception.Reason.LOW_PROFIT;
import static com.gtc.opportunity.trader.service.opportunity.creation.fastexception.Reason.MAX_LT_MIN;

/**
 * Approximate price changes:
 * BTC 99th percentile 0.04623860090727638 %/s
 * XRP 99th percentile 0.06939927219604214 %/s
 * ETH 99th percentile 0.08884766941679999 %/s
 * NEO 99th percentile 0.12605719134495022 %/s
 * LTC 99th percentile 0.03222509298093853 %/s
 * XMR 99th percentile 4.689832415428314E-4 %/s
 * DASH 99th percentile 0.012959324943600676 %/s
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpportunitySatisifactionLimiter {

    private final WalletRepository walletRepository;
    private final OpportunityMapperFactory mapperFactory;

    /**
     * We assume that price can get away from our desired value due to delays up to n %
     * from current best in worst case which we validate and it is profitable enough.
     * Also there is a safety check with amount - safety margin amount which multiplies required amounts,
     * this means if we have safety margin 10% - we will require that at given price there exists 10% more
     * amount than that we are going to request. I.e. we are about to place order with 10 BTC, so our price
     * should satisfy 11BTC with safety margin 10%.
     */
    @Transactional
    public XoTradeCondition calculateAmount(ClientConfig from, ClientConfig to,
                                            FullCrossMarketOpportunity opportunity) {
        OpportunityMapperFactory.MappedOpp opp = mapperFactory.map(opportunity, from, to);

        // stage one - check that worst-case deviated prices do have 'some' amount to be satisfied
        double safetyPriceCoef = calculateSafetyMarginPricePct(from, to) / 100.0;

        // stage two - check that worst-case deviated prices are sufficient to give profit
        double marketSellPrice = opp.marketToBestSellPrice() * (1.0 + safetyPriceCoef);
        double marketBuyPrice = opp.marketFromBestBuyPrice() * (1.0 - safetyPriceCoef);

        double fromCharge = (1.0 - from.getTradeChargeRatePct().doubleValue() / 100.0);
        double tradeFeeLoss = fromCharge * (1.0 - to.getTradeChargeRatePct().doubleValue() / 100.0);
        double profitPct = marketBuyPrice / marketSellPrice * tradeFeeLoss * 100 - 100.0;
        double requiredProfit = calculateRequiredProfitabilityPct(from, to);

        Checker.validateAtLeast(LOW_PROFIT, profitPct, requiredProfit);

        // balance pre-check
        BigDecimal minBal = walletsCapacity(from, to, BigDecimal.valueOf(marketSellPrice));
        double minSellAmount = calculateMinAmountWithNotional(to, marketSellPrice);
        double minBuyAmount = calculateMinAmountWithNotional(from, marketBuyPrice);

        Checker.validateAtLeast(LOW_BAL, minBal.doubleValue(), minSellAmount);
        Checker.validateAtLeast(LOW_BAL, minBal.doubleValue(), minBuyAmount);

        // precise check - using histogram and worst case prices try to find solution with minimum req. profit
        double maxSellAmount = calculateMaxAmount(to, minBal).doubleValue();
        double maxBuyAmount = calculateMaxAmount(from, minBal).doubleValue();

        Checker.validateAtLeast(MAX_LT_MIN, maxSellAmount, minSellAmount);
        Checker.validateAtLeast(MAX_LT_MIN, maxBuyAmount, minBuyAmount);

        return XoTradeCondition.builder()
                .key(from.getClient().getName() + to.getClient().getName() + from.getCurrency().getCode() +
                        from.getCurrencyTo().getCode())
                .permits((double) Math.min(
                        from.getXoConfig().getMaxSolveRatePerS(),
                        to.getXoConfig().getMaxSolveRatePerS()))
                .sellTo(opp.marketToSellHistogram())
                .buyFrom(opp.marketFromBuyHistogram())
                .minToSellAmount(minSellAmount)
                .maxToSellAmount(maxSellAmount)
                .minFromBuyAmount(minBuyAmount)
                .maxFromBuyAmount(maxBuyAmount)
                .minToSellPrice(marketSellPrice)
                .maxFromBuyPrice(marketBuyPrice)
                .amountSafetyFromCoef(amountSafetyCoef(from))
                .amountSafetyToCoef(amountSafetyCoef(to))
                .lossFromCoef(lossCoef(from))
                .lossToCoef(lossCoef(to))
                .stepFromPricePow10(scalePrice(from))
                .stepToPricePow10(scalePrice(to))
                .stepFromAmountPow10(scaleAmount(from))
                .stepToAmountPow10(scaleAmount(to))
                .requiredProfitCoef(BigDecimal.ONE.add(BigDecimal.valueOf(requiredProfit).movePointLeft(2)))
                .maxSolveTimeMs(Math.min(
                        from.getXoConfig().getMaxSolveTimeMs(),
                        to.getXoConfig().getMaxSolveTimeMs()))
                .build();
    }

    private BigDecimal walletsCapacity(ClientConfig from, ClientConfig to, BigDecimal price) {
        Wallet fromWallet = walletRepository.findByClientAndCurrency(from.getClient(), from.getCurrency())
                .orElseThrow(() -> new IllegalStateException("No from wallet"));
        Wallet toWallet = walletRepository.findByClientAndCurrency(to.getClient(), to.getCurrencyTo())
                .orElseThrow(() -> new IllegalStateException("No to wallet"));

        return Stream.of(
                fromWallet.getBalance(),
                toWallet.getBalance().divide(price, MathContext.DECIMAL128)
        ).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
    }

    private BigDecimal calculateMaxAmount(ClientConfig cfg, BigDecimal balance) {
        return Stream.of(
                nvl(cfg.getMaxOrder()),
                balance
        ).min(BigDecimal::compareTo).orElseThrow(IllegalStateException::new);
    }

    private static BigDecimal nvl(BigDecimal value) {
        return null == value ? BigDecimal.ZERO : value;
    }

    private double calculateSafetyMarginPricePct(ClientConfig from, ClientConfig to) {
        return Stream.of(to, from)
                .map(it -> it.getXoConfig().getSafetyMarginPricePct().doubleValue())
                .max(Double::compareTo)
                .orElseThrow(IllegalStateException::new);
    }

    private double calculateRequiredProfitabilityPct(ClientConfig from, ClientConfig to) {
        return Stream.of(to, from)
                .mapToDouble(it -> it.getXoConfig().getRequiredProfitablityPct().doubleValue())
                .max()
                .orElseThrow(IllegalStateException::new);
    }

    private double calculateMinAmountWithNotional(ClientConfig cfg, double price) {
        if (null == cfg.getMinOrderInToCurrency()) {
            return cfg.getMinOrder().doubleValue();
        }

        return Math.max(cfg.getMinOrderInToCurrency().doubleValue() / price, cfg.getMinOrder().doubleValue());
    }

    private BigDecimal lossCoef(ClientConfig config) {
        return BigDecimal.ONE.subtract(config.getTradeChargeRatePct().movePointLeft(2));
    }

    private BigDecimal scalePrice(ClientConfig config) {
        return BigDecimal.ONE.movePointLeft(config.getScalePrice());
    }

    private BigDecimal scaleAmount(ClientConfig config) {
        return BigDecimal.ONE.movePointLeft(config.getScaleAmount());
    }

    private BigDecimal amountSafetyCoef(ClientConfig config) {
        return BigDecimal.ONE.add(config.getXoConfig().getSafetyMarginAmountPct().movePointLeft(2));
    }
}
