package onvifsnapshottaker.session

import onvifsnapshottaker.Config
import onvifsnapshottaker.db.Preset
import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils

object CamPresetMover {

  private val templateUri = Config().getString("camPresetMoveUri")

  def move(preset: Preset): Unit = {

    val uri = templateUri.replace("{name}", preset.name)
    val get = new HttpGet(uri)

    val response = Config.httpClient.execute(get)
    EntityUtils.consume(response.getEntity)
    if (!response.getStatusLine.getStatusCode.toString.startsWith("2")) {
      throw new IllegalStateException(s"Couldn't move to preset ${response.getStatusLine}")
    }

  }

}
