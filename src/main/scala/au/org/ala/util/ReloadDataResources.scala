package au.org.ala.util

/**
 * Reloads/processes an existing data resources records
 */
import au.org.ala.biocache._
import org.slf4j.LoggerFactory

object ReloadDataResources {
    val logger = LoggerFactory.getLogger("ReloadDataResources")
    def main(args: Array[String]): Unit = {
    var dataResource:String = ""
    var mark:Boolean=false
    var load:Boolean=false
    var remove:Boolean =false //remove from persistent data store
    var process:Boolean =false
    var index:Boolean = false
    
    val parser = new OptionParser("index records options") {
        arg("<data resource UID>", "The UID of the data resource to load", {v: String => dataResource = v})
        opt("all", "perform all phases" ,{mark=true; load=true; remove=true; process=true; index=true})
        opt("mark", "mark the occurences in the data store as deleted", {mark=true})        
        opt("load", "reload the records from the data resource", {load=true})
        opt("remove", "remove the occurences from the data store", {remove=true})
        opt("process", "reproces the records for the data resoure", {process=true})
        opt("index", "reindex the records for the data resoure", {index=true})
    }

    if(parser.parse(args)){
        
        logger.info("Reloading data resouce " + dataResource + " reprocessing: " + process + " reindexing: " + index + " removing from data store: " + remove)
        completeReload(dataResource, mark, load,process,index,remove)
        if(index){
            Config.indexDAO.shutdown
        }
     }
    
  }
   
    def completeReload(dataResourceUid:String, mark:Boolean=true, load:Boolean=true, process:Boolean=true, index:Boolean=true, remove:Boolean=true){
        
        val deletor = new DataResourceVirtualDelete(dataResourceUid)
        if(mark){
            //Step 1: Mark all records for dr as deleted
            deletor.deleteFromPersistent
        }
        if(load){
            //Step 2: Reload the records
            val l = new Loader
            l.load(dataResourceUid)
        }
        //Step 3: Reprocess records
        if(process){
            ProcessWithActors.processRecords(4, None, Some(dataResourceUid), true) //want to process on the not deleted records
        }
        if(index){
            //Step4: Remove current records from the index
            deletor.deleteFromIndex        
            //Step 5: Reindex dataResource
            IndexRecords.index(None, Some(dataResourceUid), false, false,checkDeleted=true)
        }
        if(remove){
            //Step 6: Remove "deleted" records from persistence.
            deletor.physicallyDeleteMarkedRecords
        }
    }
    

//   val cache = new scala.collection.mutable.HashMap[String, Map[String, String]]
//   val pm = Config.persistenceManager
//   def main(args: Array[String]): Unit = {
//    println("Reloading the data resource values...")
//    val start = if(args.length>0)args(0) else "dr354"
//    reload(start)
////    println(getDataResourceFromWS("dr376"))
////    println(getDataResourceFromWS("dr461"))
//    pm.shutdown
//  }
//  
//  def reload(startUuid:String){
//    var counter =0
//    var startTime = System.currentTimeMillis
//    var finishTime = System.currentTimeMillis
//    
//    pm.pageOverSelect("occ", (guid, map) => {
//      counter += 1
//      if (!map.isEmpty) {
//        val druid = map.getOrElse("dataResourceUid","")
//        val rowKey = map.getOrElse("rowKey", "")
//        if(druid == ""){
//          pm.put(rowKey, "occ", "dataResourceUid", rowKey.substring(0, rowKey.indexOf("|")))
////          println("Updated " + rowKey );
////          exit(0)
//        }
//        //println())
//        //add the new values to cassandra
////        val newmap = getDataResource(map.get("dataResourceUid").get)
////        if(newmap.size>0)
////          pm.put(guid, "occ", newmap)
//      }
//        //debug counter
//        if (counter % 1000 == 0) {
//          finishTime = System.currentTimeMillis
//          println(counter + " >> Last key : " + guid + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
//          startTime = System.currentTimeMillis
//        }
//      true
//    },startUuid, "",1000, "dataResourceUid","rowKey")
//  }
//
//  def getDataResource(value:String):Map[String,String]={
//
//    //attempt to locate it in the cache first
//    if(cache.contains(value))
//      return cache.get(value).get
//    else{
//      val newval = getDataResourceFromWS(value)
//      cache += (value->newval)
//      println(cache)
//      return newval
//    }
//
//  }
//  /**
//   * Obtains the data resource information from the webservice. This method willneed to be used
//   * in the DWCA loader. A user will supply an archive and a data resource uid the
//   * WS will be queried once at the beginning. Eventually this web service will define the
//   * DWCA fields that uniquely identify a record.
//   */
//  def getDataResourceFromWS(value:String):Map[String,String]={
//   
//    println("Calling web service for " + value)
//
//    val wscontent = WebServiceLoader.getWSStringContent(AttributionDAO.collectoryURL+"/ws/dataResource/"+value+".json")
//    
//    val wsmap = Json.toMap(wscontent)
//
//    val name = wsmap.getOrElse("name","").toString
//
//    val hints =wsmap.getOrElse("taxonomyCoverageHints",null)
//    val shints ={
//                  if(hints != null){
//                  val ahint = hints.asInstanceOf[java.util.ArrayList[Object]].toArray.map((o:Object)=> o.toString().replace("=",":").replace("{","").replace("}",""));
//                  if(ahint.size >0)
//                    Json.toJSON(ahint.asInstanceOf[Array[AnyRef]]);
//                  else ""
//                  }
//                  else ""
//              }
//    //the hubMembership
//     val hub = wsmap.getOrElse("hubMembership", null)
//     val shub ={
//                if(hub !=  null){
//                  val ahub = hub.asInstanceOf[java.util.ArrayList[Object]].toArray.map((o:Object)=> (o.asInstanceOf[java.util.LinkedHashMap[Object,Object]]).get("uid").toString)
//                  if(ahub.size >0)
//                    Json.toJSON(ahub.asInstanceOf[Array[AnyRef]]);
//                  else ""
//                }
//                else ""
//              }
//
//    //data Provider
//    val dp = wsmap.getOrElse("provider", null).asInstanceOf[java.util.Map[String,String]]
//    val dpname = if(dp != null) dp.get("name") else""
//    val dpuid = if(dp != null) dp.get("uid") else ""
//
//    Map("dataResourceName"->name, "dataProviderUid"->dpuid, "dataProviderName"->dpname, "dataHubUid"->shub, "taxonomicHints"->shints).filter(kv => kv._2.length>0)
//    
//  }
//

}
