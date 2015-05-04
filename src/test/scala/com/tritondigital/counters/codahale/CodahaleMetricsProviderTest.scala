package com.tritondigital.counters.codahale

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.codahale.metrics.{Gauge, MetricRegistry}
import com.tritondigital.counters.{DoubleValue, LongValue, MetricValue, CustomMatchers}
import org.scalatest.WordSpec

import scala.concurrent.Await
import scala.concurrent.duration._

class CodahaleMetricsProviderTest extends WordSpec with CustomMatchers {
  "Codahale Metrics provider" should {

    "not accept null dependencies" in withSystem { system =>
      an[IllegalArgumentException] should be thrownBy new CodahaleMetricsProvider(system, null)
      an[IllegalArgumentException] should be thrownBy new CodahaleMetricsProvider(null, new MetricRegistry())
    }

    "provide gauges" in withSut { (provide, registry) =>
      registry.register("a.b.c.i", new Gauge[Integer] {
        override def getValue = 2
      })
      registry.register("a.b.c.l", new Gauge[Long] {
        override def getValue = 3
      })
      registry.register("a.b.c.f", new Gauge[Float] {
        override def getValue = 4.0f
      })
      registry.register("a.b.c.d", new Gauge[Double] {
        override def getValue = 5.0
      })

      provide() shouldEqual List(
        ("a.b.c.d", DoubleValue(5.0)),
        ("a.b.c.f", DoubleValue(4.0)),
        ("a.b.c.i", LongValue(2)),
        ("a.b.c.l", LongValue(3))
      )
    }

    "ignore null gauges values" in withSut { (provide, registry) =>
      registry.register("a.b.c.i", new Gauge[Integer] {
        override def getValue = null
      })

      provide() shouldBe empty
    }

    "reject unknown gauge values" in withSut { (provide, registry) =>
      registry.register("a.b.c.b", new Gauge[Boolean] {
        override def getValue = true
      })

      an[IllegalArgumentException] should be thrownBy provide()
    }

    "provide counters" in withSut { (provide, registry) =>
      registry.counter("a.b.c").inc(3)

      provide() shouldEqual List(
        ("a.b.c", LongValue(3))
      )
    }

    "provide histograms" in withSut { (provide, registry) =>
      val histogram = registry.histogram("a.b.h")
      histogram.update(7)
      histogram.update(7)

      provide() shouldEqual List(
        ("a.b.h.count", LongValue(2)),
        ("a.b.h.median", DoubleValue(7)),
        ("a.b.h.p75", DoubleValue(7)),
        ("a.b.h.p99", DoubleValue(7))
      )
    }

    "provide timers" in withSut { (provide, registry) =>
      val timer = registry.timer("a.b.t")
      timer.update(8, TimeUnit.MILLISECONDS)
      timer.update(8, TimeUnit.MILLISECONDS)

      provide() shouldEqual List(
        ("a.b.t.count", LongValue(2)),
        ("a.b.t.m1", DoubleValue(0)),
        ("a.b.t.median", DoubleValue(8)),
        ("a.b.t.p75", DoubleValue(8)),
        ("a.b.t.p99", DoubleValue(8))
      )
    }

    "provide meters" in withSut { (provide, registry) =>
      val meter = registry.meter("a.b.m")
      meter.mark()
      meter.mark()
      meter.mark()

      provide() shouldEqual List(
        ("a.b.m.count", LongValue(3)),
        ("a.b.m.m1", DoubleValue(0))
      )
    }
  }

  def withSystem(test: ActorSystem => Unit) {
    val system = ActorSystem()
    try {
      test(system)
    }
    finally {
      system.shutdown()
      system.awaitTermination()
    }
  }

  def withSut(test: (() => List[(String, MetricValue)], MetricRegistry) => Unit) {
    usingActorSystem() { system =>
      val registry = new MetricRegistry
      val sut = new CodahaleMetricsProvider(system, registry)

      def provide =
        Await
          .result(sut.provide(), 30.second)
          .toList
          .sortBy(_.name)
          .map(m => (m.name, m.value))

      test(provide _, registry)
    }
  }
}
