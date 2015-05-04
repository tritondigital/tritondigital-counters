package com.tritondigital.counters

import _root_.akka.actor.ActorSystem
import ch.qos.logback.core.Context
import com.tritondigital.counters.datadog.DatadogPublisher
import com.tritondigital.counters.jmx.JMXMetricsProvider
import com.tritondigital.counters.logback.{ExceptionCounterAppender, LogPublisher}
import org.slf4j.{Logger, LoggerFactory}

/**
 * Main entry point for the Triton Counters. This is starting the metrics system and provide various extension points.
 */
class MetricsSystemFactory(system: ActorSystem,
                           otherProviders: Array[MetricsProvider],
                           datadogFilter: ScalaMetricFilter,
                           globalTags: Array[Tag],
                           publishToDatadog: Boolean,
                           publishToLogback: Boolean,
                           doMonitorLogback: Boolean) extends Logging {
  require(system != null, "Actor system must not be null")

  // Instantiate private components

  private val config =
    system.settings.config

  private val simpleMetrics =
    new SimpleMetrics(system)

  private val allProviders =
    simpleMetrics +: JMXMetricsProvider +: Option(otherProviders).getOrElse(Array.empty[MetricsProvider])

  private val deduplicatingFilter =
    new MetricDeduplicatingFilter(config)

  private val datadogPublisher =
    if (publishToDatadog)
      Some(new DatadogPublisher(system, simpleMetrics, globalTags, Option(datadogFilter).getOrElse(FilterNoMetric)))
    else
      None

  private val logPublisher =
    if (publishToLogback)
      Some(LogPublisher)
    else
      None

  private val coordinator =
    new MetricsPublicationCoordinator(system, allProviders, List(datadogPublisher, logPublisher).flatten, deduplicatingFilter)

  private val scheduler =
    new JitteredScheduler(system, coordinator.startPublicationRound())

  // Wire up everything

  if (doMonitorLogback)
    installLogbackAppender()

  simpleMetrics.markMeter("app.start")

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run() {
      simpleMetrics.markMeter("app.stop")
    }
  })

  scheduler.start()

  // Expose publicly available components

  val metrics: Metrics = simpleMetrics

  private def installLogbackAppender() {
    val appender = new ExceptionCounterAppender(simpleMetrics)
    appender.setContext(LoggerFactory.getILoggerFactory.asInstanceOf[Context])

    val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).asInstanceOf[ch.qos.logback.classic.Logger]
    root.addAppender(appender)

    appender.start()
  }
}
