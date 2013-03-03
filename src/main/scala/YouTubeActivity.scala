package org.furidamu.omnimote

import _root_.android.os.Bundle
import android.content.Intent

import Constants._
import XBMCRemote._

class YouTubeActivity extends ScalaActivity {
  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    setContentView(R.layout.main)

    val intent = getIntent()
    intent.getExtras() match {
      case null =>
      case extras =>
        extras.getString(Intent.EXTRA_TEXT) match {
        case null =>
        case url =>
          log(s"trying to send $url")
          val videoId = if (url.contains("&") && (url.indexOf("v=") < url.indexOf("&")))
              url.substring((url.indexOf("v=")+2), (url.indexOf("&", url.indexOf("v=") + 2)))
            else
              url.substring((url.indexOf("v=")+2))
          log(s"parsed out $videoId")
          sendCommand("Playlist.Clear", """{"playlistid": 1}""", r => {
            sendCommand("Playlist.Add", s"""{"playlistid": 1, "item": {"file": "plugin://plugin.video.youtube/?action=play_video&videoid=$videoId"}}""", r => {
              sendCommand("Player.Open", """{"item": {"playlistid": 1, "position": 0}}""")
              })
            })
      }
    }
    finish()
  }
}
