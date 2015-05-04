package com.tritondigital.counters

import com.typesafe.config.ConfigFactory

import scala.util.Random

object Benchmark extends App with CustomMatchers {
  val Loops = 100000
//  val TagCardinality = 100
  val TagCardinality = 2

  val config = ConfigFactory
    .load()

  usingActorSystem(config) { system =>
    val metricsSystem = new MetricsSystemFactory(system, Array.empty[MetricsProvider], null, Array.empty[Tag], true, false, true)
    val metrics = metricsSystem.metrics

    // Warm up
    benchmarkedLogic(metrics)
    Thread.sleep(100)

    val start = System.nanoTime()
//    readLine("Ready to start the benchmark. Press [ENTER] to proceed.")
    for(cycle <- 0 to 4) {
      println("Starting cycle")
      for (i <- 0 to Loops) {
        benchmarkedLogic(metrics)
      }
      println("Cycle done!")
      Thread.sleep(15 * 1000)
    }
    val timeTaken = System.nanoTime() - start
    println(s"Took ${timeTaken / 1000000L} ms for $Loops calls (${timeTaken / Loops} ns / call)")
    readLine("Press [ENTER] to quit.")
  }

  def benchmarkedLogic(metrics: Metrics) {
    metrics.updateHistogram("benchmark.histo." + Random.nextInt(2), 12L, Tag("tag1", Random.nextInt(TagCardinality).toString), Tag("tag2", Random.nextInt(TagCardinality).toString))
  }
}
