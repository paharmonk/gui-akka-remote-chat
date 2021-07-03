package com.pashcom.akkaremotechat

import akka.actor.{ActorRef, ActorSystem}
import com.pashcom.akkaremotechat.actors.{ClientChatActor, Disconnect, DisplayActor}
import com.pashcom.akkaremotechat.extra.Tools
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.swing.TextArea
import scala.util.{Failure, Success}

class Client {
  private var actorSystem: ActorSystem = _
  private var clientActor: ActorRef = _
  private var displayActor: ActorRef = _

  def startClient(serverIp: String, serverPort: Int,
                  localServerIp: String, localServerPort: Int,
                  userName: String,
                  logBox: TextArea,
                  onSuccess: () => Unit): Unit = {
    val connectingToServerFuture = Future {
      val clientUserName = if (userName.nonEmpty) userName else "unknown user"
      val configString = Tools.akkaRemoteConf(localServerIp, localServerPort)

      actorSystem = ActorSystem("ChatServer", ConfigFactory.parseString(configString).withFallback(ConfigFactory.load()))
      implicit val dispatcher = actorSystem.dispatcher

      val chatServerAddress = s"""akka://ChatServer@$serverIp:$serverPort/user/chatServer"""
      val remoteServerActorSelectionFuture = actorSystem.actorSelection(chatServerAddress).resolveOne(5.seconds)

      remoteServerActorSelectionFuture.onComplete {
        case Success(value) =>
          displayActor = actorSystem.actorOf(DisplayActor.props(logBox))
          clientActor = actorSystem.actorOf(ClientChatActor.props(value, clientUserName, displayActor), "chatClient")
        case Failure(exception) =>
          logBox.append(s"[${Tools.getDateString()}] connect error.\n${exception.toString}" + "\n")
      }
    }

    connectingToServerFuture.onComplete({
      case Success(_)         => onSuccess()
      case Failure(exception) => logBox.append(exception.toString + "\n")
    })
  }

  def stopClient(onSuccess: () => Unit): Unit = {
    clientActor ! Disconnect
    actorSystem.terminate()
    onSuccess()
  }

  def sendMessage(message: String): Unit = clientActor ! message
}
