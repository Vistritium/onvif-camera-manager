package onvifsnapshottaker

import java.time.ZonedDateTime

import akka.actor.{Actor, ActorRef, Props, Terminated}
import com.typesafe.scalalogging.LazyLogging
import onvifsnapshottaker.alarm.AlarmAdder
import onvifsnapshottaker.datasend.DataSenderTick
import onvifsnapshottaker.db.Root
import onvifsnapshottaker.session.{FinishedJob, FinishedJobs, PhotoSession}

import scala.util.{Failure, Try}

class MainWorker extends Actor with LazyLogging {

  private var sessionMaker: Option[ActorRef] = None
  private var config: Root = _

  private val datasender = context.actorSelection("/user/datasender")

  private val testSession = Config().getBoolean("testSession")

  override def receive: Receive = {

    case ConfigChanged(root) => {
      logger.info("Received config change")
      config = root
      AlarmAdder.clearAlarms()
      config.triggers.hoursOfDay.foreach(hour => {
        AlarmAdder.addAlarm(hour, (hour) => self ! Fire(hour))
      })
      if (testSession) {
        AlarmAdder.addAndFire(ZonedDateTime.now().getHour, (hour) => self ! Fire(hour))
      }

    }

    case Fire(hour) => {
      logger.info(s"Hour ${hour} fired")
      if (sessionMaker.nonEmpty) {
        logger.warn("New session wanted to start but there is Session still running..")
      } else {
        sessionMaker = Some(context.actorOf(Props(classOf[PhotoSession], config.presets, hour)))
        context.watch(sessionMaker.get)
      }
    }

    case FinishedJobs(jobs) => {
      logger.info("PhotoSession finished")
      datasender ! DataSenderTick
      sessionMaker = None
    }

    case Terminated(_) => {
      sessionMaker = None
    }
    case FinishedJob(preset, data) => {
      Try {
        PhotoSaver.savePhoto(preset -> data)
      } match {
        case Failure(exception) => {
          logger.warn(s"Couldn't save photo ${exception.getMessage}")
          logger.debug(s"Couldn't save photo ${exception.getMessage}", exception)
        }
        case _ =>
      }

    }

  }

  private case class Fire(hour: Int)

}
