package onvifsnapshottaker

import java.nio.file.{Files, Paths}

import scala.util.Try

object FfmpegPhotoMaker extends PhotoMaker {

  private val config = Config().getConfig("ffmpeg-photo-maker")
  private val script = Paths.get(config.getString("script"))
  private val image = Paths.get(config.getString("image"))

  override def shot(): Try[Array[Byte]] = {
    Try {
      val process = Runtime.getRuntime.exec(s"${script.toString} | tee ffmpeg.log")
      process.waitFor()
      val bytes = Files.readAllBytes(image)
      process.destroy()
      bytes
    }
  }

}
