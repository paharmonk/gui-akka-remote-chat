package com.pashcom.akkaremotechat.extra

import java.text.SimpleDateFormat
import java.util.Date

import scala.util.{Failure, Success, Try}

object Tools {

  def getDateString(dateMilisec: Long = System.currentTimeMillis()): String = {
    val date = new Date(dateMilisec)
    val formatter = new SimpleDateFormat("dd.MM.yy HH:mm:ss")
    formatter.format(date)
  }

  def validateIp(ip: String): Either[String, String] = {
    val ipRegex = """^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$"""
    if (ip.matches(ipRegex)) Left(ip)
    else                     Right("wrong ip")
  }

  def validatePort(port: String): Either[Int, String] = {
    Try { port.toInt } match {
      case Success(value) => Left(value)
      case Failure(_)     => Right("wrong port")
    }
  }
}
