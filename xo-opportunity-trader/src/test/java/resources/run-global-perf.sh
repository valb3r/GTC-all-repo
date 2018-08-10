#!/bin/bash

max_gain=5

export GLOBAL_NN_TEST="true"
export START="2018-07-27T00:00:00"
export END="2018-07-28T23:59:59"
mkdir -p logs

for gain in $(seq 1 $max_gain);
do
	for level in $(seq 1 $max_gain);
	do
		export FUTURE_GAIN_PCT="0.$gain"
		export NOOP_THRESHOLD="1.00$level"
		echo "for gain $FUTURE_GAIN_PCT / level $NOOP_THRESHOLD"
		cd ../../../../..
		./gradlew clean :xo-opportunity-trader:test \
		 --tests com.gtc.opportunity.trader.service.nnopportunity.global.GlobalNnPerformanceTest.test \
		 &> "logs/gain_test_g"$gain"_l"$level".log"
	done
done
