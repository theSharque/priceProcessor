package com.price.processor;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

public class PriceThrottlerTest {
    private static final int DEFAULT_TIMEOUT = 10;
    private static final int MONKEY_TIMEOUT = 3;
    private static final List<String> pairs = Arrays.asList("EURUSD", "USDJPY", "EURRUB", "EURJPY", "RUBJPY");

    private final TestPriceProcessor slowPriceProcessor = new TestPriceProcessor(100, "slow");
    private final TestPriceProcessor midPriceProcessor = new TestPriceProcessor(35, "Midl");
    private final TestPriceProcessor fastPriceProcessor = new TestPriceProcessor(1, "Fast");

    @Test
    public void subscribeAndUnsubscribe() {
        PriceThrottler priceThrottleProcessor = new PriceThrottler();

        priceThrottleProcessor.subscribe(slowPriceProcessor);
        priceThrottleProcessor.subscribe(midPriceProcessor);
        priceThrottleProcessor.subscribe(fastPriceProcessor);

        assertThat(priceThrottleProcessor.listeners.size(), equalTo(3));

        priceThrottleProcessor.unsubscribe(slowPriceProcessor);
        priceThrottleProcessor.unsubscribe(midPriceProcessor);
        priceThrottleProcessor.unsubscribe(fastPriceProcessor);

        assertThat(priceThrottleProcessor.listeners.size(), equalTo(0));
    }

    @Test
    public void incorrectSubscribe() {
        PriceThrottler priceThrottleProcessor = new PriceThrottler();

        priceThrottleProcessor.subscribe(fastPriceProcessor);
        priceThrottleProcessor.subscribe(fastPriceProcessor);
        priceThrottleProcessor.subscribe(fastPriceProcessor);

        assertThat(priceThrottleProcessor.listeners.size(), equalTo(1));
    }

    @Test
    public void incorrectUnsubscribe() {
        PriceThrottler priceThrottleProcessor = new PriceThrottler();

        priceThrottleProcessor.subscribe(fastPriceProcessor);
        priceThrottleProcessor.unsubscribe(fastPriceProcessor);
        priceThrottleProcessor.unsubscribe(fastPriceProcessor);

        assertThat(priceThrottleProcessor.listeners.size(), equalTo(0));
    }

    @Test
    public void onPriceSimple() {
        PriceThrottler priceThrottleProcessor = new PriceThrottler();

        priceThrottleProcessor.subscribe(fastPriceProcessor);

        try {
            priceThrottleProcessor.onPrice("EURUSD", 1.1);
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat(fastPriceProcessor.values.size(), equalTo(1));
        assertThat(fastPriceProcessor.values.get("EURUSD"), equalTo(1.1));
    }

    @Test
    public void onPriceMultiThread() {
        PriceThrottler priceThrottleProcessor = new PriceThrottler();

        priceThrottleProcessor.subscribe(slowPriceProcessor);
        priceThrottleProcessor.subscribe(fastPriceProcessor);
        priceThrottleProcessor.subscribe(midPriceProcessor);

        try {
            priceThrottleProcessor.onPrice("EURUSD", 1.1);
            Thread.sleep(DEFAULT_TIMEOUT);
            priceThrottleProcessor.onPrice("EURRUB", 2.1);
            Thread.sleep(DEFAULT_TIMEOUT);
            priceThrottleProcessor.onPrice("USDJPY", 3.1);
            Thread.sleep(DEFAULT_TIMEOUT);
            priceThrottleProcessor.onPrice("EURUSD", 1.2);
            Thread.sleep(DEFAULT_TIMEOUT);
            priceThrottleProcessor.onPrice("EURUSD", 1.3);
            Thread.sleep(DEFAULT_TIMEOUT);
            priceThrottleProcessor.onPrice("EURRUB", 2.2);
            Thread.sleep(DEFAULT_TIMEOUT);
            priceThrottleProcessor.onPrice("EURUSD", 1.4);
            Thread.sleep(DEFAULT_TIMEOUT);
            priceThrottleProcessor.onPrice("EURUSD", 1.5);
            Thread.sleep(DEFAULT_TIMEOUT);
            priceThrottleProcessor.onPrice("EURUSD", 1.6);
            Thread.sleep(DEFAULT_TIMEOUT);
            priceThrottleProcessor.onPrice("USDJPY", 3.2);
            Thread.sleep(DEFAULT_TIMEOUT);
            priceThrottleProcessor.onPrice("EURUSD", 1.7);
            Thread.sleep(DEFAULT_TIMEOUT);
            priceThrottleProcessor.onPrice("EURUSD", 1.8);
            Thread.sleep(DEFAULT_TIMEOUT);

            // Just to make sure all slow instance is finished his job
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat(fastPriceProcessor.values.size(), equalTo(3));
        assertThat(midPriceProcessor.values.size(), equalTo(3));
        assertThat(slowPriceProcessor.values.size(), equalTo(3));

        assertThat(slowPriceProcessor.values.get("USDJPY"), equalTo(3.2));
        assertThat(slowPriceProcessor.values.get("EURUSD"), equalTo(1.8));
        assertThat(slowPriceProcessor.values.get("EURRUB"), equalTo(2.2));

        assertThat(midPriceProcessor.values.get("USDJPY"), equalTo(3.2));
        assertThat(midPriceProcessor.values.get("EURUSD"), equalTo(1.8));
        assertThat(midPriceProcessor.values.get("EURRUB"), equalTo(2.2));

        assertThat(fastPriceProcessor.values.get("USDJPY"), equalTo(3.2));
        assertThat(fastPriceProcessor.values.get("EURUSD"), equalTo(1.8));
        assertThat(fastPriceProcessor.values.get("EURRUB"), equalTo(2.2));
    }

    @Test
    public void onPriceMonkeyMultiThread() {
        PriceThrottler priceThrottleProcessor = new PriceThrottler();

        priceThrottleProcessor.subscribe(slowPriceProcessor);
        priceThrottleProcessor.subscribe(fastPriceProcessor);
        priceThrottleProcessor.subscribe(midPriceProcessor);

        try {
            for (int i = 1; i < 1000; ++i) {
                priceThrottleProcessor.onPrice(pairs.get(i % 5), ((double) Math.round(Math.random() * 10000)) / 100);
                Thread.sleep(MONKEY_TIMEOUT);
            }

            // Just to make sure all slow instance is finished his job
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat(fastPriceProcessor.values.size(), equalTo(5));
        assertThat(midPriceProcessor.values.size(), equalTo(5));
        assertThat(slowPriceProcessor.values.size(), equalTo(5));

        pairs.forEach(name -> {
            assertThat(fastPriceProcessor.values.get(name), equalTo(midPriceProcessor.values.get(name)));
            assertThat(fastPriceProcessor.values.get(name), equalTo(slowPriceProcessor.values.get(name)));
        });
    }
}