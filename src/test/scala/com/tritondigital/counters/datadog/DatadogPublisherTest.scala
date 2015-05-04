package com.tritondigital.counters.datadog

import _root_.akka.actor.ActorSystem
import com.tritondigital.counters._
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.Await
import scala.concurrent.duration._

class DatadogPublisherTest extends PublisherTest[FakeDatadogServer, DatadogPublisher] with Eventually {
  implicit override val patienceConfig =
    PatienceConfig(timeout = scaled(Span(4, Seconds)), interval = scaled(Span(100, Millis)))

  private val Metric6Msg = "m1:6|g"
  private val Metric7Msg = "m1:7|g"
  private val Metric8Msg = "m1:8|g"

  "Datadog publisher" should {
    "publish metrics after interval" in withSut() { (server, sut) =>
      sut.publish(Metric6)

      eventually {
        server should havePublished(Metric6Msg)
      }
    }
    "ignore publications too close to each other" in withSut() { (server, sut) =>
      sut.publish(Metric6)
      sut.publish(Metric7) // This one should be ignored

      eventually {
        server should havePublished(Metric6Msg)
      }
    }
    "add common tags" in withSut(List(Tag("tag", "test"))) { (server, sut) =>
      sut.publish(Metric6)

      eventually {
        server should havePublished(Metric6Msg + "|#tag:test")
      }
    }
    "ignore host tag" in withSut(List(Tag("host", "test"))) { (server, sut) =>
      sut.publish(Metric6)

      eventually {
        server should havePublished(Metric6Msg)
      }
    }
    "continue to publish metrics after yet another interval" in withSut() { (server, sut) =>
      sut.publish(Metric6)

      eventually {
        server should havePublished(Metric6Msg)
      }
      
      sut.publish(Metric7)

      eventually {
        server should havePublished(Metric6Msg, Metric7Msg)
      }
    }
    "restarts when Datadog reports an error" in withSut() { (server, sut) =>
      sut.publish(Metric6)

      eventually {
        server should havePublished(Metric6Msg)
      }

      server.reportErrors()

      sut.publish(Metric7)

      eventually {
        server should haveRefused(Metric7Msg)
      }

      server.reportNoErrors()

      sut.publish(Metric8)

      eventually {
        server should havePublished(Metric6Msg, Metric8Msg)
      }
    }
    "connect once server becomes available" in withSut() { (server, sut) =>
      server.stopAcceptingConnections()

      sut.publish(Metric6) // This should not publish successfully, and silently place the socket in a weird state

      Thread.sleep(1000) // Wait enough time so that publication has been attempted once and failed due to connection failure

      server.startAcceptingConnections()

      Await.result(sut.publish(Metric7), 1.second) // This one should fail, due to the socket being in a weird state, but should detect it and reset the connection

      sut.publish(Metric8) // This one should pass

      eventually {
        server should havePublished(Metric8Msg) // We only have the one sent when successfully reconnecting
      }
    }
    "be able to pause" in withSut() { (server, sut) =>
      sut.publish(Metric6) // This one publication should fail, since we will cut the connection right after.

      sut.pause()

      Thread.sleep(1000)

      sut.publish(Metric7)

      eventually {
        server should havePublished(Metric7Msg)
      }
    }
  }

  def createServer(port: Int, system: ActorSystem) =
    new FakeDatadogServer(port)(system)

  def createSut(system: ActorSystem, metricsSystem: Metrics, commonTags: Seq[Tag]) =
    new DatadogPublisher(system, metricsSystem, commonTags, FilterNoMetric)
}
