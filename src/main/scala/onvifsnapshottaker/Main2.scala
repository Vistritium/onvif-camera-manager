package onvifsnapshottaker

import com.typesafe.scalalogging.LazyLogging

object Main2 extends LazyLogging {


  def main(args: Array[String]): Unit = {

/*    val root = Root(
      Presets(List(Preset("Garden", "Orgród"))),
      Triggers(List(9, 13, 18, 22))
    )

    val str = Config.objectMapper.writeValueAsString(root)
    logger.info(str)*/

    UriPhotoMaker.shot()

  }


}
