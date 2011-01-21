package au.org.ala.util

import au.org.ala.biocache.LocationDAO
import java.io.File
import org.wyki.cassandra.pelops.{ Mutator, Pelops, Policy, Selector }
import scala.collection.mutable.{ LinkedList, ListBuffer }
import org.apache.cassandra.thrift.{ Column, ConsistencyLevel, ColumnPath, SlicePredicate, SliceRange }

/**
 * Loads an export from the old portal database of point lookups.
 * 
 *   select p.id, p.latitude, p.longitude, g.id, g.name, g.region_type, gt.name from point p  inner join point_geo_region pg on p.id=pg.point_id  inner join geo_region g on g.id=pg.geo_region_id  inner join geo_region_type gt on gt.id=g.region_type  into outfile '/tmp/points3.txt';
 * 
 * @author Dave Martin (David.Martin@csiro.au)
 */
object LocationLoader {

  def main(args: Array[String]): Unit = {
    import FileHelper._
    println("Starting Location Loader....")
    val file = new File("/data/biocache/points.txt")
    var counter = 0
    file.foreachLine { line =>
      {
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

        if (counter % 1000 == 0) println(counter)
      }
    }
    println(counter)
    Pelops.shutdown
  }
}