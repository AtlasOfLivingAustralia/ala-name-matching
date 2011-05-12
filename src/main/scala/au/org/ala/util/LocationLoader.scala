package au.org.ala.util

import au.org.ala.biocache.LocationDAO
import au.org.ala.biocache.PersistenceManager
import au.org.ala.biocache.Config
import java.io.File

/**
 * Loads an export from the gazetteer based on point from the old portal database.
 * 
 *   select p.id, p.latitude, p.longitude, g.id, g.name, g.region_type, gt.name from point p  inner join point_geo_region pg on p.id=pg.point_id  inner join geo_region g on g.id=pg.geo_region_id  inner join geo_region_type gt on gt.id=g.region_type  into outfile '/tmp/points3.txt';
 *
 * New file structure : longitude, latitude, state, lga, ibra, imcra
 *
 * @author Dave Martin (David.Martin@csiro.au)
 */
object LocationLoader {

  def main(args: Array[String]): Unit = {
    import FileHelper._
    println("Starting Location Loader....")
    val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]
    val file = new File("/data/biocache/gaz_points.csv")
    var counter = 0
    file.foreachLine { line => {
        counter += 1
        //add point with details to
        val parts = line.split(',')
        val latitude = (parts(1).toFloat)
        val longitude = (parts(0).toFloat)
        val state:Option[String] = optioniseValue(parts(2))
        val lga:Option[String] = optioniseValue(parts(3))
        val ibra:Option[String] = optioniseValue(parts(4))
        val imcra:Option[String] = optioniseValue(parts(5))
        val habitat:String = if(!ibra.isEmpty) "terrestrial" else if(!imcra.isEmpty) "marine" else null
        //only add/update the points that have a gazetteer value
        if(!state.isEmpty || !lga.isEmpty || !ibra.isEmpty || !imcra.isEmpty){
          val values =Map[String, String]("stateProvince"->state.getOrElse(null),
                                          "lga"->lga.getOrElse(null),
                                          "ibra" ->ibra.getOrElse(null),
                                          "imcra"-> imcra.getOrElse(null), "habitat"-> habitat)
          LocationDAO.addRegionToPoint(latitude, longitude, values)
          if (counter % 1000 == 0) println(counter +": "+latitude+"|"+longitude+", mapping: "+ values)
        }


        
      }
    }
    println(counter)
    persistenceManager.shutdown
  }
  def optioniseValue(value:String):Option[String]= if (value == "null") None else Some(value)
  
}

/**
 * Loads the locations from a dump in the portal db. This is the "old" way to
 * load the location cache
 */
object OldLocationLoader {

  def main(args: Array[String]): Unit = {
    import FileHelper._
    println("Starting Location Loader....")
    val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]
    val file = new File("/data/biocache/points.txt")
    var counter = 0
    file.foreachLine { line => {
        counter += 1
        //add point with details to
        val parts = line.split('\t')
        val latitude = (parts(1).toFloat) / 10000
        val longitude = (parts(2).toFloat) / 10000
        val geoRegionName = parts(4)
        val geoRegionTypeId = parts(5).toInt
        val geoRegionTypeName = parts(6)
        val regionMapping: Option[Map[String, String]] = {
          if (geoRegionTypeId >= 1 && geoRegionTypeId <= 2) {
            Some(Map("stateProvince" -> geoRegionName))
          } else if (geoRegionTypeId >= 3 && geoRegionTypeId <= 12) {
            Some(Map("lga" -> geoRegionName))
          } else if (geoRegionTypeId == 2000) {
            Some(Map("ibra" -> geoRegionName, "habitat" -> "terrestrial"))
          } else if (geoRegionTypeId >= 3000 && geoRegionTypeId < 4000) {
            Some(Map("imcra" -> geoRegionName, "habitat" -> "marine"))
          } else {
            None
          }
        }
        if (!regionMapping.isEmpty) {
         
          LocationDAO.addRegionToPoint(latitude, longitude, regionMapping.get)
        }

        if (counter % 1000 == 0) println(counter +": "+latitude+"|"+longitude+", mapping: "+ regionMapping.getOrElse("None"))
      }
    }
    println(counter)
    persistenceManager.shutdown
  }
}