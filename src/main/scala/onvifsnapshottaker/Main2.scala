package onvifsnapshottaker

import akka.actor.{ActorSystem, Props}
import com.typesafe.scalalogging.LazyLogging

object Main2 extends LazyLogging {

  def main(args: Array[String]): Unit = {
    logger.info("App started")

    val system = ActorSystem()

    val db = system.actorOf(Props[DB], "db")
    val mainWorker = system.actorOf(Props[MainWorker])
    db.tell(RegisterForConfig, mainWorker)
  }


}
