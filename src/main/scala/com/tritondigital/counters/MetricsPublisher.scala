package com.tritondigital.counters

import scala.concurrent.Future

/** Publishes some metrics somewhere. This is usually called by a scheduler on regular intervals. **/
trait MetricsPublisher {
  def publish(metrics: Iterable[Metric]): Future[Unit]
}
