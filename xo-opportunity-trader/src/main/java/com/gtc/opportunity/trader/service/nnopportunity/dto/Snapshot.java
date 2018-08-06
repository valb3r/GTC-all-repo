package com.gtc.opportunity.trader.service.nnopportunity.dto;

import com.gtc.opportunity.trader.service.dto.FlatOrderBook;
import com.gtc.opportunity.trader.service.nnopportunity.solver.Key;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
public class Snapshot {

    private final Key key;
    private final List<FlatOrderBook> noopLabel;
    private final List<FlatOrderBook> proceedLabel;
}
