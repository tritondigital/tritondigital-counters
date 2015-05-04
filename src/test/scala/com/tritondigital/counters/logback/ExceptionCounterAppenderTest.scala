package com.tritondigital.counters.logback

import java.io.IOException

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.{LoggingEvent, ThrowableProxy}
import com.tritondigital.counters.{MarkCall, RecordingMetrics, Tag}
import org.scalatest.{Matchers, WordSpec}

class ExceptionCounterAppenderTest extends WordSpec with Matchers {
  val error1 = new LoggingEvent()
  error1.setLevel(Level.ERROR)
  val error2 = new LoggingEvent()
  error2.setLevel(Level.ERROR)
  error2.setThrowableProxy(new ThrowableProxy(new NullPointerException))
  val error3 = new LoggingEvent()
  error3.setLevel(Level.ERROR)
  error3.setThrowableProxy(new ThrowableProxy(new IOException(new NumberFormatException)))
  val error4 = new LoggingEvent()
  error4.setLevel(Level.ERROR)
  error4.setThrowableProxy(new ThrowableProxy(new InternalException))
  val warn1 = new LoggingEvent()
  warn1.setLevel(Level.WARN)
  val info1 = new LoggingEvent()
  info1.setLevel(Level.INFO)

  "The exception counter appender" should {
    "count errors" in withSut { (sut, metrics) =>
      sut.append(error1)
      sut.append(error1)

      metrics.markCalls shouldEqual Seq(
        MarkCall("log.error", 1, Seq(Tag("exception", "none"), Tag("inner", "none"))),
        MarkCall("log.error", 1, Seq(Tag("exception", "none"), Tag("inner", "none")))
      )
    }
    "count warnings" in withSut { (sut, metrics) =>
      sut.append(warn1)
      sut.append(warn1)

      metrics.markCalls shouldEqual Seq(
        MarkCall("log.warn", 1, Seq(Tag("exception", "none"), Tag("inner", "none"))),
        MarkCall("log.warn", 1, Seq(Tag("exception", "none"), Tag("inner", "none")))
      )
    }
    "not count other levels" in withSut { (sut, metrics) =>
      sut.append(info1)

      metrics.markCalls shouldBe empty
    }
    "include the exception name" in withSut { (sut, metrics) =>
      sut.append(error2)

      metrics.markCalls shouldEqual Seq(
        MarkCall("log.error", 1, Seq(Tag("exception", "java.lang.NullPointerException"), Tag("inner", "none")))
      )
    }
    "include the clean exception name for inner class exception" in withSut { (sut, metrics) =>
      sut.append(error4)

      metrics.markCalls shouldEqual Seq(
        MarkCall("log.error", 1, Seq(Tag("exception", "com.tritondigital.counters.logback.ExceptionCounterAppenderTest/InternalException"), Tag("inner", "none")))
      )
    }
    "include the inner exception name" in withSut { (sut, metrics) =>
      sut.append(error3)

      metrics.markCalls shouldEqual Seq(
        MarkCall("log.error", 1, Seq(Tag("exception", "java.io.IOException"), Tag("inner", "java.lang.NumberFormatException")))
      )
    }
  }

  private case class NoTimeMetric(name: String, incrementBy: Long, tags: Tag*)

  private def withSut(testCode: (ExceptionCounterAppender, RecordingMetrics) => Unit) {
    val metrics = new RecordingMetrics
    val sut = new ExceptionCounterAppender(metrics)
    testCode(sut, metrics)
  }

  private class InternalException extends Exception

}
