package forge.nativebinary

import sbt.Keys._
import sbt.*
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
import com.indoorvivants.detective.Platform, Platform.*
import sbtcompat.PluginCompat._
import xsbti.FileConverter
import sjsonnew.IsoFormats

object ForgeNativeBinaryPlugin extends AutoPlugin {

  object autoImport {
    type BinConfig = forge.nativebinary.BinConfig
    val BinConfig = forge.nativebinary.BinConfig

    type BuildResult = forge.nativebinary.BuildResult
    val BuildResult = forge.nativebinary.BuildResult

    val buildBinaryConfig = settingKey[BinConfig]("")
    @transient
    val buildBinaryDebug = taskKey[BuildResult]("")
    @transient
    val buildBinaryRelease = taskKey[BuildResult]("")
    @transient
    val buildBinaryPlatformDebug = taskKey[BuildResult]("")
    @transient
    val buildBinaryPlatformRelease = taskKey[BuildResult]("")
  }

  override def requires: Plugins = ScalaNativePlugin

  import autoImport.*

  import sjsonnew._, LList.:*:

  import BasicJsonProtocol._

  // implicit object BuildResultJsonFormat extends JsonFormat[BuildResult] {
  //   def write[J](x: BuildResult, builder: Builder[J]): Unit = {
  //     builder.beginObject()
  //     builder.addField("main", x.file)
  //     builder.addField("rest", x.copies)
  //     builder.endObject()
  //   }
  //   def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): BuildResult = {
  //     jsOpt match {
  //       case Some(js) =>
  //         val main = unbuilder.readField[FileRef]("main")
  //         val copies = unbuilder.readField[Seq[FileRef]]("rest")

  //         BuildResult(main, copies)
  //       case None => ???
  //     }
  //   }
  // }

  private def writeBinary(
      source: FileRef,
      destinationDir: java.nio.file.Path,
      extraDestinationDirs: Seq[java.nio.file.Path],
      log: sbt.Logger,
      platform: Option[Platform.Target],
      debug: Boolean,
      name: String
  )(implicit fileConverter: FileConverter): BuildResult = {

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

    val built = List.newBuilder[FileRef]

    (destinationDir +: extraDestinationDirs).foreach { dir =>
      val seg = if (debug) "debug" else "release"
      val dest = (dir / seg / fullName).toFile

      built += toFileRef(dest)

      Files.createDirectories(dest.getParentFile().toPath())

      Files.copy(
        toFile(source).toPath(),
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
      destinationDir = ((ThisBuild / baseDirectory).value / "out").toPath
    ),
    buildBinaryDebug := {
      implicit val conv: xsbti.FileConverter = fileConverter.value
      writeBinary(
        source = (ThisProject / Compile / (SN.nativeLink)).value,
        destinationDir = buildBinaryConfig.value.destinationDir,
        extraDestinationDirs = buildBinaryConfig.value.extraDestinationDirs,
        log = sLog.value,
        platform = None,
        debug = true,
        name = (buildBinaryConfig.value.name)
      )
    },
    buildBinaryRelease :=
      {
        implicit val conv: xsbti.FileConverter = fileConverter.value
        writeBinary(
          source = (ThisProject / Compile / (SN.nativeLinkReleaseFast)).value,
          destinationDir = buildBinaryConfig.value.destinationDir,
          extraDestinationDirs = buildBinaryConfig.value.extraDestinationDirs,
          log = sLog.value,
          platform = None,
          debug = false,
          name = (buildBinaryConfig.value.name)
        )
      },
    buildBinaryPlatformDebug :=
      {
        implicit val conv: xsbti.FileConverter = fileConverter.value
        writeBinary(
          source = (ThisProject / Compile / (SN.nativeLink)).value,
          destinationDir = buildBinaryConfig.value.destinationDir,
          extraDestinationDirs = buildBinaryConfig.value.extraDestinationDirs,
          log = sLog.value,
          platform = Some(Platform.target),
          debug = true,
          name = (buildBinaryConfig.value.name)
        )
      },
    buildBinaryPlatformRelease :=
      {
        implicit val conv: xsbti.FileConverter = fileConverter.value
        writeBinary(
          source = (ThisProject / Compile / (SN.nativeLinkReleaseFast)).value,
          destinationDir = buildBinaryConfig.value.destinationDir,
          extraDestinationDirs = buildBinaryConfig.value.extraDestinationDirs,
          log = sLog.value,
          platform = Some(Platform.target),
          debug = false,
          name = (buildBinaryConfig.value.name)
        )
      }
  )

}
