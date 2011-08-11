package au.org.ala.util

import scala.util.parsing.json.JSON

/**
 * A utility for geocoding the centre points of countries
 */
object GetCountryCentrePoints {

  def main(args:Array[String]){

    val baseUrl = "http://maps.googleapis.com/maps/api/geocode/json?sensor=true&address="

    val countries = scala.io.Source.fromURL(getClass.getResource("/countries.txt"), "utf-8").getLines.toList.map({ row =>
        val values = row.split("\t")
        values(0)
    }).toSet

    countries.foreach(c => {

      try {
        val searchUrl = baseUrl + c
        val json = JSON.parseFull(scala.io.Source.fromURL(searchUrl).mkString).get.asInstanceOf[Map[String,Object]]
        val results  = json.get("results").get.asInstanceOf[List[Map[String,Object]]]
        val elements = results(0).asInstanceOf[Map[String,Object]]
        val geometry = elements("geometry").asInstanceOf[Map[String,Object]]
        val location  = geometry.get("location").get.asInstanceOf[Map[String,Object]]
        println(c+"\t"+location("lat")+"\t"+location("lng"))
      } catch {
        case _ =>
      }
    })
  }
}