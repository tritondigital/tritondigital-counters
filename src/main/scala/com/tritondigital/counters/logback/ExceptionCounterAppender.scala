package com.tritondigital.counters.logback

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.tritondigital.counters.{Metrics, Tag}

/**
 * A logback appender providing metrics on ERROR and WARN events.
 */
class ExceptionCounterAppender(aggregator: Metrics) extends AppenderBase[ILoggingEvent] {
  def append(event: ILoggingEvent) {
    event.getLevel match {
          case Level.ERROR => increment("log.error", event)
          case Level.WARN => increment("log.warn", event)
          case _ => ()
    }
  }

  private def increment(name: String, event: ILoggingEvent) {
    val (ex, inner) =
      if (event.getThrowableProxy != null) {
        val ex = cleanClassName(event.getThrowableProxy.getClassName)
        if (event.getThrowableProxy.getCause != null)
          (ex, cleanClassName(event.getThrowableProxy.getCause.getClassName))
        else
          (ex, "none")
      }
      else {
      ("none", "none")
    }

    aggregator.markMeter(name, Tag("exception", ex), Tag("inner", inner))
  }

  private def cleanClassName(className: String) = {
    className.replace('$','/')
  }
}
