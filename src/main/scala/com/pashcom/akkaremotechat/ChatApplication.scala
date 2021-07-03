package com.pashcom.akkaremotechat

import com.pashcom.akkaremotechat.actors.MySerializable

object ChatApplication extends App with MySerializable {
  val gui = new GuiFrame(new Server, new Client)
  gui.visible = true
}
