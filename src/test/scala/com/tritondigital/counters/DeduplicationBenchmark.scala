package com.tritondigital.counters

import java.io.PrintStream

import _root_.akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

import scala.collection.mutable
import scala.concurrent.Await
import scala.util.Random
import scala.concurrent.duration._

object DeduplicationBenchmark extends App with CustomMatchers {
  val Loops = 1000000

  val config = ConfigFactory.parseString("tritondigital_counters.deduplication_time_limit = 10 minutes")
  usingActorSystem() { system =>
    import system.dispatcher

    val metrics = new SimpleMetrics(system)

    println("Generating metrics...")
    for (i <- 1 to Loops)
      metrics.updateHistogram("benchmark.histo." + Random.nextInt(2), 12L, Tag("tag1", Random.nextInt(100).toString), Tag("tag2", Random.nextInt(100).toString))
    val fixedMetrics = for(i <- 1 to 80000)
      yield Metric("benchmark.histo." + i, 2.3, Seq(Tag("tag1", Random.nextInt(100).toString), Tag("tag2", Random.nextInt(100).toString)))

    val samples = List(
      Await.result(metrics.provide, 120.seconds),
      fixedMetrics
    )

    using(new PrintStream("results.csv")) { p =>
      p.println("Implementation,Sample size,First pass time,Distinct metrics,Second pass time,Second pass time")
      for {
        sample <- samples
      } yield {
        println(s"Running benchmark with ${sample.size} metrics...")
        val filter = new MetricDeduplicatingFilter(config)
        val (time1, filtered1) = time(filter.filter(sample))
        val (time2, filtered2) = time(filter.filter(sample))
        p.println(s"${filter.getClass.getSimpleName},${sample.size},$time1,$filtered1,$time2,$filtered2")
      }
    }

    println("Benchmark done!")
  }

  def time(action: => Iterable[Metric]) = {
    val start = System.currentTimeMillis()
    val filtered = action.size
    println(s"Got $filtered metrics.")
    (System.currentTimeMillis() - start, filtered)
  }

  def time(label: String)(action: => Unit) {
    println(s"Starting $label...")
    val start = System.currentTimeMillis()
    action
    println(s"$label took ${System.currentTimeMillis() - start}.")
  }
}
