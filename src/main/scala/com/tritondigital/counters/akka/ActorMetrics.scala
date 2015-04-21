package com.tritondigital.counters.akka

import _root_.akka.actor.Actor

/**
 * Extends this trait to monitor your Scala actor.
 */
trait ActorMetrics extends Actor with ActorMetricsBase {
  def wrappedReceive: Receive

  override def receive = {
    case message =>
      monitoredReceive(message) {
        if (wrappedReceive.isDefinedAt(message))
          wrappedReceive(message)
        else
          unhandled(message)
      }
  }
}
