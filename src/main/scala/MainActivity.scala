package org.furidamu.omnimote

import _root_.android.app.Activity
import _root_.android.os.Bundle
import _root_.android.graphics.Typeface
import _root_.android.view.KeyEvent
import _root_.android.view.GestureDetector
import _root_.android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ViewConfiguration

import android.gesture.GestureLibraries
import android.gesture.GestureOverlayView
import android.gesture.Gesture



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

    /*val gestureLibrary = GestureLibraries.fromRawResource(this, R.raw.gestures);
    gestureLibrary.load();

    findViewById(R.id.touchpad).asInstanceOf[GestureOverlayView].onGesture = (g: Gesture) => {
      val predictions = gestureLibrary.recognize(g)
      if(predictions.size() > 0) {
        log("got gesture: " + predictions.get(0).name)
      }
    }*/

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


class MyGestureListener(vc: ViewConfiguration) extends SimpleOnGestureListener {
  val SWIPE_MIN_DISTANCE = vc.getScaledPagingTouchSlop()                //  32
  val SWIPE_MAX_OFF_PATH = vc.getScaledTouchSlop()                      //  16
  val SWIPE_THRESHOLD_VELOCITY = vc.getScaledMinimumFlingVelocity()     // 100

  var lastMultitouch = 0l       // time of last multitouch in milliseconds


  log(s"SWIPE_MIN_DISTANCE: $SWIPE_MIN_DISTANCE")
  log(s"SWIPE_MAX_OFF_PATH: $SWIPE_MAX_OFF_PATH")
  log(s"SWIPE_THRESHOLD_VELOCITY: $SWIPE_THRESHOLD_VELOCITY")


  override def onDown(e: MotionEvent): Boolean = {
    log(s"onDown: ${e.getX()}x${e.getY()}")
    true
  }

  override def onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = {
    log(s"swipe from ${e1.getX()}x${e1.getY()} to ${e2.getX()}x${e2.getY()}")
    log(s"${e1.getPointerCount()}, ${e2.getPointerCount()}")
    val dx = e2.getX() - e1.getX()
    val dy = e1.getY() - e2.getY()
    val r = Math.sqrt(dx*dx + dy*dy)
    val phi = Math.atan2(dy, dx)
    var PI = Math.PI
    log(s"\t$dx horizontal, $dy vertical == ($r, $phi)")

    val multitouch = (compat.Platform.currentTime - lastMultitouch < 100)

    if(r < SWIPE_MIN_DISTANCE) {
      log("too short")
      return false
    }

    phi match {
      case phi if phi < PI/4 && phi > -PI/4 => flingRight(multitouch)
      case phi if phi < 3*PI/4 && phi > PI/4 => flingUp(multitouch)
      case phi if phi < -3*PI/4 || phi > 3*PI/4 => flingLeft(multitouch)
      case phi if phi < -PI/4 && phi > -3*PI/4 => flingDown(multitouch)
    }

    true
  }

  override def onSingleTapConfirmed(e: MotionEvent): Boolean = {
    singleTap()
    true
  }

  private var flingLeft: (Boolean) => Unit = (multitouch: Boolean) => {}
  def onFlingLeft = flingLeft
  def onFlingLeft_= (f: (Boolean) => Unit) {
    flingLeft = f
  }

  private var flingRight: (Boolean) => Unit = (multitouch: Boolean) => {}
  def onFlingRight = flingRight
  def onFlingRight_= (f: (Boolean) => Unit) {
    flingRight = f
  }

  private var flingUp: (Boolean) => Unit = (multitouch: Boolean) => {}
  def onFlingUp = flingUp
  def onFlingUp_= (f: (Boolean) => Unit) {
    flingUp = f
  }

  private var flingDown: (Boolean) => Unit = (multitouch: Boolean) => {}
  def onFlingDown = flingDown
  def onFlingDown_= (f: (Boolean) => Unit) {
    flingDown = f
  }

  private var singleTap: () => Unit = () => {}
  def onSingleTap = singleTap
  def onSingleTap_= (f: () => Unit) {
    singleTap = f
  }
}
