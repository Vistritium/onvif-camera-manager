package onvifsnapshottaker.alarm

import java.time._

import com.coreoz.wisp.Scheduler
import com.coreoz.wisp.schedule.Schedules
import com.coreoz.wisp.schedule.cron.CronSchedule
import com.typesafe.scalalogging.LazyLogging

object AlarmAdder extends LazyLogging {
  private var scheduler = new Scheduler()

  private val addAdjustHour = {
    val nowSystemTime = ZonedDateTime.now()
    val nowUTCTime = OffsetDateTime.now(ZoneOffset.UTC)
    val difference = {
      val rawDiff = nowSystemTime.getHour - nowUTCTime.getHour
      if (rawDiff < 0) rawDiff + 24 else rawDiff
    }
    logger.info(s"Hour adjustment cause of system time: ${difference}")
    difference
  }

  def addAlarm(hour: Int, handler: Int => Unit): Unit = synchronized {
    logger.info(s"Adding alarm for ${hour}")
    scheduler.schedule(() => handler(hour), CronSchedule.parseUnixCron(s"0 ${(hour + addAdjustHour) % 24} * * *"))
  }


  def addMinuteAlarm(minute: Int, handler: Int => Unit): Unit = synchronized {
    logger.info(s"Adding alarm for ${minute} minute")
    this.synchronized {
      scheduler.schedule(() => handler(minute), CronSchedule.parseUnixCron(s"${minute} * * * *"))
    }
  }

  def clearAlarms(): Unit = synchronized {
    val oldScheduler = scheduler
    scheduler = new Scheduler()
    oldScheduler.gracefullyShutdown()
  }

  def addAndFire(hour: Int, handler: Int => Unit): Unit = synchronized {
    logger.info(s"Add and fire ${hour}")
    scheduler.schedule(() => handler(hour), Schedules.executeOnce(Schedules.fixedDelaySchedule(Duration.ofSeconds(3))))
  }

}
