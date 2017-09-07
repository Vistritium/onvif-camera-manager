package onvifsnapshottaker

import java.nio.file.{Files, Path, Paths}

object PhotoDatabase {

  type DateFieldsExtractor = {
    def getYear(): Int
    def getMonthValue(): Int
    def getDayOfMonth(): Int
  }

  type YearMonthExtractor = {
    def getYear(): Int
    def getMonthValue(): Int
  }

  val database: Path = Paths.get(Config().getString("photoDatabase"))
  require(Files.isDirectory(database), s"${database} must be directory")
  require(Files.isWritable(database), s"${database} must be writable")

  def getDirectoryForMonth(yearMonthExtractor: YearMonthExtractor): Path = {
    val year = PhotoDatabase.getWithLeadingZeros(yearMonthExtractor.getYear())
    val month = PhotoDatabase.getWithLeadingZeros(yearMonthExtractor.getMonthValue())
    database.resolve(s"${year}").resolve(s"${month}")
  }

  def getDirectoryForDay(dateFieldsExtractor: DateFieldsExtractor): Path = {
    val day = PhotoDatabase.getWithLeadingZeros(dateFieldsExtractor.getDayOfMonth())
    getDirectoryForMonth(dateFieldsExtractor).resolve(s"${day}")
  }

  def getWithLeadingZeros(value: Int, limit: Int = 2): String = {
    val limitValue = limit - 1
    val str = value.toString
    val length = str.length
    if (length < limit) {
      val difference = limitValue - length
      (0 to difference).map(_ => "0").reduce(_ + _) + str
    } else str
  }
}
