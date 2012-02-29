package au.org.ala.util
import au.org.ala.biocache.Config

/**
 * Copies the data from one column to another.
 * 
 * Optionally deleting the original value
 */
object CopyDataNewColumn {
  
  val occurrenceDAO = Config.occurrenceDAO
  val persistenceManager = Config.persistenceManager

  def main(args : Array[String]) : Unit = {

    println("Starting...")
    
    var columnFamily:String = ""
    var start:Option[String] = None
    var delete = false
    var dr:Option[String] = None
    var source:String = ""
    var target:String =""    
//    var fileName = ""

    val parser = new OptionParser("copy column options") {
        arg("columnFamily","The columns family to copy from.",{v:String => columnFamily = v})
        arg("sourceColumn", "The column to copy from." ,{v:String => source = v})
        arg("targetColumn", "The column to copy from." ,{v:String => target = v})
        opt("s", "start","The record to start with", {v:String => start = Some(v)})
        opt("dr", "resource", "The data resource to process", {v:String =>dr = Some(v)})
        opt("delete","delete the source value",{delete=true})
    }
    
    if(parser.parse(args)){
	    println("Copying from " + source + " to " + target + " in " + columnFamily)    
	    val startUuid = if(start.isDefined) start.get else if(dr.isDefined) dr.get +"|" else ""
	    val endUuid = if(dr.isDefined) dr.get +"|~" else ""
	    var count =0; 
	    val originalStartTime = System.currentTimeMillis
	    var startTime = System.currentTimeMillis
	    var finishTime = System.currentTimeMillis
	    
	    persistenceManager.pageOverSelect(columnFamily, (guid,map)=>{
	            val sourceValue = map.get(source)
	            if(sourceValue.isDefined){
	              persistenceManager.put(guid, columnFamily, target, sourceValue.get)
	              if(delete)
	                persistenceManager.deleteColumns(guid, columnFamily, source)               
	            }
	            if (count % 1000 == 0) {
	            	finishTime = System.currentTimeMillis
	                println(count
	                + " >> Last key : " + guid
	                + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f)
	                + ", time taken for "+1000+" records: " + (finishTime - startTime).toFloat / 1000f
	                + ", total time: "+ (finishTime - originalStartTime).toFloat / 60000f +" minutes"
	        )
	        startTime = System.currentTimeMillis
	      }
	            count= count +1
	            true
	        }, startUuid, endUuid, 1000, "rowKey", "uuid", source)
    }
    //shutdown the persistence
    persistenceManager.shutdown
  }

}