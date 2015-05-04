package com.tritondigital.counters

import org.scalatest.{Matchers, WordSpec}

class TagTest extends WordSpec with Matchers {
  "A tag" must {
    "not accept null key" in {
      an[IllegalArgumentException] should be thrownBy Tag(null, "abc")
    }
    "not accept null value" in {
      an[IllegalArgumentException] should be thrownBy Tag("abc", null)
    }
    "not accept invalid characters" in {
      an[IllegalArgumentException] should be thrownBy Tag("a&b", "abc")
      an[IllegalArgumentException] should be thrownBy Tag("a b", "abc")
      an[IllegalArgumentException] should be thrownBy Tag("abc", "a&b")
      an[IllegalArgumentException] should be thrownBy Tag("abc", "a b")
    }
    "accept valid characters" in {
      Tag("a.b.c_d-e", "abc")
      Tag("abc", "a.b.c_d-e")
      Tag("abc", "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890-_./")
    }
    "be comparable" in {
      Tag("k1", "v1") compare Tag("k1", "v1") shouldBe 0
      Tag("k1", "v2") compare Tag("k1", "v1") shouldBe 1
      Tag("k1", "v1") compare Tag("k1", "v2") shouldBe -1
      Tag("k2", "v1") compare Tag("k1", "v1") shouldBe 1
      Tag("k1", "v1") compare Tag("k2", "v1") shouldBe -1
    }
  }
}
