package org.furidamu.omnimote

import android.widget.SeekBar
import android.view.View
import android.widget.TextView
import android.widget.ListView
import android.widget.AdapterView
import android.app.ProgressDialog
import android.content.DialogInterface

import net.minidev.json.JSONValue
import net.minidev.json.JSONArray
import net.minidev.json.JSONObject

import _root_.android.util.Log

import scala.collection.JavaConversions._

object Constants {
  val PREFERENCES_NAME = "wakeontelnet"

  implicit def ViewToRichView(v: View) = new RichView(v)
  implicit def TextViewToRichTextView(v: TextView) = new RichTextview(v)
  implicit def ListViewToRichListView(v: ListView) = new RichListView(v)

  def log(str: String) {
    str match {
      case null => Log.d("oauth", "null")
      case s => Log.d("oauth", s)
    }
  }

  def runInBackground(f: => Unit) {
    new java.lang.Thread {
      override def run() {
        try { f }
        catch {
          case e =>
            log(Log.getStackTraceString(e))
        }
      }
    }.start()
  }

  def parseJSON(s: String) = new ScalaJSON(JSONValue.parse(s))
  def makeJSON(a: Any): String = a match {
    case m: Map[String, Any] => m.map {
      case (name, content) => "\"" + name + "\":" + makeJSON(content)
    }.mkString("{", ",", "}")
    case l: List[Any] => l.map(makeJSON).mkString("[", ",", "]")
    case l: java.util.List[Any] => l.map(makeJSON).mkString("[", ",", "]")
    case s: String => "\"" + s + "\""
    case i: Int => i.toString
  }

  def showProgressDialog(title: String, text: String, f: () => Unit = () => {})(implicit ctx: android.content.Context) = {
    ProgressDialog.show(ctx, title, text, true, true, new DialogInterface.OnCancelListener() {
      def onCancel(dialog: DialogInterface) {
        f
      }
    } )
  }

}

import Constants._


class RichTextview(tv: TextView) {
  def text = tv.getText()
  def text_= (t: String) = tv.setText(t)
}

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
  def findView[T](tr: TypedResource[T]) = view.findViewById(tr.id).asInstanceOf[T]
}


class RichListView(view: ListView) {
  def onItemClick = throw new Exception
   def onItemClick_= (f: (Int, Long) => Unit) {
     view.setOnItemClickListener(
     new AdapterView.OnItemClickListener() {
      def onItemClick(parent: AdapterView[_], v: View, position: Int, id: Long) {
        f(position, id)
      }
     } )
   }
}

case class JSONException extends Exception

class ScalaJSONIterator(i: java.util.Iterator[java.lang.Object]) extends Iterator[ScalaJSON] {
  def hasNext = i.hasNext()
  def next() = new ScalaJSON(i.next())
}

class ScalaJSON(o: java.lang.Object) extends Seq[ScalaJSON] {
  override def toString: String = o.toString
  def toInt: Int = o match {
    case i: Integer => i
    case _ => throw new JSONException
  }
  def toDouble: Double = o match {
    case d: java.lang.Double => d
    case f: java.lang.Float => f.toDouble
    case _ => throw new JSONException
  }
  def apply(key: String): ScalaJSON = o match {
    case m: JSONObject => new ScalaJSON(m.get(key))
    case _ => throw new JSONException
  }

  def apply(idx: Int): ScalaJSON = o match {
    case a: JSONArray => new ScalaJSON(a.get(idx))
    case _ => throw new JSONException
  }
  def length: Int = o match {
    case a: JSONArray => a.size()
    case m: JSONObject => m.size()
    case _ => throw new JSONException
  }
  def iterator: Iterator[ScalaJSON] = o match {
    case a: JSONArray => new ScalaJSONIterator(a.iterator())
    case _ => throw new JSONException
  }
}
