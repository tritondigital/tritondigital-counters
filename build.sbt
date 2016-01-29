name := "tritondigital-counters"

version := "1.0.2-SNAPSHOT"

organization := "com.tritondigital"

organizationHomepage := Some(new URL("http://www.tritondigital.com"))

description := "A library for easy application monitoring in Java and Scala"

homepage := Some(new URL("https://github.com/tritondigital/tritondigital-counters"))

startYear := Some(2015)

licenses := Seq("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))

crossScalaVersions := Seq("2.10.5", "2.11.6")

scalacOptions ++= Seq("-target:jvm-1.7", "-deprecation", "-feature")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.4",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.4",
  "com.codahale.metrics" % "metrics-core" % "3.0.2",
  "ch.qos.logback" % "logback-classic" % "1.0.12" % "provided",

  "com.typesafe.akka" %% "akka-testkit" % "2.3.4" % "test",
  "org.scalatest"  %% "scalatest" % "2.2.2" % "test",
  "org.scalacheck" %% "scalacheck" % "1.11.6" % "test"
)

publishMavenStyle := true

sonatypeProfileName := "com.tritondigital"

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

ScoverageSbtPlugin.ScoverageKeys.coverageMinimum := 85

ScoverageSbtPlugin.ScoverageKeys.coverageFailOnMinimum := true

publishArtifact in Test := false

credentials += Credentials("Nexus Repository Manager","oss.sonatype.org",System.getenv("SONATYPE_USERNAME"),System.getenv("SONATYPE_PASSWORD"))

pomIncludeRepository := { _ => false }

pomExtra :=
  <scm>
    <url>git@github.com:tritondigital/tritondigital-counters.git</url>
    <connection>scm:git:git@github.com:tritondigital/tritondigital-counters.git</connection>
  </scm>
  <developers>
    <developer>
      <id>jletroui</id>
      <name>Julien Letrouit</name>
      <url>http://julienletrouit.com</url>
    </developer>
  </developers>
