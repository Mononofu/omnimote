package org.furidamu.omnimote

import _root_.android.app.Activity
import _root_.android.os.Bundle
import _root_.android.graphics.Typeface
import _root_.android.view.KeyEvent
import _root_.android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.os.Handler


import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.entity.StringEntity

import Constants._


class MainActivity extends Activity with TypedActivity {
  var gestureDetector: GestureDetector = _
  var listener: MyGestureListener = _
  var playingMovie = false
  var showingOSD = false
  val delayBeforeMove = 300             // in milliseconds
  val buttonPressFrequency = 10
  var touchStartTime = 0l                    // in milliseconds
  var touchStartY = 0f
  var touchCurrentY = 0f

  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    setContentView(R.layout.main)

    listener = new MyGestureListener(ViewConfiguration.get(getApplicationContext()))
    listener.onFlingLeft = (multitouch: Boolean) => {
      if(multitouch) { sendCommand("Input.Back") }
      else {
        if(playingMovie && !showingOSD) sendCommand("Player.Seek", """{"playerid": 1, "value": "smallbackward"}""")
        else sendCommand("Input.Left")
      }
    }
    listener.onFlingRight = (_) => {
      if(playingMovie && !showingOSD) sendCommand("Player.Seek", """{"playerid": 1, "value": "smallforward"}""")
      else sendCommand("Input.Right")
    }
    listener.onFlingUp = (_) => sendCommand("Input.Up")
    listener.onFlingDown = (multitouch: Boolean) => {
      if(multitouch) {
        if(playingMovie) sendCommand("Input.ShowOSD")
        else sendCommand("Input.ContextMenu")
      }
      else {
        if(playingMovie && !showingOSD) sendCommand("Input.ShowOSD")
        else sendCommand("Input.Down")
      }
    }
    listener.onSingleTap = () => {
      if(playingMovie && !showingOSD) sendCommand("Player.PlayPause", """{ "playerid": 1 }""")
      else sendCommand("Input.Select")
    }
    listener.onDoubleTap = () => sendCommand("Player.Stop", """{ "playerid": 1 }""")

    gestureDetector = new GestureDetector(listener)

    val fontawesome = Typeface.createFromAsset(getAssets(), "fontawesome.ttf")

    findView(TR.tuner_btn).setTypeface(fontawesome)
    findView(TR.tv_btn).setTypeface(fontawesome)
    findView(TR.pc_btn).setTypeface(fontawesome)
    findView(TR.pi_btn).setTypeface(fontawesome)

    findView(TR.tuner_btn).onClick = () => AVRemote.selectTuner()
    findView(TR.tv_btn).onClick = () => AVRemote.selectTV()
    findView(TR.pc_btn).onClick = () => AVRemote.selectPC()
    findView(TR.pi_btn).onClick = () => AVRemote.selectPI()

    findView(TR.touchpad).onTouch = (e: MotionEvent) => {
      touchCurrentY = e.getY()
      e.getActionMasked() match {
        case 0 =>
          touchStartTime = compat.Platform.currentTime
          touchStartY = e.getY()
          moveCursor.run()
        case 1 =>
          cursorHandler.removeCallbacks(moveCursor)
          findView(TR.touchpad).clear(true)
        case _ =>
      }
      //log(s"touch event: ${e.getX()}x${e.getY()} - ${e.getPointerId(e.getActionIndex())} - ${e.getActionMasked()}")
      if(e.getPointerId(e.getActionIndex()) > 0) {
        // touch with second finger
        listener.lastMultitouch = compat.Platform.currentTime
      }
      gestureDetector.onTouchEvent(e)
    }
  }

  private val moveCursor: Runnable = new Runnable() {
    override def run() {
      var freq: Double = buttonPressFrequency
      if(compat.Platform.currentTime - touchStartTime > delayBeforeMove) {
        val height = findView(TR.touchpad).getHeight()
        if(Math.abs(touchStartY - touchCurrentY) > height / 10) {
          listener.processFling(0, touchStartY, 0, touchCurrentY)
          freq *= (Math.exp(2*(Math.abs(touchStartY - touchCurrentY) / height - 0.1)) - 0.9)
        }
      }
      cursorHandler.postDelayed(moveCursor, (1000.0f / freq).toLong)
    }
  }

  override def onResume() {
    super.onResume()
    checkPlayState.run()
  }

  override def onPause() {
    super.onPause()
    playStateHandler.removeCallbacks(checkPlayState)
    cursorHandler.removeCallbacks(moveCursor)
  }

  val playStateHandler = new Handler()
  val cursorHandler = new Handler()

  private val checkPlayState: Runnable = new Runnable() {
    override def run() {
      sendCommand("Player.GetActivePlayers", (reply: String) => {
        val json = parseJSON(reply)
        if(json.result.length > 0) {
          playingMovie = true
        } else {
          playingMovie = false
        }
      })

      sendCommand("GUI.GetProperties", """{"properties": ["currentwindow"]}""", (reply: String) => {
        val json = parseJSON(reply)
        val label = json.result.currentwindow.label.toString
        if(!label.contains("video")) {
          showingOSD = true
        } else {
          showingOSD = false
        }
      })

      playStateHandler.postDelayed(checkPlayState, 1000*5)
    }
  }

  // suppresses the volume change sound
  override def onKeyUp(keyCode: Int, event: KeyEvent): Boolean = keyCode match {
    case KeyEvent.KEYCODE_VOLUME_DOWN => true
    case KeyEvent.KEYCODE_VOLUME_UP => true
    case KeyEvent.KEYCODE_BACK => sendCommand("Input.Back"); true
    case _ => false
  }

  override def onKeyDown(keyCode: Int, event: KeyEvent): Boolean = keyCode match {
    case KeyEvent.KEYCODE_VOLUME_DOWN => AVRemote.volumeDown(); true
    case KeyEvent.KEYCODE_VOLUME_UP => AVRemote.volumeUp(); true
    case _ => false
  }

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

  private def sendCommandRaw(cmd: String, callback: (String) => Unit) = runInBackground {
    try {
      val client = new DefaultHttpClient()
      val req = new HttpPost("http://mononofu-nas:8088/jsonrpc")
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
