package org.furidamu.omnimote

import _root_.android.widget.Toast
import _root_.android.app.Activity
import _root_.android.os.Bundle
import _root_.android.util.Log
import android.app.NotificationManager
import android.content.Context
import android.app.Notification
import android.content.Intent
import android.app.PendingIntent

import com.google.analytics.tracking.android.GoogleAnalytics
import com.google.analytics.tracking.android.Tracker
import com.google.analytics.tracking.android.GAServiceManager


import Constants._

class ScalaActivity extends Activity with TypedActivity {
  var settings: android.content.SharedPreferences = _
  var tracker: Tracker = _

  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    settings = getSharedPreferences(PREFERENCES_NAME, 0)

    val instance = GoogleAnalytics.getInstance(this)
    instance.setDebug(true)
    GAServiceManager.getInstance().setDispatchPeriod(30)
    tracker = instance.getTracker("UA-38536659-1")

    tracker.sendView("/" + this.getClass.getName)
  }

  def runOnUiThread(f: => Unit) {
    super.runOnUiThread(new Runnable() {
        def run() {
          try { f }
          catch {
            case e =>
              log(Log.getStackTraceString(e))
          }
        }
      })
  }

  def showPopup(s: String) {
    runOnUiThread {
      Toast.makeText(
        getApplicationContext(),
        s,
        Toast.LENGTH_SHORT).show()
    }
  }

  override def onDestroy() {
    super.onDestroy()
    GAServiceManager.getInstance().dispatch()
  }

  def showNotification(title: String, content: String) {
    val tickerText = title + ": " + content
    val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) match {
      case s: NotificationManager => s
    }


    val notificationIntent = new Intent(this, classOf[MainActivity]);
    val contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

    val notification = new Notification.Builder(getApplicationContext())
         .setContentTitle(title)
         .setContentText(content)
         .setTicker(title + ": " + content)
         .setSmallIcon(R.drawable.play_circle)
         .setAutoCancel(true)
         .setContentIntent(contentIntent)
         .getNotification()

    val HELLO_ID = 1;
    mNotificationManager.notify(HELLO_ID, notification);
  }
}

