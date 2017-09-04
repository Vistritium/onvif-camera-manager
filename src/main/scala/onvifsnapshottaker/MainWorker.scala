package onvifsnapshottaker

import akka.actor.{Actor, ActorRef, Props, Terminated}
import com.typesafe.scalalogging.LazyLogging
import onvifsnapshottaker.db.Root

class MainWorker extends Actor with LazyLogging {

  var sessionMaker: Option[ActorRef] = None
  var config: Root = _

  override def receive: Receive = {

    case ConfigChanged(root) => {
      config = root
      AlarmAdder.clearAlarms()
      config.triggers.hoursOfDay.foreach(hour => {
        AlarmAdder.addAlarm(hour, (hour) => self ! Fire(hour))
      })
    }

    case Fire(hour) => {
      if (sessionMaker.nonEmpty) {
        logger.warn("New session wanted to start but there is Session still running..")
      } else {
        sessionMaker = Some(context.actorOf(Props(classOf[PhotoSession], config.presets, hour)))
        context.watch(sessionMaker.get)
      }
    }

    case FinishedJobs(jobs) => {
      logger.info("PhotoSession finished")
      sessionMaker = None
    }

    case Terminated(_) => {
      sessionMaker = None
    }
    case FinishedJob(preset, data) => {
      PhotoSaver.savePhoto(preset -> data)
    }

  }

  private case class Fire(hour: Int)

}
