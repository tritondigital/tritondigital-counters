package com.tritondigital.counters

import java.util.concurrent.TimeUnit

import org.scalatest.WordSpec
import _root_.akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}

class JitteredSchedulerTest extends WordSpec with CustomMatchers {

  "The Jittered Scheduler" should {
    "schedule the given action with jitter" in withSut(ConfigFactory
      .load()
      .withValue("tritondigital_counters.publish_interval",ConfigValueFactory.fromAnyRef(100))
      .withValue("tritondigital_counters.publish_interval_jitter", ConfigValueFactory.fromAnyRef(0.20))) { sut =>
      sut.scheduler.start()
      val publish_interval = sut.system.settings.config.getDuration("tritondigital_counters.publish_interval", TimeUnit.MILLISECONDS)
      Thread.sleep(publish_interval * 10)
      sut.mock.count should be (10 +- 2)
    }

    "not crash" in withSut(ConfigFactory
      .load()
      .withValue("tritondigital_counters.publish_interval", ConfigValueFactory.fromAnyRef(100))
      .withValue("tritondigital_counters.publish_interval_jitter", ConfigValueFactory.fromAnyRef(0.25))) { sut =>
      sut.scheduler.start()
      val publish_interval = sut.system.settings.config.getDuration("tritondigital_counters.publish_interval", TimeUnit.MILLISECONDS)
      Thread.sleep(publish_interval * 10)
      sut.system.shutdown()
      Thread.sleep(1000)
      sut.mock.count should be (10 +- 3)
    }

    "raise an exception" in withSut(ConfigFactory
      .load()
      .withValue("tritondigital_counters.publish_interval", ConfigValueFactory.fromAnyRef(Long.MaxValue))
      .withValue("tritondigital_counters.publish_interval_jitter", ConfigValueFactory.fromAnyRef(0.25))) { sut =>
      sut.scheduler.start()
      a[IllegalArgumentException] should be thrownBy sut.scheduler.run()
    }
  }

  case class Sut(system: ActorSystem, mock: ActionMock, scheduler: JitteredScheduler)

  def withSut(config: Config)(f: Sut => Unit) = {
    val system = ActorSystem("JitteredScheduler", config)
    val mock = new ActionMock()
    val scheduler = new JitteredScheduler(system, mock.action())
    f(Sut(system, mock, scheduler))
  }

  class ActionMock {
    var count = 0
    def action() = {
      count += 1
    }
  }

}
