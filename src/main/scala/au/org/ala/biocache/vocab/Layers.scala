package au.org.ala.biocache.vocab

import scala.io.Source
import scala.util.parsing.json.JSON

/**
 * Created by mar759 on 18/02/2014.
 */
object Layers {

  lazy val nameToIdMap: Map[String, String] = {
    //get the JSON string for the layers
    val nonDefaultFieldMap = Map[String, String]("aus1" -> "stateProvince", "aus2" -> "lga", "ibra_reg_shape" -> "ibra",
      "ibra_merged" -> "ibra", "imcra4_pb" -> "imcra", "ne_world" -> "country")
    try {
      val json = Source.fromURL("http://spatial.ala.org.au/layers.json").mkString
      val map = JSON.parseFull(json).get.asInstanceOf[Map[String, List[Map[String, AnyRef]]]]
      val layers = map("layerList")
      var idmap = new scala.collection.mutable.HashMap[String, String]
      layers.foreach(layer => {
        val name = layer.get("name")
        val layerType = layer.getOrElse("type", "")
        if (!name.isEmpty) {
          val sname = name.get.asInstanceOf[String].toLowerCase
          val id = nonDefaultFieldMap.getOrElse(sname, getPrefix(layerType.asInstanceOf[String]) + layer.get("id").get.asInstanceOf[Double].toInt)
          idmap += sname -> id
        }
      })
      idmap.toMap
    }
    catch {
      case e: Exception => e.printStackTrace; Map[String, String]()
    }
  }

  lazy val idToNameMap: Map[String, String] = nameToIdMap.map(_.swap)

  def getPrefix(value: String) = if (value == "Environmental") "el" else "cl"
}
