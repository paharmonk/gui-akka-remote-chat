package com.pashcom.akkaremotechat.gui

import com.pashcom.akkaremotechat.actors.{ChatClientActor, ChatServerActor, Disconnect}
import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import com.typesafe.config.ConfigFactory
import com.pashcom.akkaremotechat.extra.{SystemRebooter, Tools}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.swing.BorderPanel.Position
import scala.swing.{MenuItem, _}
import scala.util.{Failure, Success}

class Gui extends MainFrame {
  title = "ChatApp"
  preferredSize = new Dimension(500, 500)
  resizable = false

  // for server mode
  protected val serverServerIP = new TextField("127.0.0.1") {columns = 10}
  protected val serverServerPort = new TextField("228") {columns = 10}
  protected val logBoxServer = new TextArea {rows = 10;lineWrap = true;wordWrap = true;editable = false}
  protected val startServerButton = new Button(Action("start") {startServer})
  protected val stopServerButton = new Button(Action("stop") {stopServer}) {enabled = false}

  // for client mode
  protected val chatBoxClient = new TextArea {rows = 6;lineWrap = true;wordWrap = true;editable = false}
  protected val userNameField = new TextField(10) {text = "vasya"; horizontalAlignment = Alignment.Center}
  protected val sendMessageField = new TextField(32)
  protected val sendMessageButton = new Button(Action("send") {sendMessage}) {enabled = false}
  protected val startClientButton = new Button(Action("connect") {startClient})
  protected val stopClientButton = new Button(Action("disconnect") {stopClient}) {enabled = false}
  // remote server info
  protected val clientServerIP = new TextField(10)
  protected val clientServerPort = new TextField(10)
  // local server info
  protected val clientClientIP = new TextField("127.0.0.1") {columns = 10}
  protected val clientClientPort = new TextField("3002") {columns = 10}

  private val serverPanel = getServerPanel
  private val clientPanel = getClientPanel
  protected val menuModes = getMenuModes

  protected var actorSystem: ActorSystem = _
  protected var serverActor: ActorRef = _
  protected var chatClientActor: ActorRef = _

  serverPanel.visible = true
  clientPanel.visible = false

  menuBar = new MenuBar() {
    contents += menuModes
    contents += new Menu("Help") {contents += new MenuItem(Action("About")(menuShowAboutClick))}
  }

  contents = new BoxPanel(Orientation.Vertical) {
    contents += serverPanel
    contents += clientPanel
  }

  // GUI COMMANDS
  //
  // menu commands
  private def menuServerClick = {
    clientPanel.visible = false
    serverPanel.visible = true
  }

  private def menuClientClick = {
    serverPanel.visible = false
    clientPanel.visible = true
  }

  def menuShowAboutClick = {
    val res = Dialog.showConfirmation(contents.head,
                                      "Хочешь я перезагружу тебе компьютер?",
                                      optionType = Dialog.Options.YesNo,
                                      title = title)
    if (res == Dialog.Result.Ok) SystemRebooter.reboot
  }

  // server mode commands
  private def startServer: Unit = {
    val ip = Tools.validateIp(serverServerIP.text)
    val port = Tools.validatePort(serverServerPort.text)

    (ip, port) match {
      case (Left(ip), Left(port)) =>
        val startingServerFuture = Future {
          val configString = s"""akka.remote.netty.tcp{hostname="$ip",port=$port}"""

          actorSystem = ActorSystem("ChatServer", ConfigFactory.parseString(configString).withFallback(ConfigFactory.load()))
          serverActor = actorSystem.actorOf(ChatServerActor.props(logBoxServer), "chatServer")
        }

        startingServerFuture.onComplete({
          case Failure(exception) => logBoxServer.append(s"[${Tools.getDateString()}] server start error. $exception" + "\n")
          case Success(_) => menuModes.enabled = false
                             startServerButton.enabled = false
                             stopServerButton.enabled = true
                             serverServerIP.editable = false
                             serverServerPort.editable = false
        })
      case _ => if (ip.isRight) logBoxServer.append(s"[${Tools.getDateString()}] server start error. ${ip.right.get}" + "\n")
                if (port.isRight) logBoxServer.append(s"[${Tools.getDateString()}] server start error. ${port.right.get}" + "\n")
    }
  }

  private def stopServer: Unit = {
    serverActor ! PoisonPill
    actorSystem.terminate().onComplete({
      _ =>
        menuModes.enabled = true
        startServerButton.enabled = true
        stopServerButton.enabled = false
        serverServerIP.editable = true
        serverServerPort.editable = true
    })
  }

  // client mode commands
  private def startClient: Unit = {
    val ipServer = Tools.validateIp(clientServerIP.text)
    val portServer = Tools.validatePort(clientServerPort.text)
    val ipClient = Tools.validateIp(clientClientIP.text)
    val portClient = Tools.validatePort(clientClientPort.text)

    (ipServer, portServer, ipClient, portClient) match {
      case (Left(ipServer), Left(portServer),Left(ipClient), Left(portClient)) => connectToServer(ipServer, portServer, ipClient, portClient)
      case _ => if (ipServer.isRight) chatBoxClient.append(s"[${Tools.getDateString()}] server start error. ${ipServer.right.get}" + "\n")
                if (portServer.isRight) chatBoxClient.append(s"[${Tools.getDateString()}] server start error. ${portServer.right.get}" + "\n")
                if (ipClient.isRight) chatBoxClient.append(s"[${Tools.getDateString()}] server start error. ${ipClient.right.get}" + "\n")
                if (portClient.isRight) chatBoxClient.append(s"[${Tools.getDateString()}] server start error. ${portClient.right.get}" + "\n")

    }
  }

  private def stopClient: Unit = {
    chatClientActor ! Disconnect
    actorSystem.terminate()
    menuModes.enabled = true
    sendMessageButton.enabled = false
    stopClientButton.enabled = false
    startClientButton.enabled = true
    userNameField.enabled = true
    clientServerIP.enabled = true
    clientServerPort.enabled = true
    clientClientIP.enabled = true
    clientClientPort.enabled = true
  }

  private def connectToServer(serverIp: String, serverPort: Int, localServerIp: String, localServerPort: Int): Unit = {
    val connectingToServer = Future {
      val userName = if (userNameField.text.length > 0) userNameField.text else "unknown user"

      val configString = s"""akka.remote.netty.tcp{hostname="$localServerIp",port=$localServerPort}"""

      actorSystem = ActorSystem("ChatServer", ConfigFactory.parseString(configString).withFallback(ConfigFactory.load()))

      implicit val dispatcher = actorSystem.dispatcher

      val chatServerAddress = s"""akka.tcp://ChatServer@$serverIp:$serverPort/user/chatServer"""
      val remoteServerActorFuture = actorSystem.actorSelection(chatServerAddress).resolveOne(5 seconds)

      remoteServerActorFuture.onComplete {
        case Success(value) => chatClientActor = actorSystem.actorOf(ChatClientActor.props(value, userName, chatBoxClient), "chatClient")
        case Failure(exception) => chatBoxClient.append(s"[${Tools.getDateString()}] connect error.\n${exception.toString}" + "\n")
      }
    }
    connectingToServer.onComplete({
      case Success(_) =>
        menuModes.enabled = false
        sendMessageButton.enabled = true
        startClientButton.enabled = false
        stopClientButton.enabled = true
        userNameField.enabled = false
        clientServerIP.enabled = false
        clientServerPort.enabled = false
        clientClientIP.enabled = false
        clientClientPort.enabled = false
      case Failure(exception) =>
        chatBoxClient.append(exception.toString + "\n")
    })
  }

  private def sendMessage = {
    val newMessage = sendMessageField.text
    if (newMessage.length > 0) {
      chatClientActor ! newMessage
      sendMessageField.text = ""
    }
  }

  // GET GUI ELEMENTS
  def getServerPanel: BorderPanel = {
    new BorderPanel() {
      add(new BoxPanel(Orientation.Vertical) {
        contents += Swing.VStrut(3)
        contents += new ScrollPane(logBoxServer)
        contents += Swing.VStrut(3)
      }, Position.Center)
      add(new BoxPanel(Orientation.Vertical) {
        contents += new GridPanel(2, 2) {
          contents += new Label("server ip") {horizontalAlignment = Alignment.Left}
          contents += serverServerIP
          contents += new Label("server port") {horizontalAlignment = Alignment.Left}
          contents += serverServerPort
        }
        contents += Swing.VStrut(3)
        contents += new GridPanel(1, 2) {
          contents += startServerButton
          contents += stopServerButton
        }
      }, Position.South)

      contents.foreach(_.xLayoutAlignment = 0.0)
      border = Swing.EmptyBorder(3, 3, 3, 3)
    }
  }

  def getMenuModes = {
    new Menu("Modes") {
      contents += new MenuItem(Action("Server")(menuServerClick))
      contents += new MenuItem(Action("Client")(menuClientClick))
    }
  }

  def getClientPanel = {
    new BorderPanel() {
      add(new BoxPanel(Orientation.Vertical) {
        contents += Swing.VStrut(3)
        contents += new ScrollPane(chatBoxClient)
        contents += Swing.VStrut(3)
      }, Position.Center)
      add(new BoxPanel(Orientation.Vertical) {
        contents += new BoxPanel(Orientation.Horizontal) {
          contents += userNameField
          contents += sendMessageField
          contents += sendMessageButton
        }
        contents += Swing.VStrut(3)
        contents += new GridPanel(4, 2) {
          contents += new Label("server ip") {horizontalAlignment = Alignment.Left}
          contents += clientServerIP
          contents += new Label("server port") {horizontalAlignment = Alignment.Left}
          contents += clientServerPort
          contents += new Label("local server ip") {horizontalAlignment = Alignment.Left}
          contents += clientClientIP
          contents += new Label("local server port") {horizontalAlignment = Alignment.Left}
          contents += clientClientPort
        }
        contents += Swing.VStrut(3)
        contents += new GridPanel(1, 2) {
          contents += startClientButton
          contents += stopClientButton
        }
      }, Position.South)

      contents.foreach(_.xLayoutAlignment = 0.0)
      border = Swing.EmptyBorder(3, 3, 3, 3)
    }
  }
}
