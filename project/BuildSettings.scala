import sbt._
import sbt.Keys._


object BuildSettings {

  val libName     = "taskmanager"
  val macrosName  = libName + "-macros"
  val coreName    = libName + "-core"
  val springName  = libName + "-spring"
  val swingName   = libName + "-swing"
  val samplesName = libName + "-samples"

  object Versions {
    val akka     = "2.3.3"
    val paradise = "2.0.0"
    val scala    = "2.10.4"
  }
  
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "akka.duke",
    version := "1.0.1-SNAPSHOT",
    scalacOptions ++= Seq(),
    scalaVersion := Versions.scala,
    resolvers ++= Seq(
      Resolver.sonatypeRepo("snapshots"),
      Resolver.sonatypeRepo("releases"),
      Resolver.typesafeRepo("releases"),
      "SpringSource Milestone Repository" at "https://repo.springsource.org/libs-milestone"
    ),
    publishTo := Some(Resolver.file("file",  new File("../taskmanager-pages/repository")))
  )
  
}