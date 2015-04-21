package com.tritondigital.counters

import java.net.ServerSocket

import _root_.akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.scalatest.WordSpec

trait PublisherTest[Server, Publisher] extends WordSpec with CustomMatchers {
  val Metric6 = List(Metric("m1", 6))
  val Metric7 = List(Metric("m1", 7))
  val Metric8 = List(Metric("m1", 8))

  def createServer(port: Int, system: ActorSystem): Server
  def createSut(system: ActorSystem, metricsSystem: Metrics, commonTags: Seq[Tag]): Publisher

  def withSut(tags: List[Tag] = Nil)(testCode: (Server, Publisher) => Unit) {
    val port = using(new ServerSocket(0))(_.getLocalPort)
    val config = ConfigFactory.parseString(
      s"""
        |tritondigital_counters.datadog.host = "localhost"
        |tritondigital_counters.datadog.port = $port
        |akka.loggers = ["akka.event.slf4j.Slf4jLogger"]
        |akka.loglevel = "DEBUG"
        |akka.actor.debug.receive = on
      """.stripMargin).withFallback(ConfigFactory.load())

    usingActorSystem(config) { system =>
      val metrics = new RecordingMetrics
      val server = createServer(port, system)
      val sut = createSut(system, metrics, tags)

      testCode(server, sut)
    }
  }

}
