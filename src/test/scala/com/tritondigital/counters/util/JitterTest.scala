package com.tritondigital.counters
package util

import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, PropSpec}

class JitterTest extends PropSpec with PropertyChecks with Matchers {
  val DistributionLoops = 1000

  property("always return a number in the expected range of value +- jitter factor") {
    forAll { (sut: Jitter) =>
      val lowerBound = sut.value * (1 - sut.jitterFactor)
      val upperBound = sut.value * (1 + sut.jitterFactor)

      for (_ <- 0 to 10)
        sut.jitterrize should (be >= lowerBound and be <= upperBound)
    }
  }

  property("returns number relatively well distributed") {
    forAll { (sut: Jitter) =>
      val results =
        for(_ <- 1 to DistributionLoops)
          yield sut.jitterrize

      val avg = results.sum / results.size
      val variation = math.abs(sut.value - avg)
      val maxExpectedVariation = 0.1 * sut.jitterFactor * sut.value // Avg should be within 10% of the jitter factor

      variation should be <= maxExpectedVariation
    }
  }

  private implicit val jitterGen = Arbitrary (
    for {
      value <- Gen.choose(0.001, Double.MaxValue / DistributionLoops / 10)
      factor <- Gen.choose(0.0, 1.0)
    } yield Jitter(factor, value)
  )
}
