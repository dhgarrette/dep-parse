import sbt._
import Keys._

object DepparseBuild extends Build {

  lazy val main = Project("depparse", file(".")) dependsOn(scalautil)

  lazy val scalautil = Project("scala-util", file("scala-util"))

}
