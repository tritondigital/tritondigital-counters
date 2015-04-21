package com.tritondigital.counters;

import scala.concurrent.Future;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Aggregate metrics during an interval.
 * Publish metrics aggregated value at the end of the interval.
 */
public interface Metrics {
    /** Sets the value of the given gauge to the given value **/
    void setGaugeValue(String name, double value, Tag... tags);
    /** Increments the given metric by the given amount **/
    void incrementCounter(String name, long incrementBy, Tag... tags);
    /** Increments the given metric by 1 **/
    void incrementCounter(String name, Tag... tags);
    /** Updates the given timer with a new duration **/
    void updateTimer(String name, long duration, TimeUnit unit, Tag... tags);
    /** Updates the given timer with a new duration calculated from the given start in milliseconds **/
    void updateTimer(String name, long startMillis, Tag... tags);
    /** Marks the given meter **/
    void markMeter(String name, Tag... tags);
    /** Marks the given meter **/
    void markMeter(String name, long times, Tag... tags);
    /** Updates the given histogram with a new value **/
    void updateHistogram(String name, long value, Tag... tags);
    /** Returns for each distinct combination the sum of _local_ (not server's) values for the given metric **/
    Future<Map<List<Tag>, Double>> sumQuery(String name, String... tagKeyCombination);
}
