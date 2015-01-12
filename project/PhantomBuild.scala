import sbt._
import sbt.Keys._
import sbtassembly.Plugin._
import AssemblyKeys._

object AnalyticsServer extends Build {

  val ScalaVersion = "2.10.4"
  val ScalaTestVersion = "2.1.0"
  val datastaxDriverVersion = "2.1.1"
  val finagleVersion = "6.17.0"

  val publishSettings: Seq[Def.Setting[_]] = Seq(
    publishMavenStyle := true,
    publishTo <<= version.apply {
      v =>
        val nexus = "http://artifactory.sphoniclabs.net:8081/"
        if (v.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at nexus + "artifactory/sphonic-snapshot-local/")
        else
          Some("releases" at nexus + "artifactory/sphonic-releases-local/")
    },
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => true },
    if (System.getenv().containsKey("TRAVIS_JOB_ID")) {
      val pass: String = System.getenv().get("TRAVIS_SBT_CREDENTIALS")
      println("Using encrypted Travis credentials.")
      credentials += Credentials(
        "Artifactory Realm",
        "artifactory.sphoniclabs.net",
        "buildbot",
        pass
      )
    } else {
      println("Using credentials from /.ivy2/.sphonic_credentials")
      credentials += Credentials(Path.userHome / ".ivy2" / ".sphonic_credentials")
    }
  )
  
  val noPublish: Seq[Def.Setting[_]] = Seq(
    publish := (),
    publishLocal := (),
    publishTo := None
  )

  val sharedSettings: Seq[Def.Setting[_]] = Seq(
    organization := "com.sphonic",
    version := "0.1.3",
    scalaVersion := ScalaVersion,
    resolvers ++= Seq(
      "Typesafe repository"              at "http://repo.typesafe.com/typesafe/releases/",
      "Sonatype Scala-Tools"             at "https://oss.sonatype.org/content/groups/scala-tools/",
      "Sonatype"                         at "https://oss.sonatype.org/content/repositories/releases",
      "Java.net Maven2 Repository"       at "http://download.java.net/maven/2/",
      "Twitter Repository"               at "http://maven.twttr.com",
      "Websudos releases"                at "http://maven.websudos.co.uk/ext-release-local",
      "Sphonic snapshots"                at "http://artifactory.sphoniclabs.net:8081/artifactory/sphonic-snapshot-local/",
      "Sphonic releases"                 at "http://artifactory.sphoniclabs.net:8081/artifactory/sphonic-releases-local/",
      "jgit-repo"                        at "http://download.eclipse.org/jgit/maven"
    ),
    unmanagedSourceDirectories in Compile <<= (scalaSource in Compile)(Seq(_)),
    scalacOptions ++= Seq(
      "-language:postfixOps",
      "-language:implicitConversions",
      "-language:reflectiveCalls",
      "-language:higherKinds",
      "-language:existentials",
      "-Yinline-warnings",
      "-Xlint",
      "-deprecation",
      "-feature",
      "-unchecked"
    ),
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest"   % ScalaTestVersion % "test"
    )
  ) ++ publishSettings ++ net.virtualvoid.sbt.graph.Plugin.graphSettings

  lazy val root = Project(
		id = "root",
		base = file("."),
		settings = Defaults.coreDefaultSettings ++ noPublish
  ).settings(
    name := "phantom-root"
	).aggregate(
		connector,
		zookeeper,
		sbt
	)

  lazy val connector = Project(
    id = "connector",
    base = file("connector"),
    settings = Defaults.coreDefaultSettings ++ sharedSettings
  ).settings(
    name := "phantom-connector",
    libraryDependencies ++= Seq(
      "com.datastax.cassandra"  %  "cassandra-driver-core"  % datastaxDriverVersion,
      "com.twitter"             %% "util-core"              % "6.20.0"
    )
  )
    
  lazy val zookeeper = Project(
    id = "zookeeper",
    base = file("zookeeper"),
    settings = Defaults.coreDefaultSettings ++ sharedSettings
  ).settings(
    name := "phantom-zookeeper",
    libraryDependencies ++= Seq(
      "com.twitter"             %% "finagle-serversets"     % finagleVersion exclude("org.slf4j", "slf4j-jdk14"),
      "com.twitter"             %% "finagle-zookeeper"      % finagleVersion
    )
  ).dependsOn(
    connector
  )
  
  lazy val sbt = Project(
    id = "sbt",
    base = file("sbt"),
    settings = Defaults.coreDefaultSettings ++ sharedSettings
  ).settings(
    name := "phantom-sbt",
    sbtPlugin := true,
    libraryDependencies ++= Seq(
      "org.cassandraunit"      % "cassandra-unit"  % "2.0.2.2"  excludeAll (
	    ExclusionRule("org.slf4j", "slf4j-log4j12"),
	    ExclusionRule("org.slf4j", "slf4j-jdk14")
	  )
	)
  )

}
