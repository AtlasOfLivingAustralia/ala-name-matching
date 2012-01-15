package au.org.ala.util

import au.org.ala.biocache.CassandraPersistenceManager

object MigrationUtil {

  val elPattern = """el[0-9]{1,}\.p""".r
  val clPattern = """cl[0-9]{1,}\.p""".r

  def isEL(str:String) = !elPattern.findFirstMatchIn(str).isEmpty
  def isCL(str:String) = !clPattern.findFirstMatchIn(str).isEmpty

  def main(args:Array[String]){

    var sourceHost = ""
    var sourcePort = -1
    var targetHost = ""
    var targetPort = -1
    var dataResource:Option[String] = None
    var startRK:Option[String] = None
    var keyspace = ""
    var columnFamily = ""
    val obsCols = List("originalDecimalLatitude","originalDecimalLongitude","originalLocality",
      "originalLocationRemarks","originalVerbatimLatitude","originalVerbatimLongitude","lastLoadTime")

    val parser = new OptionParser("Cassandra migration") {
        arg("source-host", "", {v:String => sourceHost = v})
        arg("source-port", "", {v:String => sourcePort = v.toInt})
        arg("target-host", "", {v:String => targetHost = v})
        arg("target-port", "", {v:String => targetPort = v.toInt})
        arg("keyspace", "", {v:String => keyspace = v})
        arg("column-family","",{v:String => columnFamily = v})
        opt("dr", "data-resource", "data resource to migrate. All records will be migrated if none specified", {v:String =>dataResource=Some(v)})
        opt("s","start", "The row key to start the migration." ,{v:String => startRK=Some(v)})
    }

    var counter = 0
    val originalStart = System.currentTimeMillis
    var start = System.currentTimeMillis()
    var finish = System.currentTimeMillis()
    if(parser.parse(args)){
      //page through source keyspace, and add record to target
      val sourcePM = new CassandraPersistenceManager(sourceHost, sourcePort, "source", keyspace)
      val targetPM = new CassandraPersistenceManager(targetHost, targetPort, "target", keyspace)
      
      val startUuid = if(startRK.isDefined) startRK.get else if(dataResource.isDefined) dataResource.get +"|" else ""
      val endUuid = if(dataResource.isDefined) startUuid + "~" else ""      

      sourcePM.pageOverAll(columnFamily, (guid, map) => {
        counter += 1
        if (counter % 1000 == 0){
          finish = System.currentTimeMillis()
          println(">> processing record: " + counter +", guid: " + guid + 
                  ", time taken for last 1000: " + ((finish.toFloat-start.toFloat) / 1000) +
                  ", total time " + (finish - originalStart).toFloat / 60000f +" minutes")
          start = System.currentTimeMillis()
        }
        val deleted = map.getOrElse("deleted", "false").toBoolean
        if (!deleted){
          val mapFiltered = map.filter({ case (key, value) => {
            if(isEL(key)) false
            else if (isCL(key)) false
            else if (obsCols.contains(key)) false
            else if (value == "[]") false
            else true
          }})
          targetPM.put(guid, columnFamily, mapFiltered)
        }
        true
      },startUuid,endUuid)
    }
  }
}