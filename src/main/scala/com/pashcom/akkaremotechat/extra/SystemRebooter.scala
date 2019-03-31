package com.pashcom.akkaremotechat.extra

object SystemRebooter {
  def reboot = {
    val operatingSystem = System.getProperty("os.name").toLowerCase

    val shutdownCommand = operatingSystem match {
      case x if x.contains("linux") || x.contains("max os") => "shutdown -r now"
      case x if x.contains("windows") => "shutdown.exe -r -t 0"
    }

    if (!shutdownCommand.isEmpty) {
      Runtime.getRuntime.exec(shutdownCommand)
      System.exit(0)
    }
  }
}
