package onvifsnapshottaker

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}

object Config {

  val config: Config = ConfigFactory.load()

  val objectMapper: ObjectMapper = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper
  }

  val httpClient: CloseableHttpClient = HttpClients.createDefault()

  val photoMaker: PhotoMaker = {
    val str = config.getString("photoSaver")
    if (str.equalsIgnoreCase("ffmpeg")) {
      FfmpegPhotoMaker
    } else if (str.equalsIgnoreCase("uri")) {
      UriPhotoMaker
    } else throw new IllegalStateException(s"Unknown photo saver ${str}")

  }

  def apply(): Config = config
}
