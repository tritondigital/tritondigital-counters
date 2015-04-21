package com.tritondigital.counters

import _root_.akka.actor.ActorSystem

class MetricsSystemBuilder(system: ActorSystem,
                           otherProviders: Array[MetricsProvider],
                           datadogFilter: ScalaMetricFilter,
                           customTags: Array[Tag],
                           doPublishToDatadog: Boolean,
                           doPublishToLogback: Boolean,
                           doMonitorLogback: Boolean) {

  def this(system: ActorSystem) = this(system, Array.empty[MetricsProvider], null, Array.empty[Tag], false, false, false)

  def addProvider(provider: MetricsProvider) =
    new MetricsSystemBuilder(system, otherProviders :+ provider, datadogFilter, customTags, doPublishToDatadog, doPublishToLogback, doMonitorLogback)

  def publishToDatadog() =
    new MetricsSystemBuilder(system, otherProviders, datadogFilter, customTags, true, doPublishToLogback, doMonitorLogback)

  def publishToLogback() =
    new MetricsSystemBuilder(system, otherProviders, datadogFilter, customTags, doPublishToDatadog, true, doMonitorLogback)

  def monitorLogback() =
    new MetricsSystemBuilder(system, otherProviders, datadogFilter, customTags, doPublishToDatadog, doPublishToLogback, true)

  def withDatadogFilter(filter: ScalaMetricFilter) =
    new MetricsSystemBuilder(system, otherProviders, filter, customTags, true, doPublishToLogback, doMonitorLogback)

  def addGlobalTag(tagName: String, tagValue: String) =
    new MetricsSystemBuilder(system, otherProviders, datadogFilter, customTags :+ new Tag(tagName, tagValue), doPublishToDatadog, doPublishToLogback, doMonitorLogback)

  def build =
    new MetricsSystemFactory(system, otherProviders, datadogFilter, customTags, doPublishToDatadog, doPublishToLogback, doMonitorLogback).metrics
}
