package com.pashcom.akkaremotechat.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.duration._

object ClientChatActor {
  def props(chatServer: ActorRef, userName: String, displayActor: ActorRef): Props = {
    Props(new ClientChatActor(chatServer, userName, displayActor))
  }
}

class ClientChatActor(chatServer: ActorRef, userName: String, displayActor: ActorRef) extends Actor {
  implicit val timeout = Timeout(5.seconds)

  override def preStart(): Unit = {
    chatServer ! Connect(userName)
    displayActor ! Message(body = "you connected to server")
  }

  override def postStop(): Unit = {
    displayActor ! Message(body = "you disconnected from server")
  }

  override def receive: Receive = {
    case Disconnect =>
      chatServer ? Disconnect(userName)
      context.stop(self)

    case body: String =>
      chatServer ! Message(Some(userName), body)

    case message : Message =>
      displayActor ! message

    case Connected(userName) =>
      displayActor ! Message(body = s"$userName joined to chat")

    case Disconnected(userName) =>
      displayActor ! Message(body = s"$userName left chat")
  }
}

