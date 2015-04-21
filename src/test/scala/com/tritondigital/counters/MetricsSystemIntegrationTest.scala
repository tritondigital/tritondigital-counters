package com.tritondigital.counters

import java.io.IOException
import java.net.ServerSocket

import com.codahale.metrics.{Gauge, MetricRegistry}
import com.tritondigital.counters.codahale.CodahaleMetricsProvider
import com.typesafe.config.ConfigFactory
import org.scalatest.WordSpec
import org.scalatest.matchers.{HavePropertyMatchResult, HavePropertyMatcher, MatchResult, Matcher}
import org.slf4j.LoggerFactory

import scala.concurrent.Future

class MetricsSystemIntegrationTest extends WordSpec with CustomMatchers {
  val logger = LoggerFactory.getLogger(getClass)

  "the metrics system" should {
    "publish various metrics" in withSut { (metrics, datadogServer) =>
      val expectedDatadogSharedTags = "g:gv"
      metrics.incrementCounter("aggregated.count", 6, Tag("agg", "true"))
      logger.error("", new IOException)
      logger.error("", new IOException)
      logger.warn("", new NullPointerException)

      datadogServer should eventuallyVerify( havePublishedMessages(
          "aggregated.count:6|g|#agg:true," + expectedDatadogSharedTags,
          "codahale.gauge:11|g|#" + expectedDatadogSharedTags,
          "log.error.count:2|g|#exception:java.io.IOException,inner:none," + expectedDatadogSharedTags,
          "log.warn.count:1|g|#exception:java.lang.NullPointerException,inner:none," + expectedDatadogSharedTags,
          "m1:5|g|#" + expectedDatadogSharedTags
        )
      )
      datadogServer should eventuallyVerify( havePublishedMessageLike("""java.heap.usage:\d+\|g\|#""" + expectedDatadogSharedTags))
      datadogServer should eventuallyVerify( havePublishedMessageLike("""java.cpu.load:\d+.\d+(E-\d+)?\|g\|#""" + expectedDatadogSharedTags))
    }
  }

  private def withSut(testCode: (Metrics, FakeDatadogServer) => Unit) {
    val prov = new MetricsProvider {
      override def provide() = Future.successful(Seq(Metric("m1", 5)))
    }
    val port = using(new ServerSocket(0))(_.getLocalPort)
    val config = ConfigFactory
      .parseString(
        s"""
           |tritondigital_counters.port = $port
           |tritondigital_counters.publish_interval = 2 seconds
        """.stripMargin)
      .withFallback(ConfigFactory.load())

    usingActorSystem(config) { system =>
      val datadogServer = new FakeDatadogServer(port)(system)
      val registry = new MetricRegistry
      registry.register("codahale.gauge", new Gauge[Long] {
        override def getValue = 11
      })

      val metrics = new MetricsSystemBuilder(system)
        .publishToDatadog()
        .publishToLogback()
        .monitorLogback()
        .addProvider(prov)
        .addProvider(new CodahaleMetricsProvider(system, registry))
        .addGlobalTag("g", "gv")
        .build

      testCode(metrics, datadogServer)
    }
  }

  def havePublishedMessages(msgs: String*): Matcher[Server] = listContainsAllOf(msgs) compose(server => server.serverStatus.receivedMsgs)
  def havePublishedMessageLike(msgRegexe: String): Matcher[Server] = have(listContainsMatchFor(msgRegexe)) compose(server => server.serverStatus.receivedMsgs)
  def listContainsMatchFor(r: String): HavePropertyMatcher[Seq[String], String] = HavePropertyMatcher { strings =>
    val regex = r.r

    HavePropertyMatchResult(
      strings.exists(regex.findFirstMatchIn(_).isDefined),
      "message",
      r,
      ""
    )

  }
  def listContainsAllOf(msgs: Seq[String]): Matcher[Seq[String]] = Matcher { strings =>
    MatchResult(
      msgs.forall(strings.contains),
      s"$strings do no contains all of $msgs",
      s"$strings contains some of $msgs"
    )

  }
}
