package com.gtc.opportunity.trader.service.command.gateway;

import com.google.common.collect.ImmutableMap;
import com.gtc.model.gateway.BaseMessage;
import com.gtc.model.gateway.data.OrderDto;
import com.gtc.model.gateway.data.OrderStatus;
import com.gtc.model.gateway.response.account.GetAllBalancesResponse;
import com.gtc.model.gateway.response.create.CreateOrderResponse;
import com.gtc.model.gateway.response.manage.GetOrderResponse;
import com.gtc.model.gateway.response.manage.ListOpenOrdersResponse;
import com.gtc.opportunity.trader.domain.Trade;
import com.gtc.opportunity.trader.service.trade.management.TradeEsbEventHandler;
import com.gtc.opportunity.trader.service.trade.management.WalletEsbEventHandler;
import com.newrelic.api.agent.Trace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Keep anything related to statemachine changes not async - it has problems.
 */
@Service
@RequiredArgsConstructor
public class WsGatewayResponseListener {

    private final Map<OrderStatus, BiConsumer<BaseMessage, OrderDto>> manageHandlers =
            ImmutableMap.<OrderStatus, BiConsumer<BaseMessage, OrderDto>>builder()
                    .put(OrderStatus.CANCELED, this::ackCancel)
                    .put(OrderStatus.EXPIRED, this::ackCancel)
                    .put(OrderStatus.FILLED, this::ackDone)
                    .put(OrderStatus.NEW, this::ackOrder)
                    .put(OrderStatus.PARTIALLY_FILLED, this::ackOrder)
                    .put(OrderStatus.REJECTED, this::ackError)
                    .put(OrderStatus.UNMAPPED, this::ackError)
                    .build();

    private final WalletEsbEventHandler walletEsbEventHandler;
    private final TradeEsbEventHandler esbEventHandler;

    @Trace(dispatcher = true)
    public void createOrder(CreateOrderResponse create) {
        if (!create.isExecuted()) {
            esbEventHandler.ackCreate(
                    create.getRequestOrderId(),
                    create.getId(),
                    OrderStatus.NEW.name(),
                    OrderStatus.NEW.name(),
                    new Trade.EsbKey(create.getOrderId(), create.getClientName())
            );

            return;
        }

        esbEventHandler.ackCreateAndDone(
                create.getRequestOrderId(),
                create.getId(),
                OrderStatus.FILLED.name(),
                OrderStatus.FILLED.name(),
                new Trade.EsbKey(create.getOrderId(), create.getClientName())
        );
    }

    @Trace(dispatcher = true)
    public void opened(ListOpenOrdersResponse response) {
        response.getOrders().forEach(it -> manageHandlers.get(it.getStatus()).accept(response, it));
    }

    @Trace(dispatcher = true)
    public void byId(GetOrderResponse response) {
        manageHandlers.get(response.getOrder().getStatus()).accept(response, response.getOrder());
    }

    @Trace(dispatcher = true)
    public void walletUpdate(GetAllBalancesResponse response) {
        walletEsbEventHandler.updateWallet(response);
    }

    private void ackOrder(BaseMessage msg, OrderDto dto) {
        esbEventHandler.ackOrder(
                new Trade.EsbKey(dto.getOrderId(), msg.getClientName()),
                msg.getId(),
                dto.getStatus().name(),
                dto.getStatusString(),
                dto.getSize(),
                dto.getPrice()
        );
    }

    private void ackDone(BaseMessage msg, OrderDto dto) {
        esbEventHandler.ackDone(
                new Trade.EsbKey(dto.getOrderId(), msg.getClientName()),
                msg.getId(),
                dto.getStatus().name(),
                dto.getStatusString()
        );
    }

    private void ackCancel(BaseMessage msg, OrderDto dto) {
        esbEventHandler.ackCancel(
                new Trade.EsbKey(dto.getOrderId(), msg.getClientName()),
                msg.getId(),
                dto.getStatus().name(),
                dto.getStatusString()
        );
    }

    private void ackError(BaseMessage msg, OrderDto dto) {
        esbEventHandler.ackError(
                new Trade.EsbKey(dto.getOrderId(), msg.getClientName()),
                msg.getId(),
                dto.getStatus().name() + "/" + dto.getStatusString()
        );
    }
}
