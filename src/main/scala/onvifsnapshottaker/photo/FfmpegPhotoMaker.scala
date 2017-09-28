package onvifsnapshottaker.photo

import java.nio.file.{Files, Paths}

import onvifsnapshottaker.Config

import scala.util.Try

object FfmpegPhotoMaker extends PhotoMaker {

  private val config = Config().getConfig("ffmpeg-photo-maker")
  private val script = Paths.get(config.getString("script"))
  private val image = Paths.get(config.getString("image"))
  private val log = Paths.get(config.getString("log"))

  override def shot(): Try[Array[Byte]] = {
    Try {
      val process = Runtime.getRuntime.exec(s"${script.toString} | tee ${log.toAbsolutePath.toString}")
      process.waitFor()
      val bytes = Files.readAllBytes(image)
      Files.delete(image)
      process.destroy()
      bytes
    }
  }

}
