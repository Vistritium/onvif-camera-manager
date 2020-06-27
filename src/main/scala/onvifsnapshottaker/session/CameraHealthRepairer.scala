package onvifsnapshottaker.session

import java.util.Objects

import akka.actor.{Actor, Cancellable}
import com.typesafe.scalalogging.LazyLogging
import onvifsnapshottaker.Config

import concurrent.duration._
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

class CameraHealthRepairer extends Actor with LazyLogging {

  private implicit val executionContext: ExecutionContextExecutor = context.system.dispatcher

  private val finalTimeoutDuration = 2 minutes

  private var finalTimeout: Cancellable = _
  private var check: Cancellable = _

  override def receive: Receive = {
    case Check => {

      val triedPhoto = Config.photoMaker.shot()
      if (triedPhoto.isFailure) {
        logger.debug("Check failure", triedPhoto.failed.get)
        logger.debug("Check...failed")
        scheduleCheck()
      } else {
        finish(true)
      }
    }
    case FinalTimeout => {

      logger.info("Final timeout")
      val triedPhoto = Config.photoMaker.shot()
      if (triedPhoto.isFailure) {
        logger.error("Final failure", triedPhoto.failed.get)
        finish(false)
      } else {
        finish(true)
      }
    }
  }

  override def preStart(): Unit = {
    super.preStart()
    val triedPhoto = Config.photoMaker.shot()
    if (triedPhoto.isFailure) {
      logger.debug("Photo failure", triedPhoto.failed.get)
      logger.info(s"Photo failure. Will try restarting camera..", triedPhoto.failed.get)
      finalTimeout = context.system.scheduler.scheduleOnce(finalTimeoutDuration, self, FinalTimeout)
      RebootDevice.reboot() match {
        case Failure(exception) =>
          logger.error("Failed to reboot", exception)
          finish(success = false, wasBroken = false)
        case Success(_) => {
          scheduleCheck()
        }
      }

    } else {
      finish(true)
    }
  }

  private def finish(success: Boolean, wasBroken: Boolean = true): Unit = {
    if(wasBroken){
      logger.info(s"Finishing repairing with ${if (success) "success" else "failure"}")
    } else {
      logger.info(s"No reboot required. Finishing..")
    }

    context.parent ! Completed(success)
    if (Objects.nonNull(finalTimeout)) {
      finalTimeout.cancel()
    }
    if (Objects.nonNull(check)) {
      check.cancel()
    }
    context.stop(self)
  }

  private def scheduleCheck(): Unit = {
    check = context.system.scheduler.scheduleOnce(10 seconds, self, Check)
  }

  private case object FinalTimeout

  private case object Check

}

case class Completed(success: Boolean)
