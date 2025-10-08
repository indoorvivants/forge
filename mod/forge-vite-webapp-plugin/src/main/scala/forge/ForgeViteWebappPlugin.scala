package forge

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

import java.nio.file.Files
import java.util.Arrays
import java.util.stream.Collectors
import scala.sys.process
import sjsonnew.JsonFormat
import scala.util.control.NonFatal
import scala.util.Try
import sjsonnew.support.scalajson.unsafe.Converter
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.linker.interface.ModuleSplitStyle
import java.nio.file.Paths
import scala.collection.SortedMap

object ForgeViteWebappPlugin extends AutoPlugin {

  object autoImport {
    val frontendPackages = settingKey[Seq[String]]("Frontend packages")
    val frontendProjectName = settingKey[String]("")
    val frontendProjectRef = taskKey[String]("")
    val frontendInit = inputKey[Unit]("Initialize a minimal Vite project")
    val frontendBuildLocation =
      settingKey[File]("Location where to put build frontend")
    val frontendBuild =
      inputKey[Unit]("Build the frontend with full optimisations")
  }

  override def requires: Plugins = ScalaJSPlugin

  import autoImport._
  import ScalaJSPlugin.autoImport.*

  override lazy val projectSettings = Seq(
    scalaJSUseMainModuleInitializer := true,
    frontendPackages := Seq.empty,
    frontendProjectName := name.value,
    frontendProjectRef := state.value.currentProject.id,
    fastLinkJS / scalaJSLinkerConfig := {
      var conf = (fastLinkJS / scalaJSLinkerConfig).value

      conf = conf
        .withModuleKind(ModuleKind.ESModule)

      if (frontendPackages.value.nonEmpty) {
        conf = conf.withModuleSplitStyle(
          ModuleSplitStyle.SmallModulesFor(frontendPackages.value.toList)
        )
      }

      conf
    },
    frontendInit := {
      import complete.DefaultParsers._
      val args: Seq[String] = spaceDelimited("<arg>").parsed
      val force = args.contains("-f")
      val buildRoot = (ThisBuild / baseDirectory).value.toPath
      val projectRoot = {
        val raw = baseDirectory.value.toPath
        if (raw.startsWith(buildRoot.resolve(".sbt/matrix")))
          sourceDirectory.value.toPath.getParent()
        else raw
      }
      val relativePath = projectRoot.relativize(buildRoot)
      val projectRef = frontendProjectRef.value
      val projectName = frontendProjectName.value

      val contents = SortedMap(
        "package.json" ->
          s"""
        |{
        |  "name": "$projectName",
        |  "version": "0.1.0",
        |  "scripts": {
        |    "dev": "vite",
        |    "build": "vite build",
        |    "serve": "vite preview"
        |  },
        |  "type": "module",
        |  "devDependencies": {
        |    "@scala-js/vite-plugin-scalajs": "^1.0.0",
        |    "vite": "^7.1.5"
        |  }
        |}
        """.trim.stripMargin,
        "vite.config.js" ->
          s"""
          |import { defineConfig } from "vite";
          |import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";
          |
          |export default defineConfig({
          |  plugins: [
          |    scalaJSPlugin({
          |      // path to the directory containing the sbt build
          |      // default: '.'
          |      cwd: '$relativePath',
          |
          |      // sbt project ID from within the sbt build to get fast/fullLinkJS from
          |      // default: the root project of the sbt build
          |      projectID: '${projectRef}',
          |
          |      // URI prefix of imports that this plugin catches (without the trailing ':')
          |      // default: 'scalajs' (so the plugin recognizes URIs starting with 'scalajs:')
          |      uriPrefix: 'scalajs',
          |    }),
          |  ],
          |});
          """.trim.stripMargin,
        "main.js" ->
          """
          |import './style.css'
          |import 'scalajs:main.js'
          """.trim.stripMargin,
        "index.html" ->
          s"""
          |<!DOCTYPE html>
          |<html lang="en">
          |<head>
          |  <meta charset="UTF-8">
          |  <meta name="viewport" content="width=device-width, initial-scale=1.0">
          |  <title>$projectName</title>
          |</head>
          |<body>
          |  <div id="root"></div>
          |  <script type="module" src="/main.js"></script>
          |</body>
          |</html>
          """.trim.stripMargin,
        "style.css" -> "body {background-color: #f0f0f0;}",
        ".gitignore" -> List("node_modules", "dist", "target").mkString("\n")
      )

      contents.foreach { case (relativePath, contents) =>
        val destination = projectRoot.resolve(relativePath)
        if (Files.exists(destination) && !force) {
          sys.error(
            s"File ${destination} already exists, pass -f to the task invocation to overwrite"
          )
        }

        Files.writeString(destination, contents)
      }
    },
    frontendBuild := {
      val projectRoot = {
        val raw = baseDirectory.value.toPath
        if (
          raw.startsWith(
            (ThisBuild / baseDirectory).value.toPath.resolve(".sbt/matrix")
          )
        )
          sourceDirectory.value.toPath.getParent()
        else raw
      }

      import scala.sys.process.*

      assert(
        Process("npm install", cwd = projectRoot.toFile).! == 0,
        "Command [npm install] did not finish successfully"
      )

      assert(
        Process("npm run build", cwd = projectRoot.toFile).! == 0,
        "Command [npm run build] did not finish successfully"
      )

      projectRoot / "dist"

    }
  )

  override lazy val buildSettings = Seq()

  override lazy val globalSettings = Seq()

}
