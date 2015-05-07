package com.tritondigital.counters
package datadog

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import _root_.akka.actor._
import _root_.akka.io.UdpConnected._
import _root_.akka.io.{IO, UdpConnected}
import _root_.akka.pattern._
import _root_.akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps

class DatadogPublisher(system: ActorSystem, metricsSystem: Metrics, commonTags: Seq[Tag], filter: ScalaMetricFilter, providers: MetricsProvider*) extends MetricsPublisher with Logging {
  require(system != null, "system must not be null")
  require(metricsSystem != null, "metricsSystem must not be null")
  require(commonTags != null, "commonTags must not be null")
  require(filter != null, "filter must not be null")

  private [this] val config =
    system.settings.config

  private [this] val address =
    new InetSocketAddress(config.getString("tritondigital_counters.datadog.host"), config.getInt("tritondigital_counters.datadog.port"))

  private [this] var datadogConnection =
    Option.empty[ActorRef]

  private [this] var connectionIteration =
    1

  private [this] implicit val timeout =
    Timeout(90, TimeUnit.SECONDS)

  private [this] val commonTagsToSend =
    commonTags.filter(_.key != "host") // host is added naturally by Datadog, this is unnecessary

  private case class Publish(metrics: Iterable[Metric])
  private case class Ack(remaining: Iterable[Metric], count: Int, timeoutHandler: Cancellable) extends Event
  private case class SuspiciousConnection(replyTo: ActorRef) // When OS is taking a bit too much time to ack one write. Better restart the connection.
  private case object CleanupQuarantineFinished

  def publish(metrics: Iterable[Metric]) = {
    (ensureConnection ? Publish(metrics)).mapTo[Unit]
  }

  private [this] def ensureConnection = {
    this.synchronized {
      if (datadogConnection.isEmpty) {
        datadogConnection = Some(system.actorOf(Props(new DatadogConnection), s"datadog-connection-$connectionIteration"))
        connectionIteration += 1
      }
      datadogConnection.get
    }
  }

  /** Handles non reliable connection to Datadog agent **/
  private [this] class DatadogConnection extends Actor {
    import context.dispatcher

    private [this] var publishedKeys = Set.empty[MetricKey]

    // This connection is a finite state machine with 4 states (initial, waiting for connection with metrics, connected, sending).
    // Did not used Akka FSM because I am not sure it would be more readable.

    val receive = sleeping

    def sleeping: Receive = {
      case Publish(metrics) =>
        initiateConnection() // Wait an actual publication request to initiate a connection
        context become waitForConnection(metrics, sender())

      case other =>
        errorManagement("sleeping")(other)
    }

    def waitForConnection(toPublish: Iterable[Metric], replyTo: ActorRef): Receive = {
      case _: Publish =>
        log.warn("[wait for connection] Busy connecting. Ignoring publication request.")

      case _: Connected =>
        startSending("wait for connection", registerConnection, toPublish, replyTo)

      case other =>
        errorManagement("wait for connection")(other)
    }

    def connected(socket: ActorRef): Receive = {
      case Publish(metrics) =>
        startSending("connected", socket, metrics, sender())

      case other =>
        errorManagement("connected", Some(socket))(other)
    }

    def sending(socket: ActorRef, replyTo: ActorRef): Receive = {
      case _: Publish =>
        log.warn("[sending] Busy writing to socket. Ignoring publication request.")

      case Ack(remaining, count, timeoutHandler) =>
        timeoutHandler.cancel()

        if (remaining.isEmpty) {
          if (log.isDebugEnabled) log.debug(s"[sending] Published $count metrics.")
          context become connected(socket)

          metricsSystem.setGaugeValue("datadog.timeseries.count", publishedKeys.size)

          replyTo ! (())
        }
        else {
          socket ! prepareForWrite(remaining, count, replyTo)
        }

      case other =>
        errorManagement("sending", Some(socket))(other)
    }

    def cleaningConnection(timeoutHandler: Cancellable): Receive = {
      case Disconnected =>
        timeoutHandler.cancel()
        log.debug("[cleaning connection] Received proper disconnection.")
        context become sleeping

      case CleanupQuarantineFinished =>
        log.warn("[cleaning connection] Received no disconnection after 200ms. Restarting process from scratch.")
        context become sleeping
    }

    def errorManagement(stateName: String, socket: Option[ActorRef] = None): Receive = {
      case _: Connected =>
        log.warn(s"[$stateName] Received an unexpected connection confirmation. Ignoring it.")
        () // Ignore

      case CommandFailed(_: Connect) =>
        metricsSystem.markMeter("datadog.failed_connection")
        giveUp(s"[$stateName] Connection to Datadog agent failed. Are you sure '${address.getHostName}' is resolving to the right host? In particular, if you are on Mac, 'localhost' is sometimes resolving on a IPv6 address. If you are still experiencing issues, you can hard code the desired IP in the tritondigital_counters.datadog.host configuration property.")

      case CommandFailed(Send(payload, _)) =>
        throw new Exception(s"[$stateName] Received OS overflow for ${payload.utf8String}. Not supposed to since we are waiting for a ACK before sending next metrics.")

      case Received(payload) =>
        // Datadog agent is replying only to signal errors
        socket.foreach(_ ! Disconnect)
        giveUp(s"[$stateName] Datadog agent reported an error: ${payload.utf8String}. Restarting connection.")

      case PoisonPill =>
        log.debug(s"[$stateName] Datadog agent connection is being killed.")
        socket.foreach(_ ! Disconnect)

      case SuspiciousConnection(replyTo) =>
        replyTo ! (())
        giveUp(s"[$stateName] Datadog connection seem to hang.")

      case Disconnected =>
        giveUp(s"[$stateName] Datadog agent connection has been closed.")
    }

    private def initiateConnection() {
      log.info(s"Trying to connect to Datadog agent at ${address.getHostName}:${address.getPort} ....")
      IO(UdpConnected)(context.system) ! UdpConnected.Connect(self, address)
    }

    private def registerConnection = {
      val socket = sender()
      log.info("[wait for connection] Successfully connected to Datadog agent.")
      metricsSystem.markMeter("datadog.connection")
      socket
    }

    private def startSending(stateName: String, socket: ActorRef, metrics: Iterable[Metric], replyTo: ActorRef) {
      log.debug(s"[$stateName] Publishing metrics to Datadog.")
      metricsSystem.markMeter("datadog.publication")

      val deduplicated = filter.filter(metrics)
      socket ! prepareForWrite(deduplicated, 1, replyTo)

      context become sending(socket, replyTo)
    }

    private def prepareForWrite(metrics: Iterable[Metric], count: Int, replyTo: ActorRef) = {
      val metric = metrics.head

      publishedKeys += metric.key
      Send(
        metric.copy(tags = metric.tags ++ commonTagsToSend).toDatadogBytes,
        Ack(metrics.tail, count + 1, buildSendAckTimeout(replyTo))
      )
    }

    private def buildSendAckTimeout(replyTo: ActorRef) =
      system.scheduler.scheduleOnce(500 millis, self, SuspiciousConnection(replyTo))

    private def buildConnectionCleanupTimeout =
      system.scheduler.scheduleOnce(200 millis, self, CleanupQuarantineFinished)

    private def giveUp(msg: String): Unit = {
      log.warn(msg)

      // Restart the process
      context become cleaningConnection(buildConnectionCleanupTimeout)
    }
  }}
