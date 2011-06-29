package au.org.ala.util

/**
 * Reloads the data resource information for the records
 */
import au.org.ala.biocache._

object ReloadDataResources {

   val cache = new scala.collection.mutable.HashMap[String, Map[String, String]]
   val pm = Config.persistenceManager
   def main(args: Array[String]): Unit = {
    println("Reloading the data resource values...")
    val start = if(args.length>0)args(0) else ""
    reload(start)
//    println(getDataResourceFromWS("dr376"))
//    println(getDataResourceFromWS("dr461"))
    pm.shutdown
  }
  
  def reload(startUuid:String){
    var counter =0
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis
    
    pm.pageOverSelect("occ", (guid, map) => {
      counter += 1
      if (!map.isEmpty) {
        //println())
        //add the new values to cassandra
        val newmap = getDataResource(map.get("dataResourceUid").get)
        if(newmap.size>0)
          pm.put(guid, "occ", newmap)
      }
        //debug counter
        if (counter % 1000 == 0) {
          finishTime = System.currentTimeMillis
          println(counter + " >> Last key : " + guid + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
          startTime = System.currentTimeMillis
        }
      true
    },startUuid,1000, "dataResourceUid")
  }

  def getDataResource(value:String):Map[String,String]={

    //attempt to locate it in the cache first
    if(cache.contains(value))
      return cache.get(value).get
    else{
      val newval = getDataResourceFromWS(value)
      cache += (value->newval)
      println(cache)
      return newval
    }

  }
  /**
   * Obtains the data resource information from the webservice. This method willneed to be used
   * in the DWCA loader. A user will supply an archive and a data resource uid the
   * WS will be queried once at the beginning. Eventually this web service will define the
   * DWCA fields that uniquely identify a record.
   */
  def getDataResourceFromWS(value:String):Map[String,String]={
   
    println("Calling web service for " + value)

    val wscontent = WebServiceLoader.getWSStringContent(AttributionDAO.collectoryURL+"/ws/dataResource/"+value+".json")
    
    val wsmap = Json.toMap(wscontent)

    val name = wsmap.getOrElse("name","").toString

    val hints =wsmap.getOrElse("taxonomyCoverageHints",null)
    val shints ={
                  if(hints != null){
                  val ahint = hints.asInstanceOf[java.util.ArrayList[Object]].toArray.map((o:Object)=> o.toString().replace("=",":").replace("{","").replace("}",""));
                  if(ahint.size >0)
                    Json.toJSON(ahint.asInstanceOf[Array[AnyRef]]);
                  else ""
                  }
                  else ""
              }
    //the hubMembership
     val hub = wsmap.getOrElse("hubMembership", null)
     val shub ={
                if(hub !=  null){
                  val ahub = hub.asInstanceOf[java.util.ArrayList[Object]].toArray.map((o:Object)=> (o.asInstanceOf[java.util.LinkedHashMap[Object,Object]]).get("uid").toString)
                  if(ahub.size >0)
                    Json.toJSON(ahub.asInstanceOf[Array[AnyRef]]);
                  else ""
                }
                else ""
              }

    //data Provider
    val dp = wsmap.getOrElse("provider", null).asInstanceOf[java.util.Map[String,String]]
    val dpname = if(dp != null) dp.get("name") else""
    val dpuid = if(dp != null) dp.get("uid") else ""

    Map("dataResourceName"->name, "dataProviderUid"->dpuid, "dataProviderName"->dpname, "dataHubUid"->shub, "taxonomicHints"->shints).filter(kv => kv._2.length>0)
    
  }


}
