package org.furidamu.omnimote

import _root_.android.app.Activity
import _root_.android.os.Bundle
import _root_.android.graphics.Typeface
import _root_.android.view.KeyEvent
import _root_.android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration


import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.entity.StringEntity

import Constants._


class MainActivity extends Activity with TypedActivity {
  var gestureDetector: GestureDetector = _

  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    setContentView(R.layout.main)

    val listener = new MyGestureListener(ViewConfiguration.get(getApplicationContext()))
    listener.onFlingLeft = (multitouch: Boolean) => {
      if(multitouch) { sendCommand("Input.Back") }
      else { sendCommand("Input.Left") }
    }

    listener.onFlingRight = (_) => sendCommand("Input.Right")
    listener.onFlingUp = (_) => sendCommand("Input.Up")
    listener.onFlingDown = (_) => sendCommand("Input.Down")
    listener.onSingleTap = () => sendCommand("Input.Select")

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
      if(e.getActionMasked() != 2)
        log(s"touch event: ${e.getX()}x${e.getY()} - ${e.getPointerId(e.getActionIndex())} - ${e.getActionMasked()}")
      if(e.getPointerId(e.getActionIndex()) > 0) {
        // touch with second finger
        listener.lastMultitouch = compat.Platform.currentTime
      }
      gestureDetector.onTouchEvent(e)
    }

  }

  // suppresses the volume change sound
  override def onKeyUp(keyCode: Int, event: KeyEvent): Boolean = keyCode match {
    case KeyEvent.KEYCODE_VOLUME_DOWN => true
    case KeyEvent.KEYCODE_VOLUME_UP => true
    case _ => super.dispatchKeyEvent(event)
  }

  override def onKeyDown(keyCode: Int, event: KeyEvent): Boolean = keyCode match {
    case KeyEvent.KEYCODE_VOLUME_DOWN => AVRemote.volumeDown(); true
    case KeyEvent.KEYCODE_VOLUME_UP => AVRemote.volumeUp(); true
    case _ => super.dispatchKeyEvent(event)
  }


  def sendCommand(cmd: String) = runInBackground {
    val client = new DefaultHttpClient()
    val req = new HttpPost("http://mononofu-nas:8088/jsonrpc")
    req.setHeader("Content-Type", "application/json")
    req.setEntity(new StringEntity(s"""{"jsonrpc": "2.0", "method": "$cmd", "id": 1}"""))
    val resRaw = client.execute(req)
    log(resRaw.getStatusLine().getStatusCode() + ": " +
      io.Source.fromInputStream(resRaw.getEntity().getContent()).mkString(""))
  }

}
