With a variety of gestures, Omnimote allows you to easily control XBMC without looking at your phone. The gestures' meaning varies depending on context, but is always intuitive. Below is a full list:

While watching a movie:
- swipe left/right: skip backward/forward
- (two-finger) swipe down: context menu
- two-finger swipe left: back
- single tap: play/pause
- double tap: stop

While in any menu:
- normal swipes: arrow keys - move in that direction
- two-finger swipe down: context menu
- two-finger swipe left: back
- single tap: select
- swipe up/down and hold: scrolling

Use the volume keys to change the volume XBMC is playing at.

If you have suggestions or want to request a feature, please don't hesitate to contact me via email (j.schrittwieser@gmail.com) or open a request on Github: https://github.com/Mononofu/omnimote/issues

The app is fully open source and licensed under GPL v3, you can find the source here: https://github.com/Mononofu/omnimote


Installation
============

First, install sbt if you don't have it. Instructions: http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html

I'm using the sbt-android plugin. To allow it to work, make sure that ANDROID_HOME is set correctly to the location of your android SDK. (see https://github.com/jberkel/android-plugin/wiki/getting-started)

Finally, simply run 'sbt android:start-device' to build the app and install it on your phone
