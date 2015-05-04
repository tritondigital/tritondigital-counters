package com.tritondigital.counters

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

import _root_.akka.actor.ActorSystem
import _root_.akka.util.Timeout

import scala.concurrent.Future

/**
 * Guarantees that we are not starting an other publication round if previous one has not finished yet.
 */
class MetricsPublicationCoordinator(system: ActorSystem,
                                    providers: Seq[MetricsProvider],
                                    publishers: Seq[MetricsPublisher],
                                    filter: ScalaMetricFilter) extends Logging {
  require(system != null, "system must not be null")
  require(providers != null, "providers must not be null")
  require(publishers != null, "publishers must not be null")
  require(filter != null, "filter must not be null")

  import system.dispatcher

  // Import execution context for futures

  private[this] implicit val timeout =
    Timeout(2, TimeUnit.MINUTES)

  private[this] val isBusy =
    new AtomicBoolean(false)

  private[this] var publishersTask: Seq[PublisherTask] =
    publishers.map(PublisherTask(_))

  def startPublicationRound() {
    if (isBusy.compareAndSet(false, true)) {
      // Call all the providers
      val provided = providers.map(_.provide())
      for (metrics <- Future.sequence(provided)) { // Wait for all the answers to come back
        val toPublish = filter.filter(metrics.flatten)
        publishersTask = publishersTask.map(_.publishNewMetrics(toPublish))
        isBusy.getAndSet(false)
      }
    } else {
      log.warn(s"Ignoring publication round, since metrics provider are still busy.")
    }
  }

  def pause() {
    publishers.foreach(_.pause())
  }
}

case class PublisherTask(publisher: MetricsPublisher, task: Future[Unit] = Future.successful(())) extends Logging {
  def publishNewMetrics(metrics: Iterable[Metric]) = {
    if (!task.isCompleted) {
      log.warn(s"Ignoring publication for ${publisher.getClass.getSimpleName} since already busy.")
      this
    } else {
      val newTask = publisher.publish(metrics)
      this.copy(task = newTask)
    }
  }
}
