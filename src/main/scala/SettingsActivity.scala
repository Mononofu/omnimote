package org.furidamu.omnimote

import Constants._
import _root_.android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.KeyEvent
import android.view.View

import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.entity.StringEntity

class SettingsActivity extends ScalaActivity {
 override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    setContentView(R.layout.settings)

    findView(TR.host).text = settings.getString("server", "")

    findView(TR.host).onEditorAction_= ( (actionId: Int, e: KeyEvent) => actionId match {
        case EditorInfo.IME_ACTION_DONE =>
          log("got host: " + findView(TR.host).text)
          runInBackground {
            val rawServer = findView(TR.host).text.toString.trim
            val server = cleanServername(rawServer)
            val port = testPorts(server, rawServer)
            port match {
              case Some(port) =>
                val editor = settings.edit()
                editor.putString("server", s"$server:$port/")
                editor.commit()
                log("put port into settings")
                runOnUiThread { finish() }
              case None =>
                showPopup("It seems like you are using a non-standard port or your server is unreachable. Please check your input or manually specify the port.")
            }
          }
          true
        case _ => false
      }
    )

    //http://mononofu-nas:8088/
  }

  def cleanServername(rawServer: String): String = {
    var server = rawServer
    if(!server.startsWith("http"))
      server = "http://" + server
    if(server.endsWith("/"))
      server = server.substring(0, server.length - 1)
    if(server.lastIndexOf(":") > 6) {     // strip port
      server = server.substring(0, server.lastIndexOf(":"))
    }
    return server
  }

  def testPorts(server: String, rawServer: String): Option[Int] = {
    if(rawServer.lastIndexOf(":") > 6) {
      val port = rawServer.substring(rawServer.lastIndexOf(":") + 1).toInt
      if(checkServer(s"$server:$port/"))
        return Some(port)
      else
        return None
    }
    for(port <- List(9090, 8080, 8088, 5555)) {
      if(checkServer(s"$server:$port/"))
        return Some(port)
    }
    None
  }

  def checkServer(server: String): Boolean = {
    log(s"testing: $server")
    try {
      val client = new DefaultHttpClient()
      client.getParams().setIntParameter("http.connection.timeout", 200)
      client.getParams().setIntParameter("http.socket.timeout", 200)
      val req = new HttpPost(server + "jsonrpc")
      req.setHeader("Content-Type", "application/json")
      req.setEntity(new StringEntity("""{"jsonrpc": "2.0", "method": "Input.Info", "id": 1}"""))
      val resRaw = client.execute(req)
      val reply = io.Source.fromInputStream(resRaw.getEntity().getContent()).mkString("")
      log(reply)
      resRaw.getStatusLine().getStatusCode() == 200 && reply.contains("OK")
    } catch {
      case e =>
        log(e.toString)
        false
    }
  }
}
