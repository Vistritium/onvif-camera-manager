package onvifsnapshottaker.session

import akka.actor.{Actor, ActorRef, Props}
import com.typesafe.scalalogging.LazyLogging
import onvifsnapshottaker.Config
import onvifsnapshottaker.db.{Preset, Presets}
import onvifsnapshottaker.photo.PhotoMaker

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class PhotoSession(presets: Presets, hour: Int) extends Actor with LazyLogging {

  private implicit val dispatcher: ExecutionContextExecutor = context.dispatcher

  private val delaySecs = Config().getInt("cameraMoveWaitTimeSeconds")

  private var jobs: List[Preset] = presets.presets
  private var finishedJobs: List[(Preset, Array[Byte])] = List()

  private val photoMaker: PhotoMaker = Config.photoMaker

  private var errorCounter = 0
  private val errorMax = 3

  private val startTime = System.nanoTime()

  private var healthRepaier: ActorRef = _

  override def receive: PartialFunction[Any, Unit] = {
    case Tick => {
      if (jobs.nonEmpty) {
        val job = jobs.head
        jobs = jobs.tail
        self ! MoveCamera(job)
      } else {
        if (finishedJobs.nonEmpty) {
          logger.info(s"Finished successfully session at hour ${hour}")
          context.parent ! FinishedJobs(finishedJobs)
        } else {
          logger.warn("Finished but no jobs done")
        }
        context.stop(self)
      }
    }
    case Completed(success) => {
      if (success) {
        self ! Tick
      } else {
        logger.error("Stopping session because camera is not working")
        context.stop(self)
      }
    }
    case MoveCamera(preset) => {
      Try {
        logger.info(s"Moving camera to ${preset}")
        CamPresetMover.move(preset)
      } match {
        case Failure(exception) => {
          errorCounter = errorCounter + 1
          if (errorCounter > errorMax) {
            logger.error(s"Failed to move camera to location ${preset} after ${errorMax} tries. ${exception.getMessage}")
            context.system.scheduler.scheduleOnce(5 seconds, self, Tick)
          } else {
            logger.warn(s"Failed to move camera to location ${preset}. ${exception.getMessage}. Will try again")
            context.system.scheduler.scheduleOnce(15 seconds, self, MoveCamera(preset))
          }
        }
        case Success(_) => {
          errorCounter = 0
          context.system.scheduler.scheduleOnce(delaySecs seconds, self, TakePhoto(preset))
        }
      }
    }
    case TakePhoto(preset) => {
      logger.info(s"Taking photo of ${preset}")
      photoMaker.shot() match {
        case Failure(exception) => {
          errorCounter = errorCounter + 1
          if (errorCounter > errorMax) {
            logger.error(s"Failed to take photo ${preset} after ${errorMax} tries. ${exception.getMessage}")
            context.system.scheduler.scheduleOnce(5 seconds, self, Tick)
          } else {
            logger.warn(s"Failed to take photo ${preset}. ${exception.getMessage}. Will try again")
            context.system.scheduler.scheduleOnce(15 seconds, self, TakePhoto(preset))
          }
        }
        case Success(photo) => {
          errorCounter = 0
          finishedJobs = (preset -> photo) :: finishedJobs
          context.parent ! FinishedJob(preset, photo)
          self ! Tick
        }
      }
    }
  }


  override def preStart(): Unit = {
    logger.info(s"Starting session for ${hour} with ${presets.presets.size} presets")
    healthRepaier = context.actorOf(Props[CameraHealthRepairer])
    context.watch(healthRepaier)
    super.preStart()
  }

  override def postStop(): Unit = {
    super.postStop()
    logger.info(s"PhotoSession ${hour} took ${Duration.fromNanos(System.nanoTime() - startTime).toSeconds} seconds")
  }

  private case object Tick

  private case class TakePhoto(preset: Preset)

  private case class MoveCamera(preset: Preset)

}

case class FinishedJobs(jobs: List[(Preset, Array[Byte])])

case class FinishedJob(preset: Preset, photo: Array[Byte])