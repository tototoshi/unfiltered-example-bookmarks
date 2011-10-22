import sbt._
import Keys._

object BuildSettings {
  val buildName = "unfiltered-example-bookmarks"
  val buildOrganization = "odp"
  val buildVersion      = "2.0.29"
  val buildScalaVersion = "2.9.1"

  val buildSettings = Defaults.defaultSettings ++ Seq (
    name         := buildName,
    organization := buildOrganization,
    version      := buildVersion,
    scalaVersion := buildScalaVersion
  )
}

object Dependencies {

  object Unfiltered {
    val version = "0.5.1"
    val filter = "net.databinder" %% "unfiltered-filter" % version
    val jetty = "net.databinder" %% "unfiltered-jetty" % version
    val json = "net.databinder" %% "unfiltered-json" % version
    val scalate = "net.databinder" %% "unfiltered-scalate" % version
    lazy val spec = "net.databinder" %% "unfiltered-spec" % version //% "test"
  }

  val scalaSTM = "org.scala-tools" %% "scala-stm" % "0.3"

  object Logging {
    val avsl = "org.clapper" %% "avsl" % "0.3.6"
  }
}

object Resolvers {
  val newReleaseToolsRepository = ScalaToolsSnapshots
  val jboss = "JBoss repository" at "https://repository.jboss.org/nexus/content/groups/public/"
  val javaNetRepo = "Java.net Repository for Maven" at "http://download.java.net/maven/2"

  val all = Seq(newReleaseToolsRepository, jboss, javaNetRepo)
}

object UnfilteredExampleBookmarks extends Build {

  import BuildSettings._
  import Dependencies._

  lazy val root = Project (
    id = "unfiltered-example-bookmarks",
    base = file ("."),
    settings = buildSettings ++ Seq (
      resolvers ++= Resolvers.all,
      libraryDependencies ++= Seq(
        Unfiltered.filter,
        Unfiltered.jetty,
        Unfiltered.json,
        Unfiltered.scalate,
        Unfiltered.spec,
        scalaSTM,
        Logging.avsl
      )
    )
  )
}


