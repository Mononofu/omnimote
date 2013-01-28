package org.furidamu.omnimote

import _root_.android.app.Activity
import _root_.android.os.Bundle
import _root_.android.graphics.Typeface
import _root_.android.view.KeyEvent

import Constants._


class MainActivity extends Activity with TypedActivity {
  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    setContentView(R.layout.main)

    val fontawesome = Typeface.createFromAsset(getAssets(), "fontawesome.ttf")

    findView(TR.tuner_btn).setTypeface(fontawesome)
    findView(TR.tv_btn).setTypeface(fontawesome)
    findView(TR.pc_btn).setTypeface(fontawesome)
    findView(TR.pi_btn).setTypeface(fontawesome)


    findView(TR.tuner_btn).onClick = () => AVRemote.selectTuner()
    findView(TR.tv_btn).onClick = () => AVRemote.selectTV()
    findView(TR.pc_btn).onClick = () => AVRemote.selectPC()
    findView(TR.pi_btn).onClick = () => AVRemote.selectPI()
  }

  override def dispatchKeyEvent(event: KeyEvent): Boolean = {
    val action = event.getAction()
    val keyCode = event.getKeyCode()
    action match {
      case KeyEvent.ACTION_DOWN =>
        keyCode match {
          case KeyEvent.KEYCODE_VOLUME_UP =>
            AVRemote.volumeUp()
            true
          case KeyEvent.KEYCODE_VOLUME_DOWN =>
            AVRemote.volumeDown()
            true
          case _ => super.dispatchKeyEvent(event)
        }
      case _ => super.dispatchKeyEvent(event)
    }
  }
}
