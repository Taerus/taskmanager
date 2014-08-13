import sbt._
import sbt.Keys._


object TaskmanagerBuild extends Build {
  import BuildSettings._
  import Dependencies._

  lazy val root: Project = Project(
    libName,
    file("."),
    settings = buildSettings ++ Seq(
      run <<= run in Compile in samples,
      publish:= (),
      publishLocal := ()
    )
  ) aggregate(macros, core, spring, swing, samples)

  lazy val macros: Project = Project(
    macrosName,
    file(macrosName),
    settings = buildSettings ++ Seq(
      name := macrosName,
      libraryDependencies ++= macrosDependencies,
      addCompilerPlugin("org.scalamacros" % ("paradise_"+Versions.scala) % Versions.paradise)
    )
  )

  lazy val core: Project = Project(
    coreName,
    file(coreName),
    settings = buildSettings ++ Seq(
      name := coreName,
      libraryDependencies ++= coreDependencies
    )
  )
  
  lazy val spring: Project = Project(
    springName,
    file(springName),
    settings = buildSettings ++ Seq(
      name := springName,
      libraryDependencies ++= springDependencies
    )
  ) dependsOn core
  
  lazy val swing: Project = Project(
    swingName,
    file(swingName),
    settings = buildSettings ++ Seq(
      name := swingName,
      libraryDependencies ++= swingDependencies
    )
  ) dependsOn core
  
  lazy val samples: Project = Project(
    samplesName,
    file(samplesName),
    settings = buildSettings ++ Seq(
      name := samplesName,
      libraryDependencies ++= samplesDependencies,
      addCompilerPlugin("org.scalamacros" % ("paradise_"+Versions.scala) % Versions.paradise),
      publish:= (),
      publishLocal := ()
    )
  ) dependsOn(macros, core, swing, spring)
  
}