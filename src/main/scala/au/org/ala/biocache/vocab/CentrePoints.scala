package au.org.ala.biocache.vocab

trait CentrePoints {

  val north = 'N'
  val south = 'S'
  val east = 'E'
  val west = 'W'

  protected val map:Map[String, (LatLng, BBox)]
  protected val vocab:Vocab

  def matchName(str:String) = map.get(str.toLowerCase)

  /**
   * Returns true if the supplied coordinates are the centre point for the supplied
   * state or territory
   */
  def coordinatesMatchCentre(state:String, decimalLatitude:String, decimalLongitude:String) : Boolean = {

    val matchedState = vocab.matchTerm(state)
    if(!matchedState.isEmpty && decimalLatitude != null && decimalLongitude != null){

      val latlngBBox = map.get(matchedState.get.canonical.toLowerCase)
      if(!latlngBBox.isEmpty){

        val (latlng, bbox) = latlngBBox.get

        //how many decimal places are the supplied coordinates
        try {
          //convert supplied values to float
          val latitude = decimalLatitude.toFloat
          val longitude = decimalLongitude.toFloat

          val latDecPlaces = noOfDecimalPlace(latitude)
          val longDecPlaces = noOfDecimalPlace(longitude)

          //approximate the centre points appropriately
          val approximatedLat = round(latlng.latitude,latDecPlaces)
          val approximatedLong = round(latlng.longitude,longDecPlaces)

          //compare approximated centre point with supplied coordinates
          approximatedLat == latitude && approximatedLong == longitude

        } catch {
          case e:NumberFormatException => false
        }
      } else {
        false
      }
    } else {
      false
    }
  }

  def getHemispheresForPoint(lat:Double, lng:Double) : (Char,Char) = (
    if(lat >= 0) north else south,
    if(lng >= 0) east else west
  )

  def getHemispheres(region:String) : Option[Set[Char]] = {
    val matchedRegion = vocab.matchTerm(region)
    map.get(matchedRegion.get.canonical.toLowerCase) match {
      case Some((latlng, bbox)) => {
        Some(Set(
          if(bbox.north >= 0) north else south,
          if(bbox.south >  0) north else south,
          if(bbox.east  >= 0) east else west,
          if(bbox.west >  0) east else west
        ))
      }
      case _ => None
    }
  }

  /**
   * Round to the supplied no of decimal places.
   */
  def round(number:Float, decimalPlaces:Int) : Float = {
    if(decimalPlaces>0){
      var x = 1
      for (i <- 0 until decimalPlaces) x = x * 10
      (((number * x).toInt).toFloat) / x
    } else {
      number.round
    }
  }

  def noOfDecimalPlace(number:Float) : Int = {
    val numberString = number.toString
    val decimalPointLoc = numberString.indexOf(".")
    if(decimalPointLoc<0) {
      0
    } else {
       numberString.substring(decimalPointLoc+1).length
    }
  }

  def loadFromFile(filePath:String): Map[String, (LatLng,BBox)] = {
    scala.io.Source.fromURL(getClass.getResource(filePath), "utf-8").getLines.toList.map({ row =>
        val values = row.split("\t")
        val name = values.head.toLowerCase
        val coordinates = values.tail.map(x => x.toFloat)
        name -> (
          LatLng(coordinates(0), coordinates(1)),
          //12.630618	-69.8644638	12.406093	-70.0701141
          BBox(coordinates(2),coordinates(3),coordinates(4),coordinates(5))
        )
    }).toMap
  }
}
