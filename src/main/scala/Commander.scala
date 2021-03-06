package org.furidamu.omnimote

import org.apache.commons.net.telnet.TelnetClient
import java.io.PrintStream
import java.io.BufferedReader
import java.io.InputStreamReader

import scala.actors.Actor

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

import Constants._

case class ExecuteCommand(cmd: String)
case class GetOutput(cmd: String)
case object CloseSocket

class AVActor extends Actor {
  private var s: Option[java.net.Socket] = None
  private var pw: java.io.PrintWriter = _

  def act() {
    loop {
      reactWithin(TELNET_TIMEOUT * 1000) {
        case ExecuteCommand(cmd) => execute(cmd)
        case GetOutput(cmd) => execute(cmd, true) match {
          case Left(error) => sender ! error
          case Right(output) => sender ! output
        }
        case scala.actors.TIMEOUT =>
          s match {
            case Some(sock) =>
              sock.close()
              s = None
            case _ =>
          }
        case r => log("unknown request: " + r)
      }
    }
  }

  private def execute(cmd: String, output: Boolean = false): Either[Any, String] = {
    s match {
      case None =>
        try {
          s = Some(new java.net.Socket("192.168.1.150", 23))
          s.get.setSoTimeout(200) // reads throw java.net.SocketTimeoutException after 200ms
          pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(s.get.getOutputStream()), true)
        } catch {
          case e: Exception =>
            log("failed to connect to socket")
            return Left(e)
        }
      case _ =>
    }
    pw.println(cmd)
    if(output) {
      val i = io.Source.fromInputStream(s.get.getInputStream())
      var lines = ""
      try {
        for(line <- i.getLines()) {
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
  def on() = avActor ! ExecuteCommand("PO\r\n")
  def off() = avActor ! ExecuteCommand("PF\r\n")
  def selectTuner() = avActor ! ExecuteCommand("02FN\r\n")
  def selectPC()= avActor ! ExecuteCommand("04FN\r\n")
  def selectTV() = avActor ! ExecuteCommand("10FN\r\n")
  def selectPI() = avActor ! ExecuteCommand("25FN\r\n")
  def selectAUX() = avActor ! ExecuteCommand("01FN\r\n")

  def isOn: Boolean = (avActor !! GetOutput("?P\r\n"))() match {
    case out: String => out.contains("PWR0")
    case v =>
      log(s"error: received unexpected value $v in isOn")
      false
  }

  def mute(should: Boolean) {
    if(should) avActor ! ExecuteCommand("MO\r\n")
    else avActor ! ExecuteCommand("MF\r\n")
  }

  private var curVolume: Option[Int] = None
  private var curVolumeTime = 0l

  def setVolume(negDezibel: Int) {
    curVolume = Some(negDezibel)
    curVolumeTime = compat.Platform.currentTime

    val vol = negDezibel * 2 + 161
    avActor ! ExecuteCommand("%03dVL\r\n".format(vol))
  }

  def volumeUp() = {
    val v = volume()
    setVolume(v + 2)
    v + 2
  }

  def volumeDown() = {
    val v = volume()
    setVolume(v - 2)
    v - 2
  }

  def volume(): Int = {
    if(curVolume.isEmpty || compat.Platform.currentTime - curVolumeTime > 1000 * 10) {
      curVolume = Some(fetchVolume())
      curVolumeTime = compat.Platform.currentTime
    }
    curVolume.get
  }

  private def fetchVolume(): Int = (avActor !! GetOutput("?V\r\n"))() match {
      case out: String =>
        val pattern = """VOL\d\d\d""".r
        for(line <- out.split("\n")) {
          pattern.findFirstIn(line) match {
            case Some(v) =>
              return (v.drop(3).toInt - 161) / 2
            case None =>
          }
        }
        -100
      case v =>
        log(s"error: received unexpected value $v in volume")
        -100
    }
}
