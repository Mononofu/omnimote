package org.furidamu.omnimote

import org.apache.commons.net.telnet.TelnetClient
import java.io.PrintStream
import java.io.BufferedReader
import java.io.InputStreamReader

import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging
import akka.actor.Cancellable

import akka.util.duration._
import akka.util.Timeout
import akka.pattern.ask
import akka.dispatch.Await


import Constants._

case class ExecuteCommand(cmd: String)
case class GetOutput(cmd: String)
case object CloseSocket

class AVActor extends Actor {
  val log = Logging(context.system, this)
  private var s: Option[java.net.Socket] = None
  private var pw: java.io.PrintWriter = _
  private var timeout: Option[Cancellable] = None

  def receive = {
    case ExecuteCommand(cmd) => execute(cmd)
    case GetOutput(cmd) => execute(cmd, true) match {
      case Left(error) => sender ! error
      case Right(output) => sender ! output
    }
    case CloseSocket =>
      s.get.close()
      s = None
    case r => log.info("unknown request: " + r)
  }

  private def resetTimeout() {
    timeout.map(c => c.cancel)
    timeout = Some(system.scheduler.scheduleOnce(TELNET_TIMEOUT seconds, self, CloseSocket))
  }

  private def execute(cmd: String, output: Boolean = false): Either[Any, String] = {
    resetTimeout()
    s match {
      case None =>
        try {
          s = Some(new java.net.Socket("192.168.1.150", 23))
          s.get.setSoTimeout(200) // reads throw java.net.SocketTimeoutException after 200ms
          pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(s.get.getOutputStream()), true)
        } catch {
          case e: Exception =>
            log.error("failed to connect to socket")
            return Left(akka.actor.Status.Failure(e))
        }
      case _ =>
    }
    pw.println(cmd)
    if(output) {
      val i = io.Source.fromInputStream(s.get.getInputStream())
      var lines = ""
      try {
        for(line <- i.getLines()) {
          log.info("got line: " + line)
          lines += line + "\n"
        }
        Right(lines)
      } catch {
        case ex: java.net.SocketTimeoutException =>
          return Right(lines)
      }
    } else {
      Right("")
    }
  }
}


object AVRemote {

  implicit val timeout = Timeout(1 seconds)

  def on() = avActor ! ExecuteCommand("PO\r\n")
  def off() = avActor ! ExecuteCommand("PF\r\n")
  def selectTuner() = avActor ! ExecuteCommand("02FN\r\n")
  def selectPC()= avActor ! ExecuteCommand("04FN\r\n")
  def selectTV() = avActor ! ExecuteCommand("10FN\r\n")
  def selectPI() = avActor ! ExecuteCommand("25FN\r\n")
  def selectAUX() = avActor ! ExecuteCommand("01FN\r\n")

  def isOn: Boolean = {
    val future = avActor ? GetOutput("?P\r\n")
    val out = Await.result(future, timeout.duration).asInstanceOf[String]
    out.contains("PWR0")
  }

  def mute(should: Boolean) {
    if(should) avActor ! ExecuteCommand("MO\r\n")
    else avActor ! ExecuteCommand("MF\r\n")
  }

  def setVolume(negDezibel: Int) {
    val vol = negDezibel * 2 + 161
    avActor ! ExecuteCommand("%03dVL\r\n".format(vol))
  }

  def volumeUp() = setVolume(volume + 2)
	def volumeDown() = setVolume(volume - 2)

  def volume: Int = {
    val future = avActor ? GetOutput("?V\r\n")
    val out = Await.result(future, timeout.duration).asInstanceOf[String]
    val pattern = """VOL\d\d\d""".r

    for(line <- out.split("\n")) {
      pattern.findFirstIn(line) match {
        case Some(v) =>
          return (v.drop(3).toInt - 161) / 2
        case None =>
      }
    }
    -100
  }
}
