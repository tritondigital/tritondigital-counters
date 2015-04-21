package com.tritondigital.counters.codahale

import com.codahale.metrics._
import com.tritondigital.counters.{DoubleValue, LongValue, Metric, Tag}

/**
 * Converts codahale statistics to actual Datadog metrics.
 */
private [counters] object CodahaleMetricsConverters {
  private [this] val Millis = 0.000001

  def gaugeToMetric(gauge: Gauge[_], name: String, tags: Seq[Tag] = Seq.empty[Tag]) =
    gauge.getValue match {
      case l: Long => List(Metric(name, l))
      case i: Int => List(Metric(name, LongValue(i)))
      case d: Double => List(Metric(name, d))
      case f: Float => List(Metric(name, DoubleValue(f)))
      case null => Nil
      case other => throw new IllegalArgumentException(s"Metrics to Datadog bridge does not support values of type ${other.getClass.getName} for metric $name.")
    }

  def counterToMetric(counter: Counter, name: String, tags: Seq[Tag] = Seq.empty[Tag]) =
    Metric(name, counter.getCount, tags)

  def histogramToMetrics(histogram: Histogram, name: String, tags: Seq[Tag] = Seq.empty[Tag]) = {
    val snapshot = histogram.getSnapshot

    List(
      Metric(name + ".count", histogram.getCount, tags),
//      Metric(name + ".min", snapshot.getMin, tags),
//      Metric(name + ".max", snapshot.getMax, tags),
//      Metric(name + ".mean", snapshot.getMean, tags),
      Metric(name + ".median", snapshot.getMedian, tags),
      Metric(name + ".p75", snapshot.get75thPercentile, tags),
//      Metric(name + ".p95", snapshot.get95thPercentile, tags),
      Metric(name + ".p99", snapshot.get99thPercentile, tags)
//      Metric(name + ".p999", snapshot.get999thPercentile, tags)
    )
  }

  def timerToMetrics(timer: Timer, name: String, tags: Seq[Tag] = Seq.empty[Tag]) = {
    val snapshot = timer.getSnapshot

    List(
      Metric(name + ".count", timer.getCount, tags),
      Metric(name + ".m1", timer.getOneMinuteRate, tags),
//      Metric(name + ".m5", timer.getFiveMinuteRate, tags),
//      Metric(name + ".m15", timer.getFifteenMinuteRate, tags),
//      Metric(name + ".min", snapshot.getMin * Millis, tags),
//      Metric(name + ".max", snapshot.getMax * Millis, tags),
//      Metric(name + ".mean", snapshot.getMean * Millis, tags),
      Metric(name + ".median", snapshot.getMedian * Millis, tags),
      Metric(name + ".p75", snapshot.get75thPercentile * Millis, tags),
//      Metric(name + ".p95", snapshot.get95thPercentile * Millis, tags),
      Metric(name + ".p99", snapshot.get99thPercentile * Millis, tags)
//      Metric(name + ".p999", snapshot.get999thPercentile * Millis, tags)
    )
  }

  def meterToMetrics(meter: Meter, name: String, tags: Seq[Tag] = Seq.empty[Tag]) = List(
    Metric(name + ".count", meter.getCount, tags),
    Metric(name + ".m1", meter.getOneMinuteRate, tags)
//    Metric(name + ".m5", meter.getFiveMinuteRate, tags),
//    Metric(name + ".m15", meter.getFifteenMinuteRate, tags)
  )
}
