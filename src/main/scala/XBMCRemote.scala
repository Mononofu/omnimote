package org.furidamu.omnimote

import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.entity.StringEntity

import Constants._

object XBMCRemote {
  var settings: android.content.SharedPreferences = _

  def sendCommand(cmd: String) { sendCommand(cmd, (_) => {}) }

  def sendCommand(cmd: String, params: String) {
    sendCommandRaw(s"""{"jsonrpc": "2.0", "method": "$cmd", "params": $params, "id": 1}""", (_) => {})
  }

  def sendCommand(cmd: String, callback: (String) => Unit) {
    sendCommandRaw(s"""{"jsonrpc": "2.0", "method": "$cmd", "id": 1}""", callback)
  }

  def sendCommand(cmd: String, params: String, callback: (String) => Unit) {
    sendCommandRaw(s"""{"jsonrpc": "2.0", "method": "$cmd", "params": $params, "id": 1}""", callback)
  }

  private def sendCommandRaw(cmd: String, callback: (String) => Unit): Unit = runInBackground {
    try {
      val client = new DefaultHttpClient()
      val req = new HttpPost(settings.getString("server", "") + "jsonrpc")
      req.setHeader("Content-Type", "application/json")
      req.setEntity(new StringEntity(cmd))
      val resRaw = client.execute(req)
      val reply = io.Source.fromInputStream(resRaw.getEntity().getContent()).mkString("")
      callback(reply)
    } catch {
      case e => log(s"failed to connect: $e")
    }
  }
}
