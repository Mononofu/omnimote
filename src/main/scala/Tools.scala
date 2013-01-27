package org.furidamu.wakeontelnet

import android.widget.SeekBar
import android.view.View
import android.widget.TextView


object Constants {
	val TIME_DIALOG_ID = 0
	val ALARM_SLEEP = 0
	val ALARM_WAKE = 1
	val PREFERENCES_NAME = "wakeontelnet"

	implicit def ViewToRichView(v: View) = new RichView(v)
	implicit def TextViewToRichTextView(v: TextView) = new RichTextview(v)
	implicit def SeekBarToRichSeekBar(bar: SeekBar) = new RichSeekBar(bar)
}

import Constants._


class RichView(view: View) { 
	def onClick = throw new Exception
	 def onClick_= (f: () => Unit) { 
		 view.setOnClickListener(
		 new View.OnClickListener() {
		 	def onClick(v: View) {
		 		f()
		 	}
		 } ) 
	 }
}

class RichSeekBar(bar: SeekBar) {
	 def onSeekBarChange = throw new Exception
	 def onSeekBarChange_= (f: (Int, Boolean) => Unit) {
	 	bar.setOnSeekBarChangeListener(
		 	new SeekBar.OnSeekBarChangeListener() {
		 		def onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
		 			f(progress, fromUser)
		 		}

		    	def onStartTrackingTouch(seekBar: SeekBar) {}
		    	def onStopTrackingTouch(seekBar: SeekBar) {}
		 	})
	 }
}

class RichTextview(tv: TextView) {
	def text = tv.getText()
	def text_= (t: String) = tv.setText(t)
}