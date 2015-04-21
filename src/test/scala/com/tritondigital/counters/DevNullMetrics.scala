package com.tritondigital.counters

import java.util.concurrent.TimeUnit

import _root_.akka.dispatch.Futures

import scala.concurrent.Future

class DevNullMetrics extends Metrics {
  def setGaugeValue(name: String, value: Double, tags: Tag*) {}
  def incrementCounter(name: String, incrementBy: Long, tags: Tag*) {}
  private val queryResult = Futures.successful(new java.util.HashMap[java.util.List[Tag], java.lang.Double]())
  def sumQuery(name: String, tagKeyCombination: String*): Future[java.util.Map[java.util.List[Tag], java.lang.Double]] = queryResult
  def updateTimer(name: String, duration: Long, unit: TimeUnit, tags: Tag*) {}
  def updateTimer(name: String, startMillis: Long, tags: Tag*) {}
  def updateHistogram(name: String, value: Long, tags: Tag*) {}
  def incrementCounter(name: String, tags: Tag*) {}
  def markMeter(name: String, tags: Tag*) {}
  def markMeter(name: String, times: Long, tags: Tag*) {}
}
