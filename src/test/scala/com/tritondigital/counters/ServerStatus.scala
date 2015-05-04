package com.tritondigital.counters

trait ServerStatus {
  def receivedMsgs: Seq[String]
  def refusedMsgs: Seq[String]
}

trait Server {
  def serverStatus: ServerStatus
}
