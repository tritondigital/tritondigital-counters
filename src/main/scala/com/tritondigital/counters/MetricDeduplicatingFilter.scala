package com.tritondigital.counters

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config

import scala.annotation.tailrec
import scala.collection.mutable

/**
 * Typically you want to gather data about everything in your system.
 * This generates a lot of datapoints, the majority of which don't change very often over time (if ever).
 * However, you want fine-grained resolution when they do change. This filter remembers the last value and timestamp that was sent for all of the time series.
 * If the value doesn't change between sample intervals, it suppresses sending that datapoint.
 * Once the value does change (or 10 minutes have passed), it sends the last suppressed value and timestamp, plus the current value and timestamp.
 * In this way all of your graphs and such are correct. Deduplication typically reduces the number of datapoints TSD needs to collect by a large fraction.
 * This reduces network load and storage in the backend.
 */
class MetricDeduplicatingFilter(config: Config) extends Logging with ScalaMetricFilter {
  require(config != null, "config must not be null")

  private case class MetricData(var metric: Metric, var firstOccurence: Metric) {
    // When there is a new value, we don't want to republish the first occurence
    def previousValueToPublish =
      if (metric == firstOccurence) Seq()
      else Seq(metric)
  }
  private val metrics = collection.mutable.Map.empty[MetricKey, MetricData]
  private val timeLimit = config.getDuration("tritondigital_counters.deduplication_time_limit", TimeUnit.SECONDS)

  def filter(toDeduplicate: Iterable[Metric]): mutable.Buffer[Metric] = {
    @tailrec
    def deduplicate0(remaining: Iterator[Metric] = toDeduplicate.iterator,
                     acc: mutable.Buffer[Metric] = mutable.Buffer.empty[Metric]): mutable.Buffer[Metric] =
      if (!remaining.hasNext)
        acc
      else {
        val m = remaining.next()

        val newMetricsToPublish =
          if (metrics.contains(m.key)) {
            val data = metrics(m.key)

            val newMetrics = if (needPublish(m, data)) {
              val previousValueToPublish = data.previousValueToPublish
              data.firstOccurence = m
              // Value just changed, publish last one with previous value (if not first occurence, since this one is already published) and new one.
              previousValueToPublish :+ m
            }
            else if (needRepublish(m, data)) {
              data.firstOccurence = m
              // Value did not change, but enough time has passed. Publish.
              Seq(m)
            }
            else {
              // Value has not changed, don't publish the metric. Don't update firstOccurence since we want to know when to republish later.
              Seq()
            }

            data.metric = m

            newMetrics
          }
          else {
            metrics.put(m.key, MetricData(m, m))
            // New metric, publish it
            Seq(m)
          }

        deduplicate0(remaining, acc ++= newMetricsToPublish)
      }

    if (log.isDebugEnabled) log.debug(s"Deduplicating ${toDeduplicate.size} metrics...")
    val start = System.currentTimeMillis()
    val res = deduplicate0()
    if (log.isDebugEnabled) log.debug(s"Deduplicated metrics in ${System.currentTimeMillis() - start} ms. Size of metrics index is ${metrics.size}.")
    res
  }

  private def needPublish(m: Metric, data: MetricData) =
    m.value != data.metric.value // Has the value changed

  private def needRepublish(m: Metric, data: MetricData) =
    m.tstamp > data.firstOccurence.tstamp + timeLimit // Have 10 minutes passed?
}
