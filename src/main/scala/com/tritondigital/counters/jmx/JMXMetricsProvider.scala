package com.tritondigital.counters.jmx

import java.lang.management.ManagementFactory

import com.tritondigital.counters.{Metric, MetricsProvider}

import scala.concurrent.Future

/**
 * Provides a few metrics from JMX.
 */
object JMXMetricsProvider extends MetricsProvider {
  private val memBean = ManagementFactory.getMemoryMXBean
  private val operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean.asInstanceOf[com.sun.management.OperatingSystemMXBean]

  def provide(): Future[Iterable[Metric]] = Future.successful(
    Seq(
      Metric("java.heap.usage", memBean.getHeapMemoryUsage.getUsed),
      Metric("java.cpu.load", operatingSystemMXBean.getProcessCpuLoad)
    )
  )
}
