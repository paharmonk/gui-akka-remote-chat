package com.pashcom.akkaremotechat.actors

case class Disconnect(userName: String)
case class Disconnected(userName: String)
case class Connect(userName: String)
case class Connected(userName: String)
case class Message(userName: String, body: String, creationTimestamp: Long = System.currentTimeMillis())
