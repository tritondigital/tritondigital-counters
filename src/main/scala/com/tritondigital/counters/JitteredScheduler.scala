package com.tritondigital.counters

import java.util.concurrent.TimeUnit

import _root_.akka.actor.ActorSystem
import com.tritondigital.counters.util.Jitter

import scala.concurrent.duration.FiniteDuration

class JitteredScheduler(system: ActorSystem, action: => Unit) extends Runnable {
  import system.dispatcher

  private [this] val config = system.settings.config
  private [this] val jitter = Jitter(
    config.getDouble("tritondigital_counters.publish_interval_jitter"),
    config.getDuration("tritondigital_counters.publish_interval", TimeUnit.MILLISECONDS)
  )

  def start() {
    // Starts immediately a run
    system.scheduler.scheduleOnce(FiniteDuration(1, TimeUnit.MILLISECONDS), this)
  }

  def run() {
    try {
      action
    }
    finally {
      try {
        // Catch execption on shutdown
        system.scheduler.scheduleOnce(nextInterval, this)
      } catch {
        case is:IllegalStateException if is.getMessage == "cannot enqueue after timer shutdown" => // ignore
        case ex:Throwable => throw ex
      }
    }
  }

  private [this] def nextInterval =
    FiniteDuration(jitter.jitterrize.toLong, TimeUnit.MILLISECONDS)
}
