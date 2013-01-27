package org.furidamu.wakeontelnet

import org.apache.commons.net.telnet.TelnetClient
import java.io.PrintStream
import java.io.BufferedReader
import java.io.InputStreamReader

object Commander {
	var outStream: PrintStream = null
	var inStream: BufferedReader = null
	val telnet = new TelnetClient()
	var sessionStarted = -1l

	def startSession() {
		sessionStarted = System.currentTimeMillis()
		telnet.connect( "192.168.1.150", 23)
		telnet.setSoTimeout(300)
		outStream = new PrintStream( telnet.getOutputStream())
		inStream = new BufferedReader(new InputStreamReader(telnet.getInputStream()))
	}

	def endSession() {
		telnet.disconnect()
		outStream = null
		inStream = null
		sessionStarted -1l
	}

	def sendCommand(command: String, checkReply: Boolean = false): String = {
		try {
				var reply = "Executed command: " + command
				if(null == outStream)
					startSession()
				outStream.print(command)
				outStream.flush()
				if(checkReply)
					reply = inStream.readLine()
				
				endSession()
				return reply
	    	}
	    	catch  {
	    		case e: java.net.ConnectException =>
		    		return "failed to connect to server"
		    	case e: java.net.SocketException =>
		    		return "permission denied"
	    	}
	}

	def receiverOn() = sendCommand("PO\r\n")
	def receiverOff() = sendCommand("PF\r\n")

	def receiverMute() = sendCommand("MO\r\n")
	def receiverUnmute() = sendCommand("MF\r\n")

	def setVolume(negDezibel: Int) = sendCommand("%03dVL\r\n".format(negDezibel * 2 + 161))

	def selectTuner() = sendCommand("02FN\r\n")
	def selectLaptop() = sendCommand("04FN\r\n")
	def selectTV() = sendCommand("10FN\r\n")
}