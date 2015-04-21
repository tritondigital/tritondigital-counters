package com.tritondigital.counters.akka

import akka.actor.Actor
import com.tritondigital.counters.{Metrics, Tag}

trait ActorMetricsBase extends Actor {
  protected def metrics: Metrics
  private lazy val path = self.path.toStringWithoutAddress

  protected def monitoredReceive(message: Any)(receive: => Unit) {
    val start = System.currentTimeMillis()

    try {
      receive
    }
    finally {
      val msgClass =
        if (message == null)
          "null"
        else
          message.getClass.getSimpleName

      metrics.updateTimer("akka.actor.message", start, Tag("path", path), Tag("mclass", msgClass))
    }
  }
}
