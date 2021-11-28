package com.price.processor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PriceThrottler implements PriceProcessor {
    private static final int MAX_PAIRS_COUNT = 200;

    final Map<PriceProcessor, PriceProcessorHelper> listeners = new ConcurrentHashMap<>();

    @Override
    public void onPrice(String ccyPair, double rate) {
        listeners.values()
                .stream()
                .parallel()
                .forEach(listener -> listener.addRate(ccyPair, rate));
    }

    @Override
    public void subscribe(PriceProcessor priceProcessor) {
        listeners.put(priceProcessor, new PriceProcessorHelper(priceProcessor));
    }

    @Override
    public void unsubscribe(PriceProcessor priceProcessor) {
        listeners.remove(priceProcessor);
    }

    private static class PriceProcessorHelper {
        List<RateValue> values = Collections.synchronizedList(new ArrayList<>(MAX_PAIRS_COUNT));
        private final PriceProcessor priceProcessor;
        private volatile boolean isOnDuty = false;

        PriceProcessorHelper(PriceProcessor priceProcessor) {
            this.priceProcessor = priceProcessor;
        }

        public void addRate(final String ccyPair, final double rate) {
            Optional<RateValue> oldValues = values.stream().filter(a -> a.isPair(ccyPair)).findAny();
            if (oldValues.isPresent()) {
                oldValues.get().setRate(rate);
            } else {
                values.add(new RateValue(ccyPair, rate));
            }

            if (!isOnDuty) {
                isOnDuty = true;
                new Updater().start();
            }
        }

        private class Updater extends Thread {
            public void run() {
                while (!values.isEmpty()) {
                    RateValue item = values.remove(0);
                    priceProcessor.onPrice(item.getPair(), item.getRate());
                }

                isOnDuty = false;
            }
        }

        private static class RateValue {
            private final String pair;
            private double rate;

            private RateValue(final String pair, final double rate) {
                this.pair = pair;
                this.rate = rate;
            }

            public String getPair() {
                return pair;
            }

            public double getRate() {
                return rate;
            }

            public void setRate(final double rate) {
                this.rate = rate;
            }

            public boolean isPair(final String pair) {
                return this.pair.equals(pair);
            }
        }
    }
}
