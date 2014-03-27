package au.org.ala.biocache.model

import scala.beans.BeanProperty

sealed case class DupType(@BeanProperty var id:Int){
  def this() = this(-1)
}

object DuplicationTypes {
  val MISSING_YEAR = DupType(1)//"Occurrences were compared without dates."
  val MISSING_MONTH = DupType(2)// "Occurrences were compared without a month and day component.")
  val MISSING_DAY   = DupType(3)//,"Occurrences were compared without a day component.")
  val EXACT_COORD = DupType(4)//," The coordinates were identical.")
  val DIFFERENT_PRECISION = DupType(5)//, "The precision between the occurrences was different.")
  val EXACT_COLLECTOR = DupType(6)//, "Occurrences had identical collectors.")
  val FUZZY_COLLECTOR = DupType(7) // The occurrences had collectors that are similar
  val MISSING_COLLECTOR = DupType(8)// At least one of the occurrences was missing a collector
}