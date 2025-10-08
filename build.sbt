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

organization := "com.indoorvivants.forge"
sonatypeProfileName := "com.indoorvivants"

lazy val publishing = Seq(
  organization := "com.indoorvivants.forge",
  sonatypeProfileName := "com.indoorvivants"
)

val V = new {
  val scala212 = "2.12.20"
  val scala3 = "3.7.3"
}

lazy val root =
  project.in(file(".")).aggregate(forgeViteWebappPlugin, exampleWebapp)

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
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.20.1")
  )

lazy val forgeNativeBinary = project
  .in(file("mod/forge-native-binary"))
  .enablePlugins(ScriptedPlugin, SbtPlugin)
  .settings(publishing)
  .settings(
    scalaVersion := V.scala212,
    name := "sbt-forge-native-binary",
    sbtPlugin := true,
    // set up 'scripted; sbt plugin for testing sbt plugins
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-Dplugin.version=" + version.value
    ),
    scriptedBufferLog := false,
    libraryDependencies += "com.indoorvivants.detective" %%% "platform" % "0.1.0",
    addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.5.8")
  )

lazy val exampleWebapp =
  project
    .in(file("mod/example-webapp"))
    .enablePlugins(ForgeViteWebappPlugin)
    .settings(
      libraryDependencies += "com.raquo" %%% "laminar" % "17.2.1",
      frontendPackages := Seq("my.frontend"),
      scalaVersion := V.scala3
    )

lazy val exampleNativeBinary =
  project
    .in(file("mod/example-native-binary"))
    .enablePlugins(ForgeNativeBinaryPlugin)
    .settings(
      scalaVersion := V.scala3,
      buildBinaryConfig ~= { (_).withName("example-binary") }
    )
