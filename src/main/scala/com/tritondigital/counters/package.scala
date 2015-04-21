package com.tritondigital

import scala.language.reflectiveCalls

package object counters {
  def using[CLO <: { def close() }, T](closeable: CLO)(action: CLO => T) =
    try { action(closeable) }
    finally { closeable.close() }
}
