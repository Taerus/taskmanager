import sbt._


object Dependencies {
  import BuildSettings._
  
  val akka     = "com.typesafe.akka"          %% "akka-actor"     % Versions.akka
  val swing    = "org.scala-lang"             %  "scala-swing"    % Versions.scala
  val paradise = "org.scalamacros"            %% "quasiquotes"    % Versions.paradise
  val spring   = "org.springframework.scala"  %% "spring-scala"   % "1.0.0.RC1"
  val logging  = Seq(
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
    "ch.qos.logback"             % "logback-classic"      % "1.1.2"
  )

  val coreDependencies    = Seq(akka) ++ logging
  val macrosDependencies  = Seq(paradise)
  val springDependencies  = ( coreDependencies ++ Seq(spring) ).distinct
  val swingDependencies   = ( coreDependencies ++ Seq(swing) ).distinct
  val samplesDependencies = (
    coreDependencies   ++
    macrosDependencies ++
    springDependencies ++
    swingDependencies
  ).distinct
  
}