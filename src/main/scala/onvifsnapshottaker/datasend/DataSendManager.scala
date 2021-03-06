package onvifsnapshottaker.datasend

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}
import java.time.LocalDateTime

import akka.actor.{Actor, Cancellable}
import com.typesafe.scalalogging.LazyLogging
import onvifsnapshottaker.{Config, PhotoDatabase}
import resource._
import util.retry.blocking
import util.retry.blocking.{Retry, RetryStrategy}

import scala.collection.JavaConverters._
import scala.compat.java8.StreamConverters._
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class DataSendManager extends Actor with LazyLogging {

  private val enabled = Config().getBoolean("sendFiles.enabled")
  logger.info(s"DataSendManager is ${if (enabled) "enabled" else "disabled"}")

  private implicit val dispatcher: ExecutionContextExecutor = context.dispatcher

  private var filesToSend: List[Path] = List()

  private var ticker: Option[Cancellable] = None

  override def receive: PartialFunction[Any, Unit] = {
    case DataSenderTick if !enabled => logger.info("Tick [disabled]")
    case DataSenderTick => {
      logger.info("Tick")
      val now = LocalDateTime.now
      checkMonthDirectory(PhotoDatabase.getDirectoryForMonth(now))
      checkMonthDirectory(PhotoDatabase.getDirectoryForMonth(now.minusMonths(1)))
      if (filesToSend.nonEmpty) {
        logger.info(s"Will try to send ${filesToSend.size} files")
        filesToSend.grouped(10).foldLeft(true)((wasSuccess, next) =>
          if (wasSuccess) sendNotSentFiles(next)
          else false
        )
      }
      filesToSend = List()
    }
  }

  def checkMonthDirectory(path: Path): Unit = {
    if (Files.exists(path)) {
      managed(Files.newDirectoryStream(path)).map(_.iterator().asScala.toList).opt.get
        .filter(Files.isDirectory(_))
        .foreach(checkDayDirectory)
    } else {
      logger.debug(s"checkMonthDirectory ${path} does not exist")
    }
  }

  def checkDayDirectory(path: Path): Unit = {
    logger.debug(s"Checking day directory ${path.toString}")
    val metadata = path.resolve(".metadata")
    val filesInDirectory =
      managed(Files.newDirectoryStream(path)).map(_.iterator().asScala.toList).opt.get
        .filter(Files.isRegularFile(_))
        .filterNot(_.getFileName.toString.startsWith("."))
        .map(_.toAbsolutePath).toSet
    if (Files.exists(metadata)) {
      val lines = managed(Files.lines(metadata)).map(_.toScala[Set]).opt.get
      val alreadySentFiles = lines.filter(_.nonEmpty)
        .flatMap(line => Try(path.resolve(line)).toOption)
        .filter(Files.isRegularFile(_)).map(_.toAbsolutePath)
      val notSent = filesInDirectory -- alreadySentFiles
      if (notSent.nonEmpty) {
        handleFilesNotSent(notSent)
      }
    } else {
      logger.debug(s"Sending whole directory ${path}")
      handleFilesNotSent(filesInDirectory)
    }
  }

  def handleFilesNotSent(notSent: Iterable[Path]): Unit = {
    filesToSend = notSent.toList ::: filesToSend
  }

  def sendNotSentFiles(files: List[Path]): Boolean = {
    Retry(DataSender.sendFiles(files) match {
      case Failure(exception) => throw exception
      case Success(value) => value
    })(RetryStrategy.fixedBackOff(retryDuration = 5.seconds, maxAttempts = 3)) match {
      case blocking.Success(_) => {
        logger.info(s"Successfully sent ${files.size} files")
        markFilesSent(files)
        true
      }
      case blocking.Failure(exception) => {
        logger.error(s"Couldn't send ${files.size} files ${exception.getMessage}")
        logger.debug(s"Couldn't send ${files.size} files", new RuntimeException(exception))
        false
      }
    }
  }

  def markFilesSent(files: List[Path]): Unit = {
    val foldersToFiles = files.map(f => f.getParent.toAbsolutePath -> f).groupBy(_._1).mapValues(_.map(_._2))
    logger.debug(s"folderToFiles: ${foldersToFiles.map(x => s"${x._1} -> ${x._2.mkString(", ")}").mkString("\n")}")
    foldersToFiles.foreach(folderToFiles => {
      val (dir, files) = folderToFiles
      logger.info(s"Marking ${files.size} files as sent for ${dir}")
      val metadata = dir.resolve(".metadata")
      Files.write(metadata, files.map(_.getFileName).mkString("", "\n", "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    })
  }
}


case object DataSenderTick
