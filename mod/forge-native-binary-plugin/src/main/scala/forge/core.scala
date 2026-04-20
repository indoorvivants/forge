package forge.nativebinary

import java.io.File
import sbtcompat.PluginCompat._
import java.nio.file.Path

class BinConfig private (private val params: BinConfig.Params) {
  // getters
  def name: String = params.name
  def destinationDir: Path = params.destinationDir
  def extraDestinationDirs: Seq[Path] = params.extraDestinationDirs

  // setters
  def withName(n: String): BinConfig = copy(_.copy(name = n))
  def withDestinationDir(dir: Path) = copy(_.copy(destinationDir = dir))
  def addDestinationDir(st: Path) =
    copy(s => s.copy(extraDestinationDirs = s.extraDestinationDirs :+ st))

  private def copy(f: BinConfig.Params => BinConfig.Params): BinConfig =
    new BinConfig(f(params))
}
object BinConfig {
  private case class Params(
      name: String,
      destinationDir: Path,
      extraDestinationDirs: Seq[Path] = Seq.empty
  )

  def default(name: String, destinationDir: Path) = new BinConfig(
    Params(name = name, destinationDir = destinationDir)
  )
}

case class BuildResult(file: FileRef, copies: Seq[FileRef]) {
  override def toString() =
    s"BuildResult[file=$file, copies=$copies]"
}
