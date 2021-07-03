package com.pashcom.akkaremotechat

import cats.data.Validated
import cats.implicits._
import com.pashcom.akkaremotechat.extra.{SystemRebooter, Tools}

import scala.swing.BorderPanel.Position
import scala.swing.{MenuItem, _}

class GuiFrame(server: Server, client: Client) extends MainFrame {
  title = "ChatApp"
  preferredSize = new Dimension(500, 500)
  resizable = false

  // for server mode
  private val serverServerIP    = new TextField("127.0.0.1") {columns = 10}
  private val serverServerPort  = new TextField("228") {columns = 10}
  private val logBoxServer      = new TextArea {rows = 10;lineWrap = true;wordWrap = true;editable = false}
  private val startServerButton = new Button(Action("start") {startServer})
  private val stopServerButton  = new Button(Action("stop") {stopServer}) {enabled = false}

  // for client mode
  private val chatBoxClient     = new TextArea {rows = 6;lineWrap = true;wordWrap = true;editable = false}
  private val userNameField     = new TextField(10) {text = "vasya"; horizontalAlignment = Alignment.Center}
  private val sendMessageField  = new TextField(32)
  private val sendMessageButton = new Button(Action("send") {sendMessage}) {enabled = false}
  private val startClientButton = new Button(Action("connect") {startClient})
  private val stopClientButton  = new Button(Action("disconnect") {stopClient}) {enabled = false}
  // remote server info
  private val clientServerIP    = new TextField(10)
  private val clientServerPort  = new TextField(10)
  // local server info
  private val clientClientIP    = new TextField("127.0.0.1") {columns = 10}
  private val clientClientPort  = new TextField("3002") {columns = 10}

  private val serverPanel = getServerPanel
  private val clientPanel = getClientPanel
  private val menuModes   = getMenuModes

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
  private def menuShowAboutClick: Unit = {
    val res = Dialog.showConfirmation(contents.head,
                                      "Хочешь я перезагружу тебе компьютер?",
                                      optionType = Dialog.Options.YesNo,
                                      title = title)
    if (res == Dialog.Result.Ok) SystemRebooter.reboot
  }

  // server mode commands
    private def startServer: Unit = {
      startServerButton.enabled = false

      Tools.validateIpAndPort(serverServerIP.text, serverServerPort.text) match {
        case Validated.Valid((ip, port)) =>
          server.startServer(ip, port, logBoxServer, () => blockServerGuiElements(true))

        case Validated.Invalid(errors) =>
          startServerButton.enabled = true
          errors.toList.foreach(errorStr =>
            logBoxServer.append(s"[${Tools.getDateString()}] server start error. $errorStr" + "\n"))
      }
    }

  private def stopServer: Unit = {
    server.stopServer(() => blockServerGuiElements(false))
  }

  // client mode commands
  private def startClient: Unit = {
    val validatedServerData = Tools.validateIpAndPort(clientServerIP.text, clientServerPort.text)
    val validatedClientData = Tools.validateIpAndPort(clientClientIP.text, clientClientPort.text)

    val fullValidated = (validatedServerData, validatedClientData)
                            .mapN((validServer, validClient) => (validServer, validClient))

    fullValidated match {
      case Validated.Valid((serverData, clientData)) =>
        client.startClient(serverData._1, serverData._2,
                           clientData._1, clientData._2,
                           userNameField.text, chatBoxClient,
                           () => blockClientGuiElements(true))

      case Validated.Invalid(e) =>
        e.toList.foreach(errorStr => chatBoxClient.append(s"[${Tools.getDateString()}] client start error. $errorStr" + "\n"))
    }
  }

  private def stopClient: Unit = {
    client.stopClient(() => blockClientGuiElements(false))
  }

  private def sendMessage = {
    val newMessage = sendMessageField.text
    if (newMessage.nonEmpty) {
      client.sendMessage(newMessage)
      sendMessageField.text = ""
    }
  }

  // GET GUI ELEMENTS
  private def getServerPanel: BorderPanel = {
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

  private def getMenuModes: Menu = {
    // menu commands
    def menuServerClick = {
      clientPanel.visible = false
      serverPanel.visible = true
    }

    def menuClientClick = {
      serverPanel.visible = false
      clientPanel.visible = true
    }

    new Menu("Modes") {
      contents += new MenuItem(Action("Server")(menuServerClick))
      contents += new MenuItem(Action("Client")(menuClientClick))
    }
  }

 private def getClientPanel = {
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

  private def blockClientGuiElements(block: Boolean): Unit = {
    if (block) {
      menuModes.enabled = false
      sendMessageButton.enabled = true
      startClientButton.enabled = false
      stopClientButton.enabled = true
      userNameField.enabled = false
      clientServerIP.enabled = false
      clientServerPort.enabled = false
      clientClientIP.enabled = false
      clientClientPort.enabled = false
    } else {
      menuModes.enabled = true
      sendMessageButton.enabled = false
      stopClientButton.enabled  = false
      startClientButton.enabled = true
      userNameField.enabled     = true
      clientServerIP.enabled    = true
      clientServerPort.enabled  = true
      clientClientIP.enabled    = true
      clientClientPort.enabled  = true
    }
  }

  private def blockServerGuiElements(block: Boolean): Unit = {
    if (block) {
      menuModes.enabled = false
      startServerButton.enabled = false
      stopServerButton.enabled = true
      serverServerIP.editable = false
      serverServerPort.editable = false
    } else {
      menuModes.enabled = true
      startServerButton.enabled = true
      stopServerButton.enabled  = false
      serverServerIP.editable   = true
      serverServerPort.editable = true
    }
  }
}
