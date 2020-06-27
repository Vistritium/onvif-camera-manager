package onvifsnapshottaker.session

import java.util.Collections

import com.typesafe.scalalogging.LazyLogging
import onvifsnapshottaker.Config
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.{BasicHeader, BasicNameValuePair}
import org.apache.http.util.EntityUtils
import org.apache.http.{HttpHeaders, NameValuePair}

import scala.util.{Failure, Success, Try}

object RebootDevice extends LazyLogging {

  private val url = {
    val host = Config().getString("host")
    s"http://${host}/web/cgi-bin/hi3510/param.cgi"
  }

  private val authorization = {
    val user = Config().getString("camerauser.name")
    val pass = Config().getString("camerauser.password")
    logger.info(s"Using basic auth ${user}:${pass}")
    val auth = user + ":" + pass
    val encodedAuth = Base64.encodeBase64(auth.getBytes("utf-8"))
    val authHeader = "Basic " + new String(encodedAuth)
    new BasicHeader(HttpHeaders.AUTHORIZATION, authHeader)
  }

  def reboot(): Try[String] = {
    Try {
      logger.info("Rebooting camera")
      val post = new HttpPost(url)
      logger.info(url)
      post.addHeader(authorization)
      val pair: NameValuePair = new BasicNameValuePair("cmd", "sysreboot")
      val entity = new UrlEncodedFormEntity(Collections.singletonList(pair))
      post.setEntity(entity)

      val response = Config.httpClient.execute(post)
      val body = IOUtils.toString(response.getEntity.getContent, "utf-8")
      EntityUtils.consume(response.getEntity)
      if (!response.getStatusLine.getStatusCode.toString.startsWith("2")) {
        logger.info(s"Reboot failure ${response.getStatusLine} ${body}")
        Failure(new IllegalStateException("non 2xx response"))
      } else {
        logger.info(s"Reboot success ${body}")
        Success(body)
      }
    }.flatten
  }

}
