val scala3Version = "3.2.0"

// scalacOptions ++= Seq("-explain")

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
    "dev.zio" %% "zio-test" % "2.0.2" % Test,
    "dev.zio" %% "zio-test-sbt" % "2.0.2" % Test,
    // "dev.zio" %% "zio-test-magnolia" % "2.0.2" % Test
    "org.scalameta" %% "munit" % "0.7.29" % Test
  ),
  testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  // testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
)
