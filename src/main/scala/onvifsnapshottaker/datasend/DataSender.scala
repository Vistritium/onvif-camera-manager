package onvifsnapshottaker.datasend

import java.net.URI
import java.nio.file.Path

import com.typesafe.scalalogging.LazyLogging
import onvifsnapshottaker.Config
import org.apache.http.client.HttpResponseException
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntityBuilder

import scala.util.{Failure, Success, Try}


object DataSender extends LazyLogging {

  private val config = Config().getConfig("sendFiles")
  private val target = URI.create(config.getString("target"))
  private val apikey = config.getString("apikey")

  def sendFiles(iterable: Iterable[Path]): Try[Unit] = {

    val post = new HttpPost(target)

    val entity = iterable.foldLeft(MultipartEntityBuilder.create())((builder, next) =>
      builder.addBinaryBody(next.getFileName.toString, next.toFile))

    post.setEntity(entity.build())
    post.setHeader("apikey", apikey)

    val response = Config.httpClient.execute(post)
    response.close()
    if (!response.getStatusLine.getStatusCode.toString.startsWith("2")) {
      Failure(new HttpResponseException(response.getStatusLine.getStatusCode, s"Wrong response: ${response.getStatusLine}"))
    } else {
      Success()
    }
  }

}
