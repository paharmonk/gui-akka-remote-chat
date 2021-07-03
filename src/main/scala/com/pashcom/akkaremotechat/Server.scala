package com.pashcom.akkaremotechat

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import com.pashcom.akkaremotechat.actors.{DisplayActor, ServerChatActor}
import com.pashcom.akkaremotechat.extra.Tools
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.swing.TextArea
import scala.util.{Failure, Success}

class Server {
  private var actorSystem: ActorSystem = _
  private var serverActor: ActorRef = _

  def startServer(ip: String,
                  port: Int,
                  logBox: TextArea,
                  onSuccess: () => Unit): Unit = {
    val startingServerFuture = Future {
      val configString = Tools.akkaRemoteConf(ip, port)

      actorSystem = ActorSystem("ChatServer", ConfigFactory.parseString(configString).withFallback(ConfigFactory.load()))

      val displayActor = actorSystem.actorOf(DisplayActor.props(logBox))
      serverActor = actorSystem.actorOf(ServerChatActor.props(displayActor), "chatServer")
    }

    startingServerFuture.onComplete({
      case Failure(exception) =>
        val message = exception.getStackTrace.toList.map(_.toString)
        logBox.append(s"[${Tools.getDateString()}] server start error. $message" + "\n")

      case Success(_) =>
        onSuccess()
    })
  }

  def stopServer(onSuccess: () => Unit): Unit = {
    serverActor ! PoisonPill
    actorSystem.terminate()
               .onComplete(_ => onSuccess())
  }
}
