package com.tritondigital.counters

import _root_.akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.Matchers
import org.scalatest.matchers.Matcher

trait CustomMatchers extends Matchers {
  def usingActorSystem(config: Config = ConfigFactory.load())(action: ActorSystem => Unit) {
    val system = ActorSystem("test-system", config)

    try {
      action(system)
    }
    finally {
      system.shutdown()
      system.awaitTermination()
    }

  }

  def havePublished(msgs: String*): Matcher[Server] =
    be(msgs) compose(server => server.serverStatus.receivedMsgs)

  def haveRefused(msgs: String*): Matcher[Server] =
    be(msgs) compose(server => server.serverStatus.refusedMsgs)

}
