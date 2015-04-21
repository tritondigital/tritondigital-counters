package com.tritondigital.counters.codahale

import akka.actor.ActorSystem
import com.codahale.metrics.MetricRegistry
import com.tritondigital.counters.{Metric, MetricsProvider}

import scala.collection.JavaConverters._
import scala.concurrent.Future

class CodahaleMetricsProvider(system: ActorSystem, registry: MetricRegistry) extends MetricsProvider {
  require(system != null, "system must not be null")
  require(registry != null, "registry must not be null")

  import CodahaleMetricsConverters._
  import system.dispatcher

  def provide(): Future[Iterable[Metric]] = Future {
    val gauges = for {
      (name, gauge) <- registry.getGauges.asScala
    } yield gaugeToMetric(gauge, name)

    val counters = for {
      (name, counter) <- registry.getCounters.asScala
    } yield List(counterToMetric(counter, name))

    val histograms = for {
      (name, histogram) <- registry.getHistograms.asScala
    } yield histogramToMetrics(histogram, name)

    val timers = for {
      (name, timer) <- registry.getTimers.asScala
    } yield timerToMetrics(timer, name)

    val meters = for {
      (name, meter) <- registry.getMeters.asScala
    } yield meterToMetrics(meter, name)

    (gauges ++ counters ++ histograms ++ timers ++ meters).flatten
  }
}
