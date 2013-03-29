import sbt._

import Keys._
import AndroidKeys._

object General {
  val settings = Defaults.defaultSettings ++ Seq (
    name := "Omnimote",
    version := "0.4",
    versionCode := 4,
    scalaVersion := "2.10.0",
    platformName in Android := "android-17"
  )

  val proguardSettings = Seq (
    useProguard in Android := false,
    skipScalaLibrary in Android := true
  )

  lazy val fullAndroidSettings =
    General.settings ++
    AndroidProject.androidSettings ++
    TypedResources.settings ++
    proguardSettings ++
    AndroidManifestGenerator.settings ++
    AndroidMarketPublish.settings ++ Seq (
      keyalias in Android := "alias_name",
      resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.1" % "test",
      libraryDependencies += "commons-net" % "commons-net" % "2.0",
      libraryDependencies += "org.scala-lang" % "scala-actors" % "2.10.0",
      proguardOptimizations in Android += "-keep class scala.collection.SeqLike { public protected *; }"
    )
}

object AndroidBuild extends Build {
  lazy val main = Project (
    "Omnimote",
    file("."),
    settings = General.fullAndroidSettings
  )

  lazy val tests = Project (
    "tests",
    file("tests"),
    settings = General.settings ++
               AndroidTest.androidSettings ++
               General.proguardSettings ++ Seq (
      name := "OmnimoteTests"
    )
  ) dependsOn main
}
