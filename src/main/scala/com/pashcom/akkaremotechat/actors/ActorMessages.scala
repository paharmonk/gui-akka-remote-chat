package com.pashcom.akkaremotechat.actors

// задан в application.conf. все наследники сериализируются через джексон как в конфиге
trait MySerializable

case class Disconnect(userName: String) extends MySerializable
case class Disconnected(userName: String) extends MySerializable
case class Connect(userName: String) extends MySerializable
case class Connected(userName: String) extends MySerializable
case class Message(userName: Option[String] = None,
                   body: String,
                   creationTimestamp: Long = System.currentTimeMillis()) extends MySerializable
