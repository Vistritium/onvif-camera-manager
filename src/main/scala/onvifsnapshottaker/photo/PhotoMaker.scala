package onvifsnapshottaker.photo

import scala.util.Try

trait PhotoMaker {

  def shot(): Try[Array[Byte]]

}
