#!/bin/bash

if [ -z "$CLIENT_NAME" ]; then
    read -p "CLIENT_NAME: " CLIENT_NAME
    export CLIENT_NAME=$CLIENT_NAME
fi

if [ -z "$FROM" ]; then
    read -p "FROM: " FROM
    export FROM=$FROM
fi

if [ -z "$TO" ]; then
    read -p "TO: " TO
    export TO=$TO
fi

if [ -z "$MIN_ORDER" ]; then
    read -p "MIN_ORDER: " MIN_ORDER
    export MIN_ORDER=$MIN_ORDER
fi

if [ -z "$CHARGE_RATE_PCT" ]; then
    read -p "CHARGE_RATE_PCT (i.e. 0.1): " CHARGE_RATE_PCT
    export CHARGE_RATE_PCT=$CHARGE_RATE_PCT
fi

if [ -z "$SCALE_PRICE" ]; then
    read -p "SCALE_PRICE: " SCALE_PRICE
    export SCALE_PRICE=$SCALE_PRICE
fi

if [ -z "$SCALE_AMOUNT" ]; then
    read -p "SCALE_AMOUNT: " SCALE_AMOUNT
    export SCALE_AMOUNT=$SCALE_AMOUNT
fi

if [ -z "$START" ]; then
    read -p "START_DATE (iso i.e. 2018-08-01T00:00:00): " START
    export START=$START
fi

if [ -z "$END" ]; then
    read -p "END_DATE (iso i.e. 2018-08-01T00:00:00): " END
    export END=$END
fi

if [ -z "$MAX_GAIN" ]; then
    MAX_GAIN=5
fi

export SHOW_STREAMS="true"
export GLOBAL_NN_TEST="true"

# go to root dir
if [ ! -f gradlew ]; then
    cd ../../../..
fi

mkdir -p logs

for gain in $(seq 1 $MAX_GAIN);
do
	for level in $(seq 1 $(( MAX_GAIN * 2)));
	do
		export FUTURE_GAIN_PCT="0.$gain"
		export NOOP_THRESHOLD="1.00$level"
		echo "for gain $FUTURE_GAIN_PCT / level $NOOP_THRESHOLD"
		./gradlew clean :xo-opportunity-trader:test \
		 --tests com.gtc.opportunity.trader.service.nnopportunity.global.GlobalNnPerformanceTest.test \
		 &> "logs/gain_test_"$CLIENT_NAME"_"$FROM"_"$TO"_g"$gain"_l"$level".log"
	done
done
