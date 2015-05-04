package com.tritondigital.counters

import _root_.akka.util.ByteString

import scala.runtime.ScalaRunTime

object Metric {
  private [this] val validCharacters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890-_./"
  private [this] val validCharacterSet = validCharacters.toSet
  private [this] val validationMessage = s"Metrics and tags can only contain characters among '$validCharacters': "

  def apply(name: String, value: Long, tags: Seq[Tag]): Metric =
    Metric(name, LongValue(value), tags)
  def apply(name: String, value: Double, tags: Seq[Tag]): Metric =
    Metric(name, DoubleValue(value), tags)
  def apply(name: String, value: Long): Metric =
    Metric(name, LongValue(value))
  def apply(name: String, value: Double): Metric =
    Metric(name, DoubleValue(value))

  def validate(metricOrTagName: String) {
    require(metricOrTagName.forall(validCharacters.contains(_)), validationMessage + metricOrTagName)
  }

  /**
   * Clean a metric or tag name in order to be compatible with all publishers.
   * @return a normalized metric / tag name.
   */
  def clean(metricOrTagName: String) =
    metricOrTagName.filter(validCharacterSet.contains)
}

/**
 * A metrics suitable for ingestion in a Datadog time series database. Although it can in theory be published anywhere, this is closely modeled after OpenTSDB metric model and binary protocol.
 */
case class Metric(name: String, value: MetricValue, tags: Seq[Tag] = Nil, tstamp: Long = (System.currentTimeMillis() / 1000L)) {
  require(tags.size <= 8, s"Cannot create metrics with more than 8 tags: ${tags.map { case Tag(k,v) => s"$k=$v" }.mkString(", ")}")
  Metric.validate(name)
  Metric.validate(name)

  // See http://docs.datadoghq.com/guides/dogstatsd/#datagram-format
  private [counters] val toDatadogBytes = {
    val tagsStr = tags.map { case Tag(k, v) => s"$k:$v" }.mkString(",")
    val tagsSection =
      if (tagsStr.isEmpty) tagsStr
      else "|#" + tagsStr

    ByteString(s"$name:${value.toDatadogString}|g$tagsSection")
  }

  private [counters] lazy val key = MetricKey(name, tags)
}

sealed trait MetricValue {
  def toDatadogString: String
  def toDouble: Double
}

case class LongValue(value: Long) extends MetricValue {
  val toDatadogString = value.toString
  val toDouble = value.toDouble
}

case class DoubleValue(value: Double) extends MetricValue {
  val toDatadogString = value.toString
  val toDouble = value
}

private [counters] case class MetricKey(name: String, tags: Seq[Tag]) {
  // Since this class is expressly designed to serve has a HashMap key, cache the hashCode immediately, so it does not get recomputed over and over again.
  override val hashCode = ScalaRunTime._hashCode(this)
}
