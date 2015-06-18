package com.tritondigital.counters.akka

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.tritondigital.counters.{UpdateTimerCall, RecordingMetrics, Metrics, Tag}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class ActorMetricsTest(_system: ActorSystem) extends TestKit(_system) with WordSpecLike with Matchers with ImplicitSender with BeforeAndAfterAll {
  def this() = this(ActorSystem())

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  val MessageValue = "Hello"

  "an actor with metrics" should {
    "publish actor message processing latencies when written in Java" in {
      val metrics = new RecordingMetrics
      val sut = system.actorOf(Props(new JavaActor(metrics)), "java-actor")

      sut ! MessageValue

      expectMsg(MessageValue)

      Thread.sleep(200)

      metrics.updateTimerCalls shouldBe Seq(
        UpdateTimerCall("akka.actor.message", 1, Seq(Tag("path", "/user/java-actor"), Tag("mclass", "String")))
      )
    }
    "publish actor message processing latencies when written in Scala" in {
      val metrics = new RecordingMetrics
      val sut = system.actorOf(Props(new ScalaActor(metrics)), "scala-actor")

      sut ! MessageValue

      expectMsg(MessageValue)

      Thread.sleep(200)

      metrics.updateTimerCalls shouldBe Seq(
        UpdateTimerCall("akka.actor.message", 1, Seq(Tag("path", "/user/scala-actor"), Tag("mclass", "String")))
      )
    }
    "publish actor message processing latencies when actor is named with special characters" in {
      val metrics = new RecordingMetrics
      val sut = system.actorOf(Props(new ScalaActor(metrics)), "@scala;-=actor+")

      sut ! MessageValue

      expectMsg(MessageValue)

      Thread.sleep(200)

      metrics.updateTimerCalls shouldBe Seq(
        UpdateTimerCall("akka.actor.message", 1, Seq(Tag("path", "/user/scala-actor"), Tag("mclass", "String")))
      )
    }
  }

  class JavaActor(metrics: Metrics) extends ActorWithMetrics(metrics) {
    @throws(classOf[Exception])
    def wrappedOnReceive(message: Any) {
      getSender().tell(message, null)
    }
  }

  class ScalaActor(val metrics: Metrics) extends Actor with ActorMetrics {
    def wrappedReceive = {
      case message => sender ! message
    }
  }

}
