ThisBuild / version := "0.1.0-SNAPSHOT"

val AkkaVersion = "2.6.17"
val AkkaManagementVersion = "1.0.9"

resolvers += ("custome1" at "http://4thline.org/m2").withAllowInsecureProtocol(true)

// Specify the Scala version
scalaVersion := "2.12.18"

// Specify the SBT version
sbtVersion := "1.9.7"

// Specify Java version
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

//dependencies for scalafx and scalaFXML:
// https://mvnrepository.com/artifact/org.scalafx/scalafx
libraryDependencies += "org.scalafx" %% "scalafx" % "8.0.192-R14"

// https://mvnrepository.com/artifact/org.scalafx/scalafxml-core-sfx8
libraryDependencies += "org.scalafx" %% "scalafxml-core-sfx8" % "0.5"

libraryDependencies += "com.lihaoyi" %% "upickle" % "3.0.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-remote" % AkkaVersion,
  "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
  "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding" % AkkaVersion,
  "com.typesafe" % "config" % "1.4.1",
  "org.fourthline.cling" % "cling-core" % "2.1.2",
  "org.fourthline.cling" % "cling-support" % "2.1.2",
  "com.lightbend.akka.management" %% "akka-management" % AkkaManagementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % AkkaManagementVersion,
)

lazy val root = (project in file("."))
  .settings(
    name := "What Do You Meme",
  )

//add extension to compiler to add more features
addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.1" cross CrossVersion.full)

fork:=true //can run 2 apps in the same time