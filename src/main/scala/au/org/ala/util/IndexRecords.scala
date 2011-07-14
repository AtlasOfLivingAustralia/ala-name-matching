
package au.org.ala.util
import java.util.ArrayList
import org.slf4j.LoggerFactory
import au.org.ala.biocache._

/**
 * Index the Cassandra Records to conform to the fields
 * as defined in the schema.xml file.
 *
 * @author Natasha Carter
 */
object IndexRecords {

  val logger = LoggerFactory.getLogger("IndexRecords")
  val indexer = Config.getInstance(classOf[IndexDAO]).asInstanceOf[IndexDAO]
  val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
  val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]

  def main(args: Array[String]): Unit = {
    
    var startUuid =""
    
    var dr:Option[String] = None
    var empty:Boolean =false
    val parser = new OptionParser("index records options") {
            opt("empty", "empty the index first", {empty=true})
            opt("s", "start","The record to start with", {v:String => startUuid = v})
            opt("dr", "resource", "The data resource to process", {v:String =>dr = Some(v)})
        }

    
     if(parser.parse(args)){
         //delete the content of the index
         if(empty){
            println("Emptying index")
            indexer.emptyIndex
         }
         val endUuid = if(dr.isEmpty)"" else dr.get +"|~"
        if(startUuid == "" && !dr.isEmpty) startUuid = dr.get +"|"
          println("Starting to index " + startUuid + " until " + endUuid)
          processMap(startUuid, endUuid)
          //index any remaining items before exiting
      //    indexer.index(items)
          indexer.finaliseIndex()

     }
  }
  
  def processMap(startUuid:String, endUuid:String)={
    var counter = 0
    val start = System.currentTimeMillis
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis
    var items = new ArrayList[OccurrenceIndex]()
    persistenceManager.pageOverAll("occ", (guid, map)=> {
        counter += 1

        indexer.indexFromMap(guid, map)
         
        if (counter % 1000 == 0) {
          finishTime = System.currentTimeMillis
          logger.info(counter + " >> Last key : " + guid + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
          startTime = System.currentTimeMillis

        }
        
        true
    }, startUuid, endUuid)

    finishTime = System.currentTimeMillis
    println("Total indexing time " + ((finishTime-start).toFloat)/1000f + " seconds")
  }

  def processFullRecords(){

    var counter = 0
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis
    var items = new ArrayList[OccurrenceIndex]()

     //page over all records and process
    occurrenceDAO.pageOverAllVersions(versions => {
      counter += 1
      if (!versions.isEmpty) {
    	val v = versions.get
    	items.add(indexer.getOccIndexModel(v).get);
        //debug counter
        if (counter % 1000 == 0) {
          //add the items to the configured indexer
          indexer.index(items);
          items.removeAll(items);
          finishTime = System.currentTimeMillis
          logger.info(counter + " >> Last key : " + v(0).uuid + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
          startTime = System.currentTimeMillis
        }
      }
      true
    })
  }
}
