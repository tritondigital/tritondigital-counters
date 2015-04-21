package com.tritondigital.counters.util

import scala.util.Random

/**
 * Jiterrize given value by +- jitterFactor
 * Example: for a factor of 25% (0.25) and a value of 10, this will return random numbers between 7.5 and 12.5.
 */
case class Jitter(jitterFactor: Double, value: Double) {
  require(jitterFactor >= 0 && jitterFactor <= 1, "The jitterFactor must be in range [0, 1]")
  require(value > 0, "The value must be a strictly positive value")

  private val rand = new Random()
  private val base = value * (1 - jitterFactor)
  private val factor = 2 * jitterFactor * value

  def jitterrize =
    base + rand.nextDouble() * factor // value * (1 - jitterFactor + rand() * 2 * jitterFactor)
}
