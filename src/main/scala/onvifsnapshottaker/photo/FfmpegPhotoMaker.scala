package onvifsnapshottaker.photo

import java.nio.file.{Files, Paths}
import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.LazyLogging
import onvifsnapshottaker.Config

import scala.util.Try
import scala.concurrent.duration._
import scala.language.postfixOps

object FfmpegPhotoMaker extends PhotoMaker with LazyLogging {

  private val config = Config().getConfig("ffmpeg-photo-maker")
  private val script = Paths.get(config.getString("script"))
  private val image = Paths.get(config.getString("image"))
  private val log = Paths.get(config.getString("log"))

  logger.info(s"script ${script} image ${image} log ${log}")

  private val timeout = 30 seconds

  override def shot(): Try[Array[Byte]] = {
    Try {
      val process = Runtime.getRuntime.exec(s"${script.toString} | tee ${log.toAbsolutePath.toString}")
      if(!process.waitFor(timeout.toMillis, TimeUnit.MILLISECONDS)){
        throw new RuntimeException(s"timeout after ${timeout.toMillis} millis")
      }
      val bytes = Files.readAllBytes(image)
      Files.delete(image)
      process.destroy()
      bytes
    }
  }

}
