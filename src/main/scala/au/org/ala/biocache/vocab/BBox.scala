package au.org.ala.biocache.vocab

/**
 * Created by mar759 on 18/02/2014.
 */
case class BBox(north:Float, east:Float, south:Float, west:Float){

  def containsPoint(latitude:Float, longitude:Float) = if(east < west) {
    //ITS CROSSED THE DATE LINE
    north >= latitude && south <= latitude && ( (longitude >= -180 && longitude <= east)  || ( longitude >= west && longitude <= 180) )
  } else {
    north >= latitude && south <= latitude && east >= longitude && west <= longitude
  }
}
