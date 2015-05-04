package com.tritondigital.counters

import scala.concurrent.Future

/** Publishes some metrics somewhere. This is usually called by a scheduler on regular intervals. **/
trait MetricsPublisher {
  def publish(metrics: Iterable[Metric]): Future[Unit]
  /** Allows to close expensive resources when publication is halted for the time being. Those will need to be lazily re-opened next time publish is called. **/
  def pause()
}
