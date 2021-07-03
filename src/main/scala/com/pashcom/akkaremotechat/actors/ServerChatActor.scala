package com.pashcom.akkaremotechat.actors

import akka.actor.{Actor, ActorRef, Props, Terminated}

object ServerChatActor {
  def props(displayActor: ActorRef): Props = Props(new ServerChatActor(displayActor))
}

class ServerChatActor(displayActor: ActorRef) extends Actor {
  private var onlineClients = Set.empty[ActorRef]

  override def preStart(): Unit = displayActor ! Message(body = "server started")

  override def postStop(): Unit = displayActor ! Message(body = "server stopped")

  override def receive: Receive = {
    case Connect(userName) =>
      displayActor ! Message(body = s"$userName connected")
      onlineClients += sender
      context.watch(sender)
      onlineClients.filter(_ != sender)
                   .foreach(_ ! Connected(userName))

    case Terminated(ref) =>
      onlineClients -= ref

    case Disconnect(userName) =>
      displayActor ! Message(body = s"$userName disconnected")
      onlineClients -= sender
      context.unwatch(sender)
      onlineClients.filter(_ != sender).foreach(_ ! Disconnected(userName))

    case msg: Message =>
      onlineClients.foreach(_ ! msg)
  }
}

