package com.tritondigital.counters

import java.util.concurrent.atomic.AtomicInteger

import _root_.akka.actor.ActorSystem
import org.scalatest.matchers.Matcher
import org.scalatest.{Matchers, WordSpec}

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}

class MetricsPublicationCoordinatorTest extends WordSpec with Matchers with CustomMatchers {

  "The metrics publication coordinator" should {
    "not accept invalid dependencies" in {
      val system = ActorSystem()

      an [IllegalArgumentException] should be thrownBy new MetricsPublicationCoordinator(null, Nil, Nil, FilterNoMetric)
      an [IllegalArgumentException] should be thrownBy new MetricsPublicationCoordinator(system, null, Nil, FilterNoMetric)
      an [IllegalArgumentException] should be thrownBy new MetricsPublicationCoordinator(system, Nil, null, FilterNoMetric)
      an [IllegalArgumentException] should be thrownBy new MetricsPublicationCoordinator(system, Nil, Nil, null)

      system.shutdown()
      system.awaitTermination()
    }
    "gather metrics then call publishers" in withSut() { (sut, fastPublisher, slowPublisher) =>
      // Act
      sut.startPublicationRound()

      fastPublisher should eventuallyVerify( haveMadePublished(Metric("test", 1)) )
      slowPublisher should eventuallyVerify( haveMadePublished(Metric("test", 1)) )
    }
    "ignore publication requests if busy" in withSut() { (sut, fastPublisher, slowPublisher) =>
      sut.startPublicationRound()

      // Act
      sut.startPublicationRound() // Should be ignored

      fastPublisher should eventuallyVerify( haveMadePublished(Metric("test", 1)) )
    }
    "continue publishing once not busy anymore" in withSut() { (sut, fastPublisher, slowPublisher) =>
      sut.startPublicationRound()
      fastPublisher should eventuallyVerify( haveMadePublished(Metric("test", 1)) )
      slowPublisher should eventuallyVerify( haveMadePublished(Metric("test", 1)) )

      // Act
      sut.startPublicationRound()

      fastPublisher should eventuallyVerify( haveMadePublished(Metric("test", 1), Metric("test", 2)) )
      slowPublisher should eventuallyVerify( haveMadePublished(Metric("test", 1), Metric("test", 2)) )
    }
    "continue publishing in availables publishers and skip busy ones" in withSut() { (sut, fastPublisher, slowPublisher) =>
      sut.startPublicationRound()

      fastPublisher should eventuallyVerify( haveMadePublished(Metric("test", 1)) )
      sut.startPublicationRound()

      // Act
      fastPublisher should eventuallyVerify( haveMadePublished(Metric("test", 1), Metric("test", 2)) )
      slowPublisher should eventuallyVerify( haveMadePublished(Metric("test", 1)) )
    }
    "pause publishers" in withSut() { (sut, fastPublisher, slowPublisher) =>
      // Act
      sut.pause()

      fastPublisher should eventuallyVerify( bePaused )
      slowPublisher should eventuallyVerify( bePaused )
    }
  }

  private def withSut(filter: ScalaMetricFilter = FilterNoMetric)(test: (MetricsPublicationCoordinator, RecordingPublisher, SlowPublisher) => Unit) {
    usingActorSystem() { system =>
      val fastPublisher = new FastPublisher()
      val slowPublisher = new SlowPublisher(system)
      val provider = new DummyProvider(system)

      test(new MetricsPublicationCoordinator(system, List(provider), List(fastPublisher, slowPublisher), filter), fastPublisher, slowPublisher)
    }
  }

  val bePaused: Matcher[RecordingPublisher] = be(1) compose { publisher =>
    publisher.pauses.size
  }

  def haveMadePublished(metrics: Metric*): Matcher[RecordingPublisher] = be(metrics.map(assertable)) compose { publisher =>
    publisher.published.toSeq.map(assertable)
  }

  def assertable(m: Metric) = (m.key, m.value)

  class DummyProvider(system: ActorSystem) extends MetricsProvider {
    import system.dispatcher
    private val counter = new AtomicInteger()
    private val delay = 50.millis

    def provide() = {
      val promise = Promise[Iterable[Metric]]()

      // Introduce a bit of artificial delay
      system.scheduler.scheduleOnce(delay) {
        promise.success(List(Metric("test", counter.incrementAndGet())))
      }

      promise.future
    }
  }

  trait RecordingPublisher extends MetricsPublisher {
    var published = Queue.empty[Metric]
    var pauses = Queue.empty[Unit]

    /** Allows to close expensive resources when publication is halted for the time being. Those will need to be lazily re-opened next time publish is called. **/
    def pause() = pauses = pauses.enqueue(())
  }

  class FastPublisher extends RecordingPublisher {
    private val completed = Future.successful(())

    def publish(metrics: Iterable[Metric]) = {
      published = published.enqueue(metrics.toList)
      completed
    }
  }

  class SlowPublisher(val system: ActorSystem) extends RecordingPublisher {
    import system.dispatcher
    private val publicationDelay = 50.millis

    def publish(metrics: Iterable[Metric]) = {
      val promise = Promise[Unit]()

      // Introduce a bit of artificial delay in the publication
      system.scheduler.scheduleOnce(publicationDelay) {
        published = published.enqueue(metrics.toList)
        promise.success()
      }

      promise.future
    }
  }

}
