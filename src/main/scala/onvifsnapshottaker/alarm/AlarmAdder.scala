package onvifsnapshottaker.alarm

import java.time._
import java.util.Date

import com.jtheory.jdring.{AlarmEntry, AlarmListener, AlarmManager}
import com.typesafe.scalalogging.LazyLogging

object AlarmAdder extends LazyLogging {
  private val manager = new AlarmManager()

  private val addAdjustHour = {
    val nowSystemTime = ZonedDateTime.now()
    val nowUTCTime = OffsetDateTime.now(ZoneOffset.UTC)
    val difference = nowSystemTime.getHour - nowUTCTime.getHour
    logger.info(s"Hour adjustment cause of system time: ${difference}")
    difference
  }

  def addAlarm(hour: Int, handler: Int => Unit): Unit = {
    logger.info(s"Adding alarm for ${hour}")
    this.synchronized {
      manager.addAlarm(new AlarmEntry(hour.toString, 0, hour + addAdjustHour, -1, -1, -1, -1, _ => handler(hour)
      ))
    }
  }

  def addMinuteAlarm(minute: Int, handler: Int => Unit): Unit = {
    logger.info(s"Adding alarm for ${minute} minute")
    this.synchronized {
      manager.addAlarm(new AlarmEntry(minute.toString, minute, -1, -1, -1, -1, -1, _ => handler(minute)
      ))
    }
  }

  def clearAlarms(): Unit = {
    logger.info("Clearing alarms")
    manager.removeAllAlarms()
  }

  def addAndFire(hour: Int, handler: Int => Unit): Unit = {
    logger.info(s"Add and fire ${hour}")
    val alarmListener: AlarmListener = _ => handler(hour)
    val date = Date.from(Instant.now().plusSeconds(3))
    manager.addAlarm(hour.toString, date, alarmListener)
  }

}
