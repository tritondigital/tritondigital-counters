package com.tritondigital.counters

case class Tag(key: String, value: String) extends Ordered[Tag] {
  require(key != null, "key cannot be null")
  require(value != null, "value cannot be null")
  Metric.validate(key)
  Metric.validate(value)

  // See http://stackoverflow.com/questions/8087958/in-scala-is-there-an-easy-way-to-convert-a-case-class-into-a-tuple
  private def toPair = Tag.unapply(this).get

  def compare(that: Tag) = Tag.ordering.compare(toPair, that.toPair)
}

object Tag {
  private [Tag] val ordering = Ordering.Tuple2[String, String]
}
