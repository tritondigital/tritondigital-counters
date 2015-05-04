package com.tritondigital.counters

import scala.concurrent.Future

/**
 * Main API for the metrics publisher. Implement this interface to provide more metrics.
 */
trait MetricsProvider {
  /**
   * Called by the publisher on regular interval.
   * Hook yourself here to provide more metrics.
   */
  def provide(): Future[Iterable[Metric]]
}
