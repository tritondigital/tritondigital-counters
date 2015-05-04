package com.tritondigital.counters

import java.util.concurrent.TimeUnit

class RecordingMetrics extends Metrics {
  var setGaugeCalls = Seq.empty[SetGaugeCall]
  var incrementCalls = Seq.empty[IncrementCall]
  var markCalls = Seq.empty[MarkCall]
  var updateTimerCalls = Seq.empty[UpdateTimerCall]
  var updateHistogramCalls = Seq.empty[UpdateHistogramCall]

  def setGaugeValue(name: String, value: Double, tags: Tag*) {
    setGaugeCalls :+= SetGaugeCall(name, value, tags)
  }
  def incrementCounter(name: String, incrementBy: Long, tags: Tag*) {
    incrementCalls :+= IncrementCall(name, 1, tags)
  }
  def incrementCounter(name: String, tags: Tag*) {
    incrementCounter(name, 1, tags : _*)
  }
  def updateTimer(name: String, duration: Long, unit: TimeUnit, tags: Tag*) {
    updateTimerCalls :+= UpdateTimerCall(name, TimeUnit.MILLISECONDS.convert(duration, unit), tags)
  }
  def updateTimer(name: String, startInMillis: Long, tags: Tag*) {
    updateTimer(name, 1, TimeUnit.MILLISECONDS, tags : _*)
  }
  def markMeter(name: String, tags: Tag*) {
    markMeter(name, 1, tags: _*)
  }
  def markMeter(name: String, times: Long, tags: Tag*) {
    markCalls :+= MarkCall(name, times, tags)
  }
  def updateHistogram(name: String, value: Long, tags: Tag*) {
    updateHistogramCalls :+= UpdateHistogramCall(name, value, tags)
  }

  def sumQuery(name: String, tagKeyCombination: String*) = ???
}

case class SetGaugeCall(name: String, value: Double, tags: Seq[Tag])

case class IncrementCall(name: String, incrementedBy: Long, tags: Seq[Tag])

case class MarkCall(name: String, times: Long, tags: Seq[Tag])

case class UpdateTimerCall(name: String, millis: Long, tags: Seq[Tag])

case class UpdateHistogramCall(name: String, value: Long, tags: Seq[Tag])
