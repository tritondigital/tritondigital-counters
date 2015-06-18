package com.tritondigital.counters

import java.util.concurrent.TimeUnit

import _root_.akka.actor.{Actor, ActorSystem, Props}
import _root_.akka.pattern.Patterns
import com.codahale.metrics._
import com.tritondigital.counters.codahale.CodahaleMetricsConverters

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal

/**
 * Thread safe implementation of Metrics, using Codahale for computing stats.
 */
class SimpleMetrics(actorSystem: ActorSystem) extends Metrics with MetricsProvider with Logging {
  import actorSystem.dispatcher

  private case object GetMetrics
  private case class SetGaugeValue(key: MetricKey, value: Double)
  private case class IncrementCounter(key: MetricKey, incrementBy: Long)
  private case class UpdateTimer(key: MetricKey, duration: Long, unit: TimeUnit)
  private case class MarkMeter(key: MetricKey, times: Long)
  private case class UpdateHistogram(key: MetricKey, value: Long)

  private val actor = actorSystem.actorOf(Props(new AggregatorActor))

  def setGaugeValue(name: String, value: Double, tags: Tag*): Unit = {
    actor ! SetGaugeValue(MetricKey(name, tags), value)
  }
  def incrementCounter(name: String, incrementBy: Long, tags: Tag*) {
    if (log.isTraceEnabled) log.trace(s"Incrementing $name")
    actor ! IncrementCounter(MetricKey(name, tags), incrementBy)
  }
  def incrementCounter(name: String, tags: Tag*) {
    incrementCounter(name, 1, tags: _*)
  }
  def updateTimer(name: String, duration: Long, unit: TimeUnit, tags: Tag*) {
    if (log.isTraceEnabled) log.trace(s"Updating $name")
    actor ! UpdateTimer(MetricKey(name, tags), duration, unit)
  }
  def updateTimer(name: String, startInMillis: Long, tags: Tag*) {
    updateTimer(name, System.currentTimeMillis() - startInMillis, TimeUnit.MILLISECONDS, tags: _*)
  }
  def markMeter(name: String, tags: Tag*) {
    markMeter(name, 1, tags: _*)
  }
  def markMeter(name: String, times: Long, tags: Tag*) {
    if (log.isTraceEnabled) log.trace(s"Marking $name")
    actor ! MarkMeter(MetricKey(name, tags), times)
  }
  def updateHistogram(name: String, value: Long, tags: Tag*) {
    if (log.isTraceEnabled) log.trace(s"Updating $name")
    actor ! UpdateHistogram(MetricKey(name, tags), value)
  }

  def provide =
    Patterns
      .ask(actor, GetMetrics, 60.second)
      .mapTo[Iterable[Metric]]
      .recover {
        case NonFatal(ex) =>
          log.warn("Took more than 60 seconds to gather metrics. You probably have too much metrics...", ex)
          Iterable.empty[Metric]
      }

  private class AggregatorActor extends Actor {
    private val gauges = collection.mutable.Map
      .empty[MetricKey, Double]
    private val counters = collection.mutable.Map
      .empty[MetricKey, Counter]
    private val meters = collection.mutable.Map
      .empty[MetricKey, Meter]
    private val histograms = collection.mutable.Map
      .empty[MetricKey, Histogram]
    private val timers = collection.mutable.Map
      .empty[MetricKey, Timer]

    def receive = {

      case SetGaugeValue(key, value) =>
        gauges += key -> value

      case IncrementCounter(key, incrementBy) =>
        getCounter(key).inc(incrementBy)
        if (log.isTraceEnabled) log.trace(s"Incremented $key")

      case UpdateTimer(key, duration, unit) =>
        getTimer(key).update(duration, unit)
        if (log.isTraceEnabled) log.trace(s"Updated $key")

      case MarkMeter(key, times) =>
        getMeter(key).mark(times)
        if (log.isTraceEnabled) log.trace(s"Marked $key")

      case UpdateHistogram(key, value) =>
        getHistogram(key).update(value)
        if (log.isTraceEnabled) log.trace(s"Updated $key")

      case GetMetrics =>
        sender ! toMetrics

    }

    private def getCounter(key: MetricKey) =
      if (!counters.contains(key)) {
        val newCounter = new Counter
        counters += key -> newCounter
        newCounter
      }
      else
        counters(key)

    private def getMeter(key: MetricKey) =
      if (!meters.contains(key)) {
        val newMeter = new Meter
        meters += key -> newMeter
        newMeter
      }
      else
        meters(key)

    private def getHistogram(key: MetricKey) =
      if (!histograms.contains(key)) {
        val newHistogram = new Histogram(new ExponentiallyDecayingReservoir)
        histograms += key -> newHistogram
        newHistogram
      }
      else
        histograms(key)

    private def getTimer(key: MetricKey) =
      if (!timers.contains(key)) {
        val newTimer = new Timer
        timers += key -> newTimer
        newTimer
      }
      else
        timers(key)

    private def toMetrics = {
      import CodahaleMetricsConverters._

      val gaugeMetrics = gauges
        .map { case (key, value) => Metric(key.name, value, key.tags) }
      val counterMetrics = counters
        .map { case (key, counter) => counterToMetric(counter, key.name, key.tags) }
      val meterMetrics = meters
        .map { case (key, meter) => meterToMetrics(meter, key.name, key.tags) }.flatten
      val histogramMetrics = histograms
        .map { case (key, histogram) => histogramToMetrics(histogram, key.name, key.tags) }.flatten
      val timerMetrics = timers
        .map { case (key, timer) => timerToMetrics(timer, key.name, key.tags) }.flatten

      gaugeMetrics ++ counterMetrics ++ meterMetrics ++ histogramMetrics ++ timerMetrics
    }
  }

}

