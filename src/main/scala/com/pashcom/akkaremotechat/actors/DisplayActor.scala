package com.pashcom.akkaremotechat.actors

import akka.actor.{Actor, Props}
import com.pashcom.akkaremotechat.extra.Tools

import scala.swing.TextArea

object DisplayActor {
  def props(textArea: TextArea): Props = Props(new DisplayActor(textArea))
}

class DisplayActor(textArea: TextArea) extends Actor {

  override def receive: Receive = {
    case message: Message =>
      val messageText = message.userName match {
        case Some(name) => s"[${Tools.getDateString(message.creationTimestamp)}] $name: ${message.body}"
        case None       => s"[${Tools.getDateString(message.creationTimestamp)}] ${message.body}"
      }

      textArea.append(messageText + "\n")
  }
}
