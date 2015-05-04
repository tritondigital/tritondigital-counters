package com.tritondigital.counters.datadog

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

import _root_.akka.actor.{Actor, ActorRef, ActorSystem, Props}
import _root_.akka.io.Udp._
import _root_.akka.io.{IO, Udp}
import _root_.akka.pattern._
import _root_.akka.util.{ByteString, Timeout}
import com.tritondigital.counters.{ServerStatus, Logging, Server}

import scala.concurrent.{Await, Promise}
import scala.util.Success

/** Server using exposing the same binary protocol as OpenTSDB. It records metrics put requests for later assertions. **/
class FakeDatadogServer(port: Int)(implicit system: ActorSystem) extends Logging with Server {
  private implicit val TimeoutMs = Timeout(4000, TimeUnit.MILLISECONDS)
  private val address = new InetSocketAddress("localhost", port)
  private val readyPromise = Promise[Boolean]()
  private val server = system.actorOf(Props(new Server(readyPromise)), "fake-datadog-server")
  private val shouldReportErrors = new AtomicBoolean(false)
  Await.result(readyPromise.future, TimeoutMs.duration)

  def reportErrors() {
    log.info("Will now only report errors")
    shouldReportErrors.getAndSet(true)
  }

  def reportNoErrors() {
    log.info("Will now not report errors")
    shouldReportErrors.getAndSet(false)
  }

  def stopAcceptingConnections() {
    Await.result(server ? StopAcceptingConnections, TimeoutMs.duration)
    log.info("Stopped accepting connections.")
  }


  def startAcceptingConnections() {
    Await.result(server ? StartAcceptingConnections, TimeoutMs.duration)
    log.info("Started accepting connections.")
  }

  def status = Await.result(server ? GetReceived, TimeoutMs.duration).asInstanceOf[UdpStatus]

  def serverStatus = status

  private class Server(readyFuture: Promise[Boolean]) extends Actor {

    private var received = Seq.empty[String]
    private var refused = Seq.empty[String]
    private var replyto = Option.empty[ActorRef]
    private var socket = Option.empty[ActorRef]

    bind()

    def receive = {
      case CommandFailed(_: Bind) =>
        throw new Exception("Binding failed!")

      case CommandFailed(Unbind) =>
        throw new Exception("Unbinding failed!")

      case Bound(_) =>
        log.info("Bound.")
        socket = Some(sender())
        reply()
        if (!readyFuture.isCompleted) readyFuture.complete(Success(true))

      case Unbound =>
        log.info("Unbound.")
        reply()

      case GetReceived =>
        sender ! UdpStatus(received, refused)

      case StopAcceptingConnections =>
        log.info(s"Stopping listening on $address.")
        socket.get ! Unbind
        socket = None
        replyto = Some(sender())

      case StartAcceptingConnections =>
        bind()
        replyto = Some(sender())

      case Received(payload, from) =>
        val request = payload.utf8String
        if (shouldReportErrors.get()) {
          log.info(s"Report an error and ignore '$request'")
          sender() ! Send(ByteString("Error!"), from)
          refused = refused :+ request
        }
        else {
          log.info(s"Received '$request'")
          received = received :+ request
        }
    }

    private def bind() {
      log.info(s"Starting listening on $address.")
      IO(Udp) ! Bind(self, address)
    }

    private def reply() {
      replyto.map(_ ! "ok")
      replyto = None
    }
  }

  private case object GetReceived
  private case object StopAcceptingConnections
  private case object StartAcceptingConnections
}

case class UdpStatus(receivedMsgs: Seq[String], refusedMsgs: Seq[String]) extends ServerStatus

