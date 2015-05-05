package com.tritondigital.counters.logback

import akka.dispatch.Futures
import com.tritondigital.counters.{Logging, Metric, MetricsPublisher}

/**
 * This publisher simply publishes to LogBack by producing an INFO entry each time it is publishing.
 */
object LogPublisher extends MetricsPublisher with Logging {
  private val eol = "%n".format()
  private val completedFuture = Futures.successful(())

  def publish(metrics: Iterable[Metric]) = {
    log.info(s"Current metrics values: $eol" + metrics.mkString(eol)) // Publish all those metrics to the log
    completedFuture
  }
}
