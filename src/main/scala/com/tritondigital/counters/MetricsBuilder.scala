package com.tritondigital.counters

import _root_.akka.actor.ActorSystem

class MetricsBuilder(system: ActorSystem,
                     otherProviders: Array[MetricsProvider],
                     datadogFilter: ScalaMetricFilter,
                     customTags: Array[Tag],
                     doPublishToDatadog: Boolean,
                     doPublishToLogback: Boolean,
                     doMonitorLogback: Boolean) {

  def this(system: ActorSystem) = this(system, Array.empty[MetricsProvider], null, Array.empty[Tag], false, false, false)

  def addProvider(provider: MetricsProvider) =
    new MetricsBuilder(system, otherProviders :+ provider, datadogFilter, customTags, doPublishToDatadog, doPublishToLogback, doMonitorLogback)

  def publishToDatadog() =
    new MetricsBuilder(system, otherProviders, datadogFilter, customTags, true, doPublishToLogback, doMonitorLogback)

  def publishToLogback() =
    new MetricsBuilder(system, otherProviders, datadogFilter, customTags, doPublishToDatadog, true, doMonitorLogback)

  def monitorLogback() =
    new MetricsBuilder(system, otherProviders, datadogFilter, customTags, doPublishToDatadog, doPublishToLogback, true)

  def withDatadogFilter(filter: ScalaMetricFilter) =
    new MetricsBuilder(system, otherProviders, filter, customTags, true, doPublishToLogback, doMonitorLogback)

  def addGlobalTag(tagName: String, tagValue: String) =
    new MetricsBuilder(system, otherProviders, datadogFilter, customTags :+ new Tag(tagName, tagValue), doPublishToDatadog, doPublishToLogback, doMonitorLogback)

  def build =
    new MetricsSystemFactory(system, otherProviders, datadogFilter, customTags, doPublishToDatadog, doPublishToLogback, doMonitorLogback).metrics
}
