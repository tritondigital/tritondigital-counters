package com.tritondigital.counters

import java.util.concurrent.TimeUnit

import org.scalatest.{BeforeAndAfter, WordSpec}

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

class SimpleMetricsTest extends WordSpec with BeforeAndAfter with CustomMatchers {
  "The metrics" should {
    "provide gauges" in withSut { sut =>
      sut.setGaugeValue("metric1", 3.0, Tag("t", "t1"))
      sut.setGaugeValue("metric1", 4.0, Tag("t", "t2"))
      sut.setGaugeValue("metric2", 5.0, Tag("t", "t1"))
      sut.setGaugeValue("metric1", 6.0, Tag("t", "t1"))

      provide(sut) shouldEqual Seq(
        ("metric1", Seq(Tag("t", "t1")), DoubleValue(6.0)),
        ("metric1", Seq(Tag("t", "t2")), DoubleValue(4.0)),
        ("metric2", Seq(Tag("t", "t1")), DoubleValue(5.0))
      )
    }
    "provide counters" in withSut { sut =>
      sut.incrementCounter("metric1", Tag("t", "t1"))
      sut.incrementCounter("metric1", Tag("t", "t2"))
      sut.incrementCounter("metric2", Tag("t", "t1"))
      sut.incrementCounter("metric1", Tag("t", "t1"))

      provide(sut) shouldEqual Seq(
        ("metric1", Seq(Tag("t", "t1")), LongValue(2)),
        ("metric1", Seq(Tag("t", "t2")), LongValue(1)),
        ("metric2", Seq(Tag("t", "t1")), LongValue(1))
      )
    }
    "provide meters" in withSut { sut =>
      sut.markMeter("metric1", Tag("t", "t1"))
      sut.markMeter("metric1", Tag("t", "t2"))
      sut.markMeter("metric2", Tag("t", "t1"))
      sut.markMeter("metric1", Tag("t", "t1"))

      provide(sut) shouldEqual Seq(
        ("metric1.count", Seq(Tag("t", "t1")), LongValue(2)),
        ("metric1.count", Seq(Tag("t", "t2")), LongValue(1)),
        ("metric1.m1", Seq(Tag("t", "t1")), DoubleValue(0)),
        ("metric1.m1", Seq(Tag("t", "t2")), DoubleValue(0)),
        ("metric2.count", Seq(Tag("t", "t1")), LongValue(1)),
        ("metric2.m1", Seq(Tag("t", "t1")), DoubleValue(0))
      )
    }
    "provide histograms" in withSut { sut =>
      sut.updateHistogram("metric1", 7, Tag("t", "t1"))
      sut.updateHistogram("metric1", 8, Tag("t", "t2"))
      sut.updateHistogram("metric2", 9, Tag("t", "t1"))
      sut.updateHistogram("metric1", 7, Tag("t", "t1"))

      provide(sut) shouldEqual Seq(
        ("metric1.count", Seq(Tag("t", "t1")), LongValue(2)),
        ("metric1.count", Seq(Tag("t", "t2")), LongValue(1)),
        ("metric1.median", Seq(Tag("t", "t1")), DoubleValue(7)),
        ("metric1.median", Seq(Tag("t", "t2")), DoubleValue(8)),
        ("metric1.p75", Seq(Tag("t", "t1")), DoubleValue(7)),
        ("metric1.p75", Seq(Tag("t", "t2")), DoubleValue(8)),
        ("metric1.p99", Seq(Tag("t", "t1")), DoubleValue(7)),
        ("metric1.p99", Seq(Tag("t", "t2")), DoubleValue(8)),
        ("metric2.count", Seq(Tag("t", "t1")), LongValue(1)),
        ("metric2.median", Seq(Tag("t", "t1")), DoubleValue(9)),
        ("metric2.p75", Seq(Tag("t", "t1")), DoubleValue(9)),
        ("metric2.p99", Seq(Tag("t", "t1")), DoubleValue(9))
      )
    }
    "provide timers" in withSut { sut =>
      sut.updateTimer("metric1", 7, TimeUnit.MILLISECONDS, Tag("t", "t1"))
      sut.updateTimer("metric1", 8, TimeUnit.MILLISECONDS, Tag("t", "t2"))
      sut.updateTimer("metric2", 9, TimeUnit.MILLISECONDS, Tag("t", "t1"))
      sut.updateTimer("metric1", 7, TimeUnit.MILLISECONDS, Tag("t", "t1"))

      provide(sut) shouldEqual Seq(
        ("metric1.count", Seq(Tag("t", "t1")), LongValue(2)),
        ("metric1.count", Seq(Tag("t", "t2")), LongValue(1)),
        ("metric1.m1", Seq(Tag("t", "t1")), DoubleValue(0)),
        ("metric1.m1", Seq(Tag("t", "t2")), DoubleValue(0)),
        ("metric1.median", Seq(Tag("t", "t1")), DoubleValue(7)),
        ("metric1.median", Seq(Tag("t", "t2")), DoubleValue(8)),
        ("metric1.p75", Seq(Tag("t", "t1")), DoubleValue(7)),
        ("metric1.p75", Seq(Tag("t", "t2")), DoubleValue(8)),
        ("metric1.p99", Seq(Tag("t", "t1")), DoubleValue(7)),
        ("metric1.p99", Seq(Tag("t", "t2")), DoubleValue(8)),
        ("metric2.count", Seq(Tag("t", "t1")), LongValue(1)),
        ("metric2.m1", Seq(Tag("t", "t1")), DoubleValue(0)),
        ("metric2.median", Seq(Tag("t", "t1")), DoubleValue(9)),
        ("metric2.p75", Seq(Tag("t", "t1")), DoubleValue(9)),
        ("metric2.p99", Seq(Tag("t", "t1")), DoubleValue(9))
      )
    }
  }

  private def provide(sut: SimpleMetrics) =
    Await
      .result(sut.provide, 1.seconds)
      .toIndexedSeq
      .sortBy(m => (m.name, m.tags(0).key, m.tags(0).value))
      .map(m => (m.name, m.tags, m.value))

  private def withSut(test: SimpleMetrics => Unit) {
    usingActorSystem() { system =>
      val sut = new SimpleMetrics(system)
      test(sut)
    }
  }
}
