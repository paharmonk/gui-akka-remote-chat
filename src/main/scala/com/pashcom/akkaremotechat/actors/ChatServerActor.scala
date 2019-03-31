package com.pashcom.akkaremotechat.actors

import akka.actor.{Actor, ActorRef, Props, Terminated}
import com.pashcom.akkaremotechat.extra.Tools

import scala.swing.TextArea

object ChatServerActor {
  def props(logBox: TextArea) = Props(new ChatServerActor(logBox))
}

class ChatServerActor(logBox: TextArea) extends Actor {
  var onlineClients = Set.empty[ActorRef]

  override def preStart = logBox.append(s"[${Tools.getDateString()}] server started" + "\n")
  override def postStop = logBox.append(s"[${Tools.getDateString()}] server stopped" + "\n")

  def receive = {
    case Connect(userName) =>
      logBox.append(s"[${Tools.getDateString()}] $userName connected" + "\n")
      onlineClients += sender
      context.watch(sender)
      onlineClients.filter(_ != sender).foreach(_ ! Connected(userName))
    case Terminated(ref) =>
      onlineClients -= ref
    case Disconnect(userName) =>
      logBox.append(s"[${Tools.getDateString()}] $userName disconnected" + "\n")
      onlineClients -= sender
      context.unwatch(sender)
      onlineClients.filter(_ != sender).foreach(_ ! Disconnected(userName))
    case msg: Message =>
      onlineClients.foreach(_ ! msg)
  }
}

