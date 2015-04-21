package com.tritondigital.counters;

import com.tritondigital.counters.akka.ActorWithMetrics;

public class MyMonitoredActor extends ActorWithMetrics {
    public MyMonitoredActor(Metrics metrics) {
        super(metrics);
    }

    @Override
    public void wrappedOnReceive(Object message) throws Exception {
        // Your logic
    }
}
