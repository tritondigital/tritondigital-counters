package com.tritondigital.counters.akka

import akka.actor.UntypedActor
import com.tritondigital.counters.Metrics

/**
 * Extends this class to monitor your Java actor.
 */
abstract class ActorWithMetrics(protected val metrics: Metrics) extends UntypedActor with ActorMetricsBase {
  @throws(classOf[Exception])
  def wrappedOnReceive(message: Any): Unit

  @throws(classOf[Exception])
  override def onReceive(message: Any) {
    monitoredReceive(message) {
      wrappedOnReceive(message)
    }
  }
}
