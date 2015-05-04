package com.tritondigital.counters

import com.typesafe.config.{ConfigException, ConfigFactory}
import org.scalatest.{Matchers, WordSpec}

class MetricDeduplicatingFilterTest extends WordSpec with Matchers {
  private val NormalConfig = ConfigFactory.parseString("tritondigital_counters.deduplication_time_limit = 10 minutes")
  private val EleventMinutes = 11 * 60
  private val metricBaseSet = Seq(
    Metric("m1", LongValue(10), Nil, 100000),
    Metric("m2", LongValue(20), Nil, 100000)
  )
  private val metricNoChange1Set = Seq(
    Metric("m1", LongValue(10), Nil, 100001),
    Metric("m2", LongValue(20), Nil, 100001)
  )
  private val metricNoChange2Set = Seq(
    Metric("m1", LongValue(10), Nil, 100002),
    Metric("m2", LongValue(20), Nil, 100002)
  )
  private val metric1ChangeSet = Seq(
    Metric("m1", LongValue(10), Nil, 100005),
    Metric("m2", LongValue(21), Nil, 100005)
  )
  private val metric1AdditionSet = Seq(
    Metric("m1", LongValue(10), Nil, 100005),
    Metric("m2", LongValue(20), Nil, 100005),
    Metric("m3", LongValue(30), Nil, 100005)
  )
  private val metricNoChangeAfter11MinutesSet = Seq(
    Metric("m1", LongValue(10), Nil, 100000 + EleventMinutes),
    Metric("m2", LongValue(20), Nil, 100000 + EleventMinutes)
  )

  "Deduplication filter" should {
    "not accept null config" in {
      an [IllegalArgumentException] should be thrownBy new MetricDeduplicatingFilter(null)
    }
    "not accept missing tritondigital_counters.deduplication_time_limit config key" in {
      an [ConfigException.Missing] should be thrownBy new MetricDeduplicatingFilter(ConfigFactory.parseString(""))
    }
    "not filter new metrics" in {
      val sut = new MetricDeduplicatingFilter(NormalConfig)

      sut.filter(metricBaseSet) should contain theSameElementsAs metricBaseSet
      sut.filter(metric1AdditionSet) should contain theSameElementsAs metric1AdditionSet.drop(2)
    }
    "filter metrics with same value" in {
      val sut = new MetricDeduplicatingFilter(NormalConfig)

      sut.filter(metricBaseSet)
      sut.filter(metric1ChangeSet) should contain theSameElementsAs metric1ChangeSet.tail
    }
    "not filter latest metric before a change of value" in {
      val sut = new MetricDeduplicatingFilter(NormalConfig)

      sut.filter(metricBaseSet)
      sut.filter(metricNoChange1Set)
      sut.filter(metricNoChange2Set)
      sut.filter(metric1ChangeSet) should contain theSameElementsAs Seq(metricNoChange2Set.last, metric1ChangeSet.last)
    }
    "not filter metrics with same value after 10 minutes" in {
      val sut = new MetricDeduplicatingFilter(NormalConfig)

      sut.filter(metricBaseSet)
      sut.filter(metricNoChangeAfter11MinutesSet) should contain theSameElementsAs metricNoChangeAfter11MinutesSet
    }
  }
}
