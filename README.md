<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [Triton Digital Counters](#triton-digital-counters)
  - [Features](#features)
  - [Concepts](#concepts)
  - [Behaviour](#behaviour)
  - [Getting started](#getting-started)
  - [Configuration](#configuration)
  - [Publishing to Datadog](#publishing-to-datadog)
    - [Datadog configuration](#datadog-configuration)
    - [Datadog on Docker](#datadog-on-docker)
    - [Limiting the number of metrics sent to Datadog](#limiting-the-number-of-metrics-sent-to-datadog)
  - [Publishing to Logback](#publishing-to-logback)
  - [Publishing new metrics](#publishing-new-metrics)
  - [Built-in metrics](#built-in-metrics)
    - [JMX basic metrics](#jmx-basic-metrics)
    - [Monitoring Logback](#monitoring-logback)
  - [Appending global tags to all metrics](#appending-global-tags-to-all-metrics)
  - [Providing custom gauges at publication time](#providing-custom-gauges-at-publication-time)
  - [Publishing Codahale Metrics](#publishing-codahale-metrics)
  - [Monitoring Akka actors](#monitoring-akka-actors)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Triton Digital Counters

tritondigital-counters allows applications to publish metrics in monitoring systems as easy as logging, from Scala, Java, or any JVM language, with minimal overhead.
 
Currently, the library supports the following integrations:
 
* Datadog
* Logback
* Docker
* Akka
* Codahale metrics

tritondigital-counters act in a similar spirit to the Codahale metrics, but is adding the publication to a timeseries database, and contextual tags that makes systems like OpenTSDB and Datadog so powerfull for monitoring applications.

## Features

tritondigital-counters supports publication to the following back-ends:

* Datadog
* Logback logs

tritondigital-counters can monitor the following part of your application out of the box:
 
* application lifecycle (start / stop)
* Logback ERROR and WARNING log entries
* Akka actors
* Codahale metrics

But the most important feature is it allows you to publish your own metrics easily.

## Concepts

Most of the concepts are identical to Codahale metrics one. Please refer to the [Codahale metrics documentation](https://dropwizard.github.io/metrics/3.1.0/manual/core/) for what constitute a gauge, a counter, a marker, an histogram, and a timer.

tritondigital-counters adds the notion of tags, inherited from the OpenTSDB or Datadog tags. A tag is a bit of metadata associated with a metrics that allows for fine grained queries when retrieving timeseries. Example of tags are:

* application name
* host name
* response status code
* etc...

## Behaviour

During normal conditions, tritondigital-counters will publish a snapshot of the metrics every publication interval (15 seconds by default). 

The internal aggregation of metrics is based on Codahale Metrics, so it is fast. Although the metrics are published every few seconds, your application can update them thousends of time per seconds. 

tritondigital-counters tries to be a good citizen. It is publishing metrics that have stable values less often (10 mins instead of the regular publication interval). This makes monitoring a lot of stable metrics not an issue.

When the remote timeseries database is not responding, it will drop the current metrics publication round, and tries to reconnect after the next publication interval.
 
If for whatever reason, publication takes longer than the interval between publication, some publication rounds will be skipped. This is making tritondigital-counters to peaks where a lot of metrics need to be published.

Publication time is jittered, so as to avoid resonance phenomena that could harm the target timeseries database.

## Getting started

Start by adding the dependency to your project:

_For Maven:_

```xml
    <dependency>
        <groupId>com.tritondigital</groupId>
        <artifactId>tritondigital-counters_2.10</artifactId>
        <version>1.0.0</version>
    </dependency>
```
    
_For SBT:_

```scala
    libraryDependencies += "com.tritondigital" %% "tritondigital-counters" % "1.0.0"
```

You can then start the monitoring process. tritondigital-counters is using akka-io for fast, efficient, and non blocking IO, so you will need to pass it an actor system:

_In Java:_

```java
    ActorSystem actorSystem = ActorSystem.create();
    Metrics metrics = new MetricsSystemBuilder(actorSystem)
        .publishToDatadog()
        .build();
```

_In Scala:_

```scala
    val actorSystem = ActorSystem()
    val metrics = new MetricsSystemBuilder(actorSystem)
        .publishToDatadog
        .build
```

## Configuration

tritondigital-counters is using the [typesafe config](https://github.com/typesafehub/config). You can look at the documentation and default config values in src/main/resources/reference.conf. 

## Publishing to Datadog

For activating the publication to datadog:

```java
    Metrics metrics = new MetricsSystemBuilder(actorSystem)
        .publishToDatadog()
        .build();
```

### Datadog configuration

You will need datadog agent to run locally on the host. If running elsewhere, you can adjust the following configuration settings:

* `tritondigital_counters.datadog.host`: the datadog agent host name or ip (default: localhost)
* `tritondigital_counters.datadog.port`: the datadog agent host name or ip (default: 8125)


### Datadog on Docker

If you are running your application and the Datadog agent in Docker containers, you simply need to link the agent using the "datadog" alias:

```bash
    docker run -dt --link my-datadog-container-name:datadog my-app
```

### Limiting the number of metrics sent to Datadog

Datadog encourages you to think hard which metrics you send to their system. You can adjust which metrics you send to them by implementing a filter:

_In Java:_

```java
    import com.tritondigital.counters.MetricFilter

    public class MyDGFilter extends MetricFilter {
      public Iterable[Metric] filter(metrics: Iterable[Metric]) {
        // Do your filtering here
      }
    }

    ...

    Metrics metrics = new MetricsSystemBuilder(actorSystem)
        .publishToDatadog()
        .withDatadogFilter(new MyDGFilter())
        .build();
```

_In Scala:_

```scala
    import com.tritondigital.counters.MetricFilter

    class MyDGFilter extends ScalaMetricFilter {
      def filter(metrics: Iterable[Metric]) = {
        // Do your filtering here
      }
    }

    ...

    Metrics metrics = new MetricsSystemBuilder(actorSystem)
        .publishToDatadog()
        .withDatadogFilter(new MyDGFilter())
        .build();
```

If all you need to do is have a white list of metrics to send, a filter is readily useable:

```java
    Metrics metrics = new MetricsSystemBuilder(actorSystem)
        .publishToDatadog()
        .withDatadogFilter(new MetricPrefixFilter(new String[]{ "log.", "my-app." }))
        .build();
```

## Publishing to Logback

For debugging or in the tests, it can be usefull to publish the metrics value in your application log:

```java
    Metrics metrics = new MetricsSystemBuilder(actorSystem)
        .publishToLogback()
        .build();
```

## Publishing new metrics

You can now publish various kind of metrics:

```java
    // Counters
    metrics.incrementCounter("some.metric.without.tags");
    
    // Meters
    metrics.markMeter("some.meter");
    
    // Histograms
    metrics.updateHistogram("some.histogram", 88);
    
    // Timers
    metrics.updateTimer("some.timer", 67, TimeUnit.MILLIS);
    
    // Gauges
    metrics.setGaugeValue("some.gauge", 1.45);
    
    // All methods support contextual tags
    metrics.incrementCounter("some.metric.with.tags", new Tag("custom-tag-1", "value"), new Tag("custom-tag-2", "value"));
    metrics.incrementCounter("some.metric.with.tags", 10, new Tag("custom-tag-1", "value"), new Tag("custom-tag-2", "value"));
```

That being said, timeseries databases only support gauges. So tritondigital-counters is creating gauges out of those high level metrics. Here are the gauges actually published by tritondigital-counters:
 
``Gauges``

The gauges are published as is.

``Counters``

The counters are published as is.

``Meters``

The following gauges are published for meters:

* Suffix: ``.count``
* The total number of time the meter has been marked (= how many data points)


* Suffix: ``.m1``
* The 1 minute moving average

``Histograms``

The following gauges are published for histograms:

* Suffix: ``.count``
* The total number of time the histogram has been updated (= how many data points)


* Suffix: ``.median``
* The median value (.50th quantile)


* Suffix: ``.p75``
* .75th quantile


* Suffix: ``.p99``
* .99th quantile

``Timers``

A timer is an histogram and a meter, and thus the gauges for both meters and histograms are published. The values' unit are milliseconds.

## Built-in metrics

Out of the box, some metrics are published.

### JMX basic metrics

* ``java.heap.usage``: heap used in MB, as given by the [`java.lang.management.MemoryMXBean`](https://docs.oracle.com/javase/8/docs/api/java/lang/management/MemoryMXBean.html)
* ``java.cpu.load``: system load, as given by the [`java.lang.management.OperatingSystemMXBean`](https://docs.oracle.com/javase/8/docs/api/java/lang/management/OperatingSystemMXBean.html)

### Monitoring Logback

If you want tritondigital-counters to monitor your logs, you can activate it by:

```java
    Metrics metrics = new MetricsSystemBuilder(actorSystem)
        .monitorLogback()
        .build();
```

This will yield the following metrics:

* ``log.error.count`` and ``log.error.m1``: errors logged in Logback. 2 tags are given: `exception` which is the exception class anem that have been logged (if any) and `inner` which is the inner exception class name of that exception (if any). 
* ``log.warn.count`` and ``log.warn.m1``: warnings logged in Logback. 2 tags are given: `exception` which is the exception class anem that have been logged (if any) and `inner` which is the inner exception class name of that exception (if any). 

## Appending global tags to all metrics

```java
    Metrics metrics = new MetricsSystemBuilder(actorSystem)
        .addGlobalTag("my-app-tag-name", "my-app-tag-value")
        .build();
```

## Providing custom gauges at publication time

Sometimes, you have access to gauges at all time, and all you need is to "plug" those values at publication time. You can achieve that by implimenting a custom metrics provider:
 
```java
    import com.tritondigital.counters.MetricsProvider
    import scala.concurrent.Future

    public class MyCustomProvider implements MetricsProvider {
      public Future<Metric[]> provide() {
        // Return your metrics here
      }
    }

    ...

    Metrics metrics = new MetricsSystemBuilder(actorSystem)
        .addProvider(new MyCustomProvider())
        .build();
```

## Publishing Codahale Metrics

A provider exists for Codahale Metrics registry. You don't need a registry yourself (this is redundant with the com.tritondigital.counters.Metrics), but if an other library is providing one, you can make tritondigital-counters publish this library's metrics:

```java

    MetricRegistry registry = ...; // Get the Codahale's registry
    Metrics metrics = new MetricsSystemBuilder(actorSystem)
        .addProvider(new CodahaleMetricsProvider(actorSystem, registry))
        .build();
```

## Monitoring Akka actors

You can monitor your Akka actors. For that, a utility wrapping the receive method is provided.

_In Java:_

```java
    import com.tritondigital.counters.Metrics;
    import com.tritondigital.counters.akka.ActorWithMetrics;
    
    public class MyActor extends ActorWithMetrics {
        public MyActor(Metrics metrics) {
            super(metrics);
        }
        @Override
        public void wrappedOnReceive(Object message) throws Exception {
            // Your logic that you used to place in onReceive()
        }
    }
```

_In Scala:_

```scala
    import com.tritondigital.counters.Metrics
    import com.tritondigital.counters.akka.ActorMetrics
    
    class MyActor(val metrics: Metrics) extends Actor with ActorMetrics {
      def wrappedReceive = {
        case message =>
          // Your logic that you used to place in receive {}
      } 
    }
```
