package onvifsnapshottaker

import com.jtheory.jdring.{AlarmEntry, AlarmManager}
import com.typesafe.scalalogging.LazyLogging

object AlarmAdder extends LazyLogging {
  val manager = new AlarmManager()
  var counter = 0

  def addAlarm(hour: Int, handler: Int => Unit): Unit = {
    logger.info(s"Adding alarm for ${hour}")
    this.synchronized {
      manager.addAlarm(new AlarmEntry("", 0, hour, -1, -1, -1, -1, _ => handler(hour)
      ))
    }
  }

  def clearAlarms(): Unit = {
    logger.info("Clearing alarms")
    manager.removeAllAlarms()
  }

}
