package org.furidamu.omnimote

import _root_.android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ViewConfiguration

import Constants._

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
    processFling(e1.getX(), e1.getY(), e2.getX(), e2.getY())
    true
  }

  def processFling(x1: Float, y1: Float, x2: Float, y2: Float) {
    log(s"swipe from $x1 x $y1 to $x2 x $y2")
    val dx = x2 - x1
    val dy = y1 - y2
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

  override def onDoubleTap(e: MotionEvent): Boolean = {
    doubleTap()
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

  private var doubleTap: () => Unit = () => {}
  def onDoubleTap = doubleTap
  def onDoubleTap_= (f: () => Unit) {
    doubleTap = f
  }
}
