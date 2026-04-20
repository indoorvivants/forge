inThisBuild(
  List(
    homepage := Some(url("https://github.com/indoorvivants/forge")),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        "indoorvivants",
        "Anton Sviridov",
        "contact@indoorvivants.com",
        url("https://blog.indoorvivants.com")
      )
    ),
    version := (if (!sys.env.contains("CI")) "dev" else version.value),
    crossScalaVersions := Nil
  )
)

organization := "com.indoorvivants"

lazy val publishing = Seq(
  organization := "com.indoorvivants"
)

lazy val noPublishing = Seq(
  publish / skip := true,
  publishLocal / skip := true
)

val V = new {
  val scala212 = "2.12.20"
  val scala3 = "3.8.3"

  val sbt1ScalaVersion = scala212
  val sbt2ScalaVersion = "3.8.3"
}

lazy val root =
  project
    .in(file("."))
    .aggregate(forgeViteWebappPlugin, exampleWebapp)
    .aggregate(forgeNativeBinary.projectRefs*)
    .aggregate(exampleNativeBinary)
    .settings(noPublishing)

lazy val forgeViteWebappPlugin = project
  .in(file("mod/forge-vite-webapp-plugin"))
  .enablePlugins(ScriptedPlugin, SbtPlugin)
  .settings(publishing)
  .settings(
    scalaVersion := V.scala212,
    name := "sbt-forge-vite-webapp",
    sbtPlugin := true,
    // set up 'scripted; sbt plugin for testing sbt plugins
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-Dplugin.version=" + version.value
    ),
    scriptedBufferLog := false,
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.21.0")
  )

lazy val forgeNativeBinary = projectMatrix
  .in(file("mod/forge-native-binary-plugin"))
  .enablePlugins(ScriptedPlugin, SbtPlugin)
  .settings(publishing)
  .jvmPlatform(Seq(V.sbt1ScalaVersion, V.sbt2ScalaVersion))
  .settings(
    (pluginCrossBuild / sbtVersion) := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.12.0"
        case _      => "2.0.0-RC11"
      }
    },
    scalacOptions ++= {
      scalaBinaryVersion.value match {
        case "2.12" => "-Xsource:3" :: Nil
        case _      => Nil
      }
    },
    sbtTestDirectory := {
      scalaBinaryVersion.value match {
        case "2.12" => (sourceDirectory).value / "sbt-test"
        case _      => (sourceDirectory).value / "sbt-test-sbt2"
      }
    },
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )
  .settings(
    name := "sbt-forge-native-binary",
    sbtPlugin := true,
    libraryDependencies += "com.indoorvivants.detective" %%% "platform" % "0.1.0",
    addSbtPlugin("org.scala-native" % "sbt-scala-native" % nativeVersion),
    addSbtPlugin("com.github.sbt" % "sbt2-compat" % "0.1.0")
  )

lazy val exampleWebapp =
  project
    .in(file("mod/example-webapp"))
    .enablePlugins(ForgeViteWebappPlugin)
    .settings(
      libraryDependencies += "com.raquo" %%% "laminar" % "17.2.1",
      frontendPackages := Seq("my.frontend"),
      scalaVersion := V.scala3,
      noPublishing
    )

lazy val exampleNativeBinary =
  project
    .in(file("mod/example-native-binary"))
    .enablePlugins(ForgeNativeBinaryPlugin)
    .settings(
      scalaVersion := V.scala3,
      buildBinaryConfig ~= { (_).withName("example-binary") },
      noPublishing
    )

val Commands = List(
  "exampleWebapp/frontendInit -f",
  "exampleWebapp/frontendBuild",
  "exampleNativeBinary/buildBinaryDebug",
  "exampleNativeBinary/buildBinaryPlatformRelease"
).mkString(";")

addCommandAlias("ci", Commands)
