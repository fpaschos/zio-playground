val scala3Version = "3.2.0"

// scalacOptions ++= Seq("-explain")

lazy val commonSettings = Seq(
  organization := "dev.fpas.zio2",
  scalaVersion := "3.2.0"
)

lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .settings(name := "zio-playground", publishArtifact := false)
  .settings(dependencies)
  .aggregate(`zio2-experiments`)

lazy val `zio2-experiments` = project
  .in(file("./zio2-experiments"))
  .settings(commonSettings)
  .settings(name := "zio2-experiments")
  .settings(dependencies)

lazy val dependencies = Seq(
  libraryDependencies ++= Seq(
    "dev.zio" %% "zio" % "2.0.2",
    "dev.zio" %% "zio-streams" % "2.0.2",
    "dev.zio" %% "zio-test" % "2.0.2" % Test,
    "dev.zio" %% "zio-test-sbt" % "2.0.2" % Test,
    // "dev.zio" %% "zio-test-magnolia" % "2.0.2" % Test
    "org.scalameta" %% "munit" % "0.7.29" % Test
  ),
  testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
)
