package au.org.ala.biocache.tool

import scala.util.parsing.json.JSON
import java.net.URLEncoder

/**
 * A utility for geocoding the centre points of countries
 */
object GetCountryCentrePoints {

  def main(args:Array[String]){

    val baseUrl = "http://maps.googleapis.com/maps/api/geocode/json?sensor=true&address="

    val countries = scala.io.Source.fromURL(getClass.getResource("/countries.txt"), "utf-8").getLines.toList.map({ row =>
        val values = row.split("\t")
       (values(0), URLEncoder.encode(values(0), "UTF-8"))
    }).toSet

    countries.foreach(c => {

      try {
        val searchUrl = baseUrl + c._2
        val json = JSON.parseFull(scala.io.Source.fromURL(searchUrl).mkString).get.asInstanceOf[Map[String,Object]]
        val results  = json.get("results").get.asInstanceOf[List[Map[String,Object]]]
        val elements = results(0).asInstanceOf[Map[String,Object]]
        val geometry = elements("geometry").asInstanceOf[Map[String,Object]]
        val location  = geometry.get("location").get.asInstanceOf[Map[String,Object]]
        val bounds  = geometry.get("bounds").get.asInstanceOf[Map[String,Object]]
        val northeast  = bounds.get("northeast").get.asInstanceOf[Map[String,Object]]
        val southwest  = bounds.get("southwest").get.asInstanceOf[Map[String,Object]]
        println(c._1+"\t"+location("lat")+"\t"+location("lng")+"\t"+northeast("lat")+"\t"+northeast("lng")+"\t"+southwest("lat")+"\t"+southwest("lng"))
      } catch {
        case e:Exception => //println(e.getMessage)
      }
    })
  }
}