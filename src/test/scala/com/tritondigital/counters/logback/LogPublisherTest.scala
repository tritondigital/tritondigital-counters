package com.tritondigital.counters.logback

import com.tritondigital.counters.Metric
import org.scalatest.{Matchers, WordSpec}

class LogPublisherTest extends WordSpec with Matchers {
  "The log publisher" should {
    "not blow up when publishing" in {
      LogPublisher.publish(List(Metric("test.metric", 8)))
    }
  }
}
