package forge.nativebinary

import java.io.File

class BinConfig(private val params: BinConfig.Params) {
  // getters
  def name: String = params.name
  def destinationDir: File = params.destinationDir
  def extraDestinationDirs: Seq[File] = params.extraDestinationDirs

  // setters
  def withName(n: String): BinConfig = copy(_.copy(name = n))
  def withDestinationDir(dir: File) = copy(_.copy(destinationDir = dir))
  def addDestinationDir(st: File) =
    copy(s => s.copy(extraDestinationDirs = s.extraDestinationDirs :+ st))

  private def copy(f: BinConfig.Params => BinConfig.Params): BinConfig =
    new BinConfig(f(params))
}
object BinConfig {
  private case class Params(
      name: String,
      destinationDir: File,
      extraDestinationDirs: Seq[File] = Seq.empty
  )

  def default(name: String, destinationDir: File) = new BinConfig(
    Params(name = name, destinationDir = destinationDir)
  )
}

case class BuildResult(file: File, copies: Seq[File]) {
  override def toString() =
    s"BuildResult[file=$file, copies=$copies]"
}
