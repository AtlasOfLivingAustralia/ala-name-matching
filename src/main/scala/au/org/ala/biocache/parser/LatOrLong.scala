package au.org.ala.biocache.parser

object LatOrLong extends Enumeration {
  type LatOrLong = Value
  val Latitude, Longitude = Value
  val latDirString = "s|south|n|north".split("\\|").toSet
  val longDirString = "w|west|e|east".split("\\|").toSet

  def getDirection(raw:String): Option[LatOrLong] = {
    val normalised = raw.toLowerCase.trim
    if(latDirString.contains(normalised)) Some(Latitude)
    else if (longDirString.contains(normalised)) Some(Longitude)
    else None
  }
}