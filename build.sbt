val scala3Version = "3.2.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "zio-playground",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version
  )
  .settings(dependencies)

lazy val dependencies = Seq(
  libraryDependencies ++= Seq(
    "dev.zio" %% "zio" % "2.0.2",
    "dev.zio" %% "zio-streams" % "2.0.2",
    "org.scalameta" %% "munit" % "0.7.29" % Test
  )
)
