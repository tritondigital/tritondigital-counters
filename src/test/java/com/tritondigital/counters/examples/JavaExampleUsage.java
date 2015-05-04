package com.tritondigital.counters;

import akka.actor.ActorSystem;
import java.lang.Iterable;
import java.util.LinkedList;
import java.util.List;

public class JavaExampleUsage {
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create();
        Metrics metrics = new MetricsSystemBuilder(system)
            .addGlobalTag("my-app-tag-name", "my-app-tag-value")
            .withDatadogFilter(new DatadogMetricsFilter())
            .build();

        metrics.incrementCounter("a.b.c");

        system.shutdown();
        system.awaitTermination();
    }

    private static class DatadogMetricsFilter extends MetricFilter {
        @Override
        public Iterable<Metric> filter(Iterable<Metric> metrics) {
            List<Metric> resVal = new LinkedList<>();
            for(Metric m : metrics) {
                if (m.name().startsWith("myapp.mymetric.")) {
                    resVal.add(m);
                }
            }
            return resVal;
        }
    }
}
