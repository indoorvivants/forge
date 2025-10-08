addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.2")
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.11.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.3.8")

// Scala.js and Scala Native
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.20.1")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.5.8")

libraryDependencies ++= List(
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
)
Compile / unmanagedSourceDirectories +=
  (ThisBuild / baseDirectory).value.getParentFile /
    "mod" / "forge-vite-webapp-plugin" / "src" / "main" / "scala"

Compile / unmanagedSourceDirectories +=
  (ThisBuild / baseDirectory).value.getParentFile /
    "mod" / "forge-native-binary-plugin" / "src" / "main" / "scala"

libraryDependencies += "com.indoorvivants.detective" %% "platform" % "0.1.0"

// Compile / unmanagedSourceDirectories +=
//   (ThisBuild / baseDirectory).value.getParentFile /
//     "mod" / "snapshots-buildtime" / "src" / "main" / "scala"

// Compile / unmanagedResourceDirectories +=
//   (ThisBuild / baseDirectory).value.getParentFile /
//     "mod" / "snapshots-buildtime" / "src" / "main" / "resources"

// Compile / sourceGenerators += Def.task {
//   val tmpDest =
//     (Compile / managedResourceDirectories).value.head / "BuildInfo.scala"

//   IO.write(
//     tmpDest,
//     "package com.indoorvivants.snapshots.sbtplugin\nobject BuildInfo {def version: String = \"dev\"}"
//   )

//   Seq(tmpDest)
// }
