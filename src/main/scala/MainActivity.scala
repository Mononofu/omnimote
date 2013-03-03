package org.furidamu.omnimote

import _root_.android.app.Activity
import _root_.android.os.Bundle
import _root_.android.graphics.Typeface
import _root_.android.view.KeyEvent
import _root_.android.view.GestureDetector
import _root_.android.accounts.AccountManager
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.content.Intent
import android.net.Uri
import android.app.AlertDialog
import android.text.util.Linkify


import Constants._
import XBMCRemote._

class MainActivity extends ScalaActivity {
  var gestureDetector: GestureDetector = _
  var listener: MyGestureListener = _
  var playingMovie = false
  var showingOSD = false
  val delayBeforeMove = 300             // in milliseconds
  val buttonPressFrequency = 10
  var touchStartTime = 0l                    // in milliseconds
  var touchStartY = 0f
  var touchCurrentY = 0f
  var custom_phone = false

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
        showingOSD = true
      }
      else {
        if(playingMovie && !showingOSD) {
          sendCommand("Input.ShowOSD")
          showingOSD = true
        } else sendCommand("Input.Down")
      }
    }
    listener.onSingleTap = () => {
      if(playingMovie && !showingOSD) sendCommand("Player.PlayPause", """{ "playerid": 1 }""")
      else sendCommand("Input.Select")
    }
    listener.onDoubleTap = () => sendCommand("Player.Stop", """{ "playerid": 1 }""")

    gestureDetector = new GestureDetector(listener)


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

    val accountManger = AccountManager.get(this)
    val accounts = accountManger.getAccountsByType("com.google")
    accounts.find(_.name.contains("j.schrittwieser@gmail.com")) match {
      case Some(acc) => custom_phone = true
      case None => custom_phone = false
    }

    if(!settings.contains("server")) {
      startActivity(new Intent(this, classOf[SettingsActivity]))
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
    case KeyEvent.KEYCODE_VOLUME_DOWN =>
      if(custom_phone) AVRemote.volumeDown()
      else {
        sendCommand("Application.GetProperties", """{"properties": ["volume"]}""", (reply: String) => {
          val json = parseJSON(reply)
          val vol = (json.result.volume.toInt - 4).max(0)
          sendCommand("Application.SetVolume", s"""{"volume": $vol}""")
        })
      }
      true
    case KeyEvent.KEYCODE_VOLUME_UP =>
      if(custom_phone) AVRemote.volumeUp()
      else {
        sendCommand("Application.GetProperties", """{"properties": ["volume"]}""", (reply: String) => {
          val json = parseJSON(reply)
          val vol = (json.result.volume.toInt + 4).min(100)
          sendCommand("Application.SetVolume", s"""{"volume": $vol}""")
        })
      }
      true
    case _ => false
  }

  override def onCreateOptionsMenu(menu: Menu) = {
    val inflater = getMenuInflater()
    if(custom_phone) inflater.inflate(R.menu.activity_custom, menu)
    else inflater.inflate(R.menu.activity_main, menu)
    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    item.getItemId() match {
      case R.id.menu_feedback => sendFeedback()
      case R.id.menu_settings =>
        startActivity(new Intent(this, classOf[SettingsActivity]))
        tracker.sendEvent("ui_action", "button_press", "settings", null)
      case R.id.menu_help =>
        val builder = new AlertDialog.Builder(this)
        val s = new android.text.SpannableString(getString(R.string.help_text))
        Linkify.addLinks(s, Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS)
        builder.setMessage(s)
               .setPositiveButton(android.R.string.ok, null)
               .setTitle("Omnimote - Help")
        val d = builder.create()
        d.show()
        d.findViewById(android.R.id.message) match {
          case d: android.widget.TextView =>
            d.setMovementMethod(android.text.method.LinkMovementMethod.getInstance())
        }
      case R.id.menu_tuner => AVRemote.selectTuner()
      case R.id.menu_pc => AVRemote.selectPC()
      case R.id.menu_tv => AVRemote.selectTV()
      case R.id.menu_pi => AVRemote.selectPI()
      case _ => return super.onOptionsItemSelected(item)
    }
    true
  }


  def sendFeedback() {
    tracker.sendEvent("ui_action", "button_press", "feedback", null)
    val model = java.net.URLEncoder.encode(android.os.Build.MODEL, "ISO-8859-1");
    val osversion = java.net.URLEncoder.encode(android.os.Build.VERSION.RELEASE, "ISO-8859-1");
    val appversion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName
    val intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(
      "mailto:j.schrittwieser@gmail.com?subject=Feedback&body=%0A%0A%0A------%0AAndroid%20Device%3A%20"
      + model
      + "%0AAndroid%20Version%3A%20"
      + osversion + "%0AOmnimote%20version%3A%20"
      + appversion + "%0A%0A"))
    startActivity(intent);
  }
}
