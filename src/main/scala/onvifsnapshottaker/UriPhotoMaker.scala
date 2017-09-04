package onvifsnapshottaker

import java.util.Objects

import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet}

import scala.util.Try

object UriPhotoMaker extends PhotoMaker {

  private val shotUri = Config().getString("snapshotUri")

  def shot(): Try[Array[Byte]] = {
    var response: CloseableHttpResponse = null
    val res = Try {
      val get = new HttpGet(shotUri)
      response = Config.httpClient.execute(get)
      if (response.getStatusLine.getStatusCode.toString.startsWith("2")) {
        val bytes: Array[Byte] = readContent(response)
        bytes
      } else {
        throw new IllegalStateException(s"Non successful response ${response.getStatusLine}")
      }
    }
    if (Objects.nonNull(response)) {
      response.close()
    }
    res
  }

  private def readContent(response: CloseableHttpResponse) = {
    val bytes = if (response.getEntity.getContentLength > 0) {
      IOUtils.toByteArray(response.getEntity.getContent, response.getEntity.getContentLength)
    } else {
      IOUtils.toByteArray(response.getEntity.getContent)
    }
    bytes
  }
}
