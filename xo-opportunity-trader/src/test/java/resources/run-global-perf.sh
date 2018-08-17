#!/bin/bash

max_gain=5

export CLIENT_NAME="binance"
export FROM="EOS"
export TO="BTC"
export GLOBAL_NN_TEST="true"
export START="2018-08-04T00:00:00"
export END="2018-08-11T23:59:59"

# go to root dir
if [ ! -f gradlew ]; then
    cd ../../../../..
fi

mkdir -p logs

for gain in $(seq 1 $max_gain);
do
	for level in $(seq 1 $(( max_gain * 2)));
	do
		export FUTURE_GAIN_PCT="0.$gain"
		export NOOP_THRESHOLD="1.00$level"
		echo "for gain $FUTURE_GAIN_PCT / level $NOOP_THRESHOLD"
		./gradlew clean :xo-opportunity-trader:test \
		 --tests com.gtc.opportunity.trader.service.nnopportunity.global.GlobalNnPerformanceTest.test \
		 &> "logs/gain_test_"$CLIENT_NAME"_"$FROM"_"$TO"_g"$gain"_l"$level".log"
	done
done
