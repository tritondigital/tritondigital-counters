package com.tritondigital.counters

import _root_.akka.util.ByteString
import org.scalatest.{Matchers, WordSpec}

class MetricTest extends WordSpec with Matchers {
  private val sut = Metric("log.error.count", LongValue(15), Seq(Tag("app", "test"), Tag("env", "dev")))

  "A metric" must {
    "not accept invalid characters" in {
      an[IllegalArgumentException] should be thrownBy Metric("a&b", LongValue(1))
      an[IllegalArgumentException] should be thrownBy Metric("a b", LongValue(1))
    }
    "accept valid characters" in {
      Metric("a.b.c_d-e", LongValue(1))
    }
    "not accept more than 8 tags" in {
      an[IllegalArgumentException] should be thrownBy Metric("abc", LongValue(1), Seq(Tag("t1", ""), Tag("t2", ""), Tag("t3", ""), Tag("t4", ""), Tag("t5", ""), Tag("t6", ""), Tag("t7", ""), Tag("t8", ""), Tag("t9", "")))
    }
    "builds a valid Datadog datagram" in {
      sut.toDatadogBytes shouldEqual ByteString(s"log.error.count:15|g|#app:test,env:dev")
    }
  }
}
