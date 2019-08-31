package com.pashcom.akkaremotechat.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern._

import scala.concurrent.duration._
import akka.util.Timeout
import com.pashcom.akkaremotechat.extra.Tools

import scala.swing.TextArea

object ChatClientActor {
  def props(chatServer: ActorRef, userName: String, chatBox: TextArea) = Props(new ChatClientActor(chatServer, userName, chatBox))
}

class ChatClientActor(chatServer: ActorRef, userName: String, chatBox: TextArea) extends Actor {
  implicit val timeout = Timeout(5 seconds)

  override def preStart = {
    chatServer ! Connect(userName)
    chatBox.append(s"[${Tools.getDateString()}] you connected to server" + "\n")
  }

  override def postStop(): Unit = chatBox.append(s"[${Tools.getDateString()}] you disconnected from server" + "\n")

  def receive = {
    case Disconnect => chatServer ? Disconnect(userName)
                       context.stop(self)

    case body: String => chatServer ! Message(userName, body)

    case Message(userName, body, date) => chatBox.append(s"[${Tools.getDateString(date)}] [$userName]: $body" + "\n")

    case Connected(userName) => chatBox.append(s"[${Tools.getDateString()}] $userName joined to chat" + "\n")

    case Disconnected(userName) => chatBox.append(s"[${Tools.getDateString()}] $userName left chat" + "\n")
  }
}

