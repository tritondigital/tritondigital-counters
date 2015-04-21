package com.tritondigital.counters

import _root_.akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.Matchers
import org.scalatest.matchers.Matcher

trait CustomMatchers extends Matchers {
  def eventuallyVerify[T: Manifest](inner: Matcher[T]): Matcher[T] = Matcher { left =>
    val TimeoutMs = 4000
    val start = System.currentTimeMillis()
    var result = inner(left)
    def hasNotTimedOut = (System.currentTimeMillis() - start) < TimeoutMs

    while (!result.matches && hasNotTimedOut) {
      Thread.sleep(100)
      result = inner(left)
    }

    result
  }

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
