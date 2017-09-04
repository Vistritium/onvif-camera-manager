package onvifsnapshottaker

import scala.util.Try

trait PhotoMaker {

  def shot(): Try[Array[Byte]]

}
