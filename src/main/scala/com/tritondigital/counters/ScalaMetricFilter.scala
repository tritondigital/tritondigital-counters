package com.tritondigital.counters

import scala.collection.JavaConverters._

trait ScalaMetricFilter {
  def filter(metrics: Iterable[Metric]): Iterable[Metric]
  
  def andThen(other: Option[ScalaMetricFilter]) =
    if (other.isDefined)
      new ScalaMetricFilter {
        def filter(metrics: Iterable[Metric]) =
          other.get.filter(ScalaMetricFilter.this.filter(metrics))
      }
    else
      this
}

abstract class MetricFilter extends ScalaMetricFilter {
  def filter(metrics: Iterable[Metric]): Iterable[Metric] =
    filter(metrics.asJava).asScala

  def filter(metrics: java.lang.Iterable[Metric]): java.lang.Iterable[Metric]
}

class MetricPrefixFilter(includePrefixes: Array[String]) extends ScalaMetricFilter {
  def filter(metrics: Iterable[Metric]) = metrics.filter { metric =>
    includePrefixes.exists(metric.name.startsWith)
  }
}

object FilterNoMetric extends ScalaMetricFilter {
  def filter(metrics: Iterable[Metric]) = metrics
}
