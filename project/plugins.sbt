resolvers ++= Seq(
  "Sonatype releases" at "http://oss.sonatype.org/content/repositories/releases/",
  "jgit-repo"         at "http://download.eclipse.org/jgit/maven"
)

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.6.4")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.1")

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")

