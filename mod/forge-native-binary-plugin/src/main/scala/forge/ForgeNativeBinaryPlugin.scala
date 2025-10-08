package forge.nativebinary

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
import java.nio.file.Paths
import scala.collection.SortedMap
import scala.scalanative.sbtplugin.ScalaNativePlugin
import com.indoorvivants.detective.*, Platform.*

object ForgeNativeBinaryPlugin extends AutoPlugin {

  object autoImport {
    type BinConfig = forge.nativebinary.BinConfig
    val BinConfig = forge.nativebinary.BinConfig

    type BuildResult = forge.nativebinary.BuildResult
    val BuildResult = forge.nativebinary.BuildResult

    val buildBinaryConfig = settingKey[BinConfig]("")
    val buildBinaryDebug = taskKey[BuildResult]("")
    val buildBinaryRelease = taskKey[BuildResult]("")
    val buildBinaryPlatformDebug = taskKey[BuildResult]("")
    val buildBinaryPlatformRelease = taskKey[BuildResult]("")
  }

  override def requires: Plugins = ScalaNativePlugin

  import autoImport.*

  private def writeBinary(
      source: File,
      destinationDir: File,
      extraDestinationDirs: Seq[File],
      log: sbt.Logger,
      platform: Option[Platform.Target],
      debug: Boolean,
      name: String
  ): BuildResult = {

    import java.nio.file.*

    val fullName = platform match {
      case None         => name
      case Some(target) =>
        val ext = target.os match {
          case Platform.OS.Windows => ".exe"
          case _                   => ""
        }

        name + "-" + ArtifactNames.coursierString(target) + ext
    }

    import scala.sys.process.*

    val built = List.newBuilder[File]

    (destinationDir +: extraDestinationDirs).foreach { dir =>
      val seg = if (debug) "debug" else "release"
      val dest = dir / seg / fullName

      built += dest

      Files.createDirectories(dest.getParentFile().toPath())

      Files.copy(
        source.toPath(),
        dest.toPath(),
        StandardCopyOption.COPY_ATTRIBUTES,
        StandardCopyOption.REPLACE_EXISTING
      )

      if (debug && platform.exists(_.os == Platform.OS.MacOS))
        s"dsymutil $dest".!!

      log.info(s"Binary [$name] built in ${dest}")

    }

    val artifacts = built.result()

    BuildResult(artifacts.head, artifacts.tail)
  }

  val SN = ScalaNativePlugin.autoImport

  override lazy val projectSettings = Seq(
    buildBinaryConfig := BinConfig.default(
      name.value,
      destinationDir = (ThisBuild / baseDirectory).value / "out"
    ),
    buildBinaryDebug :=
      writeBinary(
        source = (ThisProject / Compile / (SN.nativeLink)).value,
        destinationDir = buildBinaryConfig.value.destinationDir,
        extraDestinationDirs = buildBinaryConfig.value.extraDestinationDirs,
        log = sLog.value,
        platform = None,
        debug = true,
        name = (buildBinaryConfig.value.name)
      ),
    buildBinaryRelease :=
      writeBinary(
        source = (ThisProject / Compile / (SN.nativeLinkReleaseFast)).value,
        destinationDir = buildBinaryConfig.value.destinationDir,
        extraDestinationDirs = buildBinaryConfig.value.extraDestinationDirs,
        log = sLog.value,
        platform = None,
        debug = false,
        name = (buildBinaryConfig.value.name)
      ),
    buildBinaryPlatformDebug :=
      writeBinary(
        source = (ThisProject / Compile / (SN.nativeLink)).value,
        destinationDir = buildBinaryConfig.value.destinationDir,
        extraDestinationDirs = buildBinaryConfig.value.extraDestinationDirs,
        log = sLog.value,
        platform = Some(Platform.target),
        debug = true,
        name = (buildBinaryConfig.value.name)
      ),
    buildBinaryPlatformRelease :=
      writeBinary(
        source = (ThisProject / Compile / (SN.nativeLinkReleaseFast)).value,
        destinationDir = buildBinaryConfig.value.destinationDir,
        extraDestinationDirs = buildBinaryConfig.value.extraDestinationDirs,
        log = sLog.value,
        platform = Some(Platform.target),
        debug = false,
        name = (buildBinaryConfig.value.name)
      )
  )

}
