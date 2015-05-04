package com.tritondigital.counters.jmx

import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Await
import scala.concurrent.duration._

class JMXMetricsProviderTest  extends WordSpec with Matchers {
  "JMX metrics provider" should {
    "provide metrics" in {
      val metrics = Await.result(JMXMetricsProvider.provide(), 1.seconds)

      val heap = metrics.head.value.toDouble.toLong
      val cpu = metrics.last.value.toDouble

      metrics.map(_.name) shouldEqual Seq("java.heap.usage", "java.cpu.load")
      heap should be > 0L
      cpu should (be >= 0.0 and be <= 1.0)
    }
  }
}
