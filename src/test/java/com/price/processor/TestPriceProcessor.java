package com.price.processor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class TestPriceProcessor implements PriceProcessor {
    private final int timeOut;
    private final String name;
    Map<String, Double> values = new HashMap<>();

    public TestPriceProcessor(int timeOut, String name) {
        this.timeOut = timeOut;
        this.name = name;
    }

    @Override
    public void onPrice(String ccyPair, double rate) {
        System.out.println(new Date().toString() + " New " + name + " rate begin " + ccyPair + ":" + rate);
        try {
            Thread.sleep(timeOut);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        values.put(ccyPair, rate);
        System.out.println(new Date().toString() + " " + name + " rates: " + values.entrySet().stream()
                .map(entry -> entry.getKey() + entry.getValue()).collect(Collectors.joining(",")));
    }

    @Override
    public void subscribe(PriceProcessor priceProcessor) {
    }

    @Override
    public void unsubscribe(PriceProcessor priceProcessor) {

    }
}
