name := "chat"

version := "0.1"

scalaVersion := "2.13.4"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.15"
libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.6.15"
libraryDependencies += "com.typesafe.akka" %% "akka-serialization-jackson" % "2.6.15"
libraryDependencies ++= Seq(
  "io.aeron" % "aeron-driver" % "1.32.0",
  "io.aeron" % "aeron-client" % "1.32.0"
)
libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "3.0.0"
libraryDependencies += "org.typelevel" %% "cats-core" % "2.3.0"

// for JDK 8
// https://stackoverflow.com/questions/54834125/sbt-assembly-deduplicate-module-info-class
assemblyMergeStrategy in assembly := {
  case "module-info.class" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
