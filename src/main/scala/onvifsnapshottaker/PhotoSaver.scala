package onvifsnapshottaker

import java.nio.file.{Files, Paths}
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.{Date, TimeZone}

import com.typesafe.scalalogging.LazyLogging
import onvifsnapshottaker.db.Preset
import org.apache.commons.imaging.Imaging
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet

object PhotoSaver extends LazyLogging{

  private val database = Paths.get(Config().getString("photoDatabase"))
  require(Files.isDirectory(database), s"${database} must be directory")
  require(Files.isWritable(database), s"${database} must be writable")

  private val timezone = TimeZone.getTimeZone(Config().getString("savingFormatTimezone"))
  private val (lat, lon) = {
    val config = Config().getConfig("location")
    config.getDouble("lat") -> config.getDouble("lon")
  }

  def savePhotos(images: List[(Preset, Array[Byte])], hour: Int): Unit = {
    images.foreach(entry => {
      savePhoto(entry)
    })
  }

  def savePhoto(entry: (Preset, Array[Byte])): Unit = {
    val now = ZonedDateTime.now(timezone.toZoneId)
    val nowDate = Date.from(now.toInstant)
    val formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss")

    val folder = database.resolve(s"${now.getYear}").resolve(s"${now.getMonth}").resolve(s"${now.getDayOfMonth}")
    Files.createDirectories(folder)

    val currentExiffDate = formatter.format(nowDate)

    val fileName = s"${entry._1.displayName} ${now.getYear}/${now.getMonth}/${now.getDayOfMonth} ${now.getHour}"
    val saveLocation = folder.resolve(fileName)

    val metadata = Imaging.getMetadata(entry._2).asInstanceOf[JpegImageMetadata]
    val outputSet = Option(metadata.getExif) match {
      case Some(value) => value.getOutputSet
      case None => new TiffOutputSet()
    }

    val directory = outputSet.getOrCreateExifDirectory()
    directory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED)
    directory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL)
    directory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED, currentExiffDate)
    directory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, currentExiffDate)
    outputSet.setGPSInDegrees(lon, lat)
    val outputStream = Files.newOutputStream(saveLocation)
    new ExifRewriter().updateExifMetadataLossless(entry._2, outputStream, outputSet)
    logger.info(s"Saved ${entry._1} to ${saveLocation}")
  }

  def main(args: Array[String]): Unit = {
    val now = ZonedDateTime.now(timezone.toZoneId)
    val nowDate = Date.from(now.toInstant)
    val formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss")
    val str = formatter.format(nowDate)

    println(str)
  }

}
