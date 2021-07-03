package com.pashcom.akkaremotechat.extra

import cats.data._
import cats.implicits._

import java.text.SimpleDateFormat
import java.util.Date

object Tools {

  def akkaRemoteConf(ip: String, port: Int): String = {
    s"""akka.remote.artery{transport=tcp,canonical.hostname="$ip",canonical.port=$port}"""
  }

  def getDateString(dateMilliseconds : Long = System.currentTimeMillis): String = {
    val date = new Date(dateMilliseconds)
    val formatter = new SimpleDateFormat("dd.MM.yy HH:mm:ss")
    formatter.format(date)
  }

  private def validateIp(ip: String): ValidatedNel[String, String] = {
    val ipRegex = """^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$"""

    if (ip.matches(ipRegex)) ip.validNel
    else                     "wrong ip".invalidNel
  }

  private def validatePort(port: String): ValidatedNel[String, Int] = {
    Validated.catchNonFatal(port.toInt)
             .leftMap(_ => NonEmptyList.of("wrong port"))
  }

  def validateIpAndPort(ip: String, port: String): ValidatedNel[String, (String, Int)] = {
    (validateIp(ip), validatePort(port))
                                       .mapN((validIp, validPort) => (validIp, validPort))
  }
}
