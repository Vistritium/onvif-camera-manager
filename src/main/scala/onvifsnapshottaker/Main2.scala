package onvifsnapshottaker

import akka.actor.{ActorSystem, Props}
import akka.pattern.{Backoff, BackoffSupervisor}
import com.typesafe.scalalogging.LazyLogging
import onvifsnapshottaker.alarm.AlarmAdder
import onvifsnapshottaker.datasend.{DataSendManager, DataSenderTick}

import scala.concurrent.duration._

object Main2 extends LazyLogging {

  def main(args: Array[String]): Unit = {
    logger.info("App started")

    val system = ActorSystem()

    val db = system.actorOf(Props[DB], "db")
    val mainWorker = system.actorOf(Props[MainWorker])
    db.tell(RegisterForConfig, mainWorker)

    val dataSender = system.actorOf(BackoffSupervisor.props(Backoff.onFailure(
      Props[DataSendManager],
      "datasenderworker",
      1 minute,
      5 minutes,
      0.2
    )), "datasender")

    dataSender ! DataSenderTick

    AlarmAdder.addMinuteAlarm(Config().getInt("dataSendMinute"), (minute) => dataSender ! DataSenderTick)


  }


}
