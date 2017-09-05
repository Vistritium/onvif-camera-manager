package onvifsnapshottaker

import de.onvif.soap.OnvifDevice
import collection.JavaConverters._

object Main {


  def main2(args: Array[String]): Unit = {

    val device = new OnvifDevice("marisa.maciejnowicki.com:5056", "", "")

    val profiles = device.getDevices.getProfiles
    println(profiles.asScala.map(_.getToken))

    val snapshot = device.getMedia.getTCPStreamUri("MainProfileToken")
    val screenshot = device.getMedia.getRTSPStreamUri("MainProfileToken")
    println(snapshot)
    println(screenshot)
  }



}
