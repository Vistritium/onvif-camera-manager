package onvifsnapshottaker

import java.nio.file.{Files, Path, Paths}
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

object PhotoSaver extends LazyLogging {

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
    val (currentExiffDate: String, saveLocation: Path) = locationAndDateOps(entry)

    val outputSet = Option(Imaging.getMetadata(entry._2)) match {
      case Some(value) => {
        val metadata = value.asInstanceOf[JpegImageMetadata]
        Option(metadata.getExif) match {
          case Some(tiffImageMetadata) => tiffImageMetadata.getOutputSet
          case None => new TiffOutputSet()
        }
      }
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

  private def locationAndDateOps(entry: (Preset, Array[Byte])) = {
    val now = ZonedDateTime.now(timezone.toZoneId)
    val nowDate = Date.from(now.toInstant)
    val formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss")

    val year = getWithLeadingZeros(now.getYear)
    val mounth = getWithLeadingZeros(now.getMonthValue)
    val day = getWithLeadingZeros(now.getDayOfMonth)
    val hour = getWithLeadingZeros(now.getHour)
    val folder = database.resolve(s"${year}").resolve(s"${mounth}").resolve(s"${day}")
    Files.createDirectories(folder)
    val currentExiffDate = formatter.format(nowDate)
    val fileName = s"${entry._1.displayName} ${year}.${mounth}.${day} ${hour}.jpg"
    val saveLocation = folder.resolve(fileName)
    (currentExiffDate, saveLocation)
  }

  private def getWithLeadingZeros(value: Int, limit: Int = 2) = {
    val limitValue = limit - 1
    val str = value.toString
    val length = str.length
    if (length < limit) {
      val difference = limitValue - length
      (0 to difference).map(_ => "0").reduce(_ + _) + str
    } else str
  }

}
