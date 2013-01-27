package org.furidamu.omnimote

import _root_.android.app.Activity
import _root_.android.os.Bundle
import _root_.android.graphics.Typeface

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


    findView(TR.tuner_btn).onClick = () => Commander.selectTuner()
    findView(TR.tv_btn).onClick = () => Commander.selectTV()
    findView(TR.pc_btn).onClick = () => Commander.selectPC()
    findView(TR.pi_btn).onClick = () => Commander.selectPI()
  }
}
