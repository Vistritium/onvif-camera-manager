package onvifsnapshottaker

import java.nio.file.{Files, Paths, StandardOpenOption}

import akka.actor.{Actor, ActorRef}
import com.typesafe.scalalogging.LazyLogging
import onvifsnapshottaker.db.Root
import org.apache.commons.codec.digest.DigestUtils

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class DB extends Actor with LazyLogging {

  private implicit val dispatcher: ExecutionContextExecutor = context.dispatcher

  private val path = Paths.get(Config().getString("configDb"))
  private var listeners: List[ActorRef] = List()

  private var config: Root = _
  private var digest: Array[Byte] = _

  override def receive: Receive = {
    case RegisterForConfig => {
      logger.debug(s"${sender()} registered for config change")
      listeners = sender() :: listeners
      sender() ! ConfigChanged(config)
    }
    case SetConfig(root) => {
      Try {
        val bytes = Config.objectMapper.writeValueAsBytes(root)
        Files.write(path, bytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
      } match {
        case Failure(exception) =>
          logger.error("Couldn't write new config.", exception)
        case Success(_) =>
          config = root
          listeners.foreach(_ ! ConfigChanged(config))
      }
    }
    case Tick if true => {
      logger.trace("Config reload disabled")
    }
    case Tick => {
      val bytes = Files.readAllBytes(path)
      val digest = DigestUtils.sha1(bytes)
      if (digest.deep != this.digest.deep) {
        Try {
          Config.objectMapper.readValue(bytes, classOf[Root])
        } match {
          case Failure(exception) => {
            logger.error("Parse of new config file failed", exception)
          }
          case Success(value) => {
            config = value
            this.digest = digest
            listeners.foreach(_ ! ConfigChanged(config))
          }
        }
      }
      context.system.scheduler.scheduleOnce(5 minutes, self, Tick)
    }
  }

  override def preStart(): Unit = {
    super.preStart()
    if (!(Files.isWritable(path) && Files.isReadable(path))) {
      throw new IllegalStateException(s"DB file is not readable/writable ${path}")
    }

    val bytes = Files.readAllBytes(path)
    digest = DigestUtils.sha1(bytes)
    config = Config.objectMapper.readValue(bytes, classOf[Root])
    context.system.scheduler.scheduleOnce(5 minutes, self, Tick)
  }

  private case object Tick

}


case object RegisterForConfig


case class ConfigChanged(root: Root)

case class SetConfig(root: Root)