
package au.org.ala.util
import java.util.ArrayList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import au.org.ala.biocache._

/**
 * Index the Cassandra Records to conform to the fields
 * as defined in the schema.xml file.
 *
 * TODO: Need to handle the issues that are being recorded during the process phase.
 *
 * Natasha Carter
 *
 */
object IndexRecords {

  val logger = LoggerFactory.getLogger("IndexRecords")
  var indexer = SolrOccurrenceDAO
/**
 *
 * TODO: when arg[0] is a date the reindex process overrides the index values
 * of records that have been modified since the supplied date
 *
 */
  def main(args: Array[String]): Unit = {
    var counter = 0
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis
    var items = new ArrayList[OccurrenceIndex]()
    //delete the content of the index
    indexer.emptyIndex
    //page over all records and process
    OccurrenceDAO.pageOverAllVersions(versions => {
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
          logger.info(counter + " >> Last key : " + v(0).occurrence.uuid + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
          startTime = System.currentTimeMillis
          
        }
      }
      true

    })
    //index any remaining items before exiting
    indexer.index(items)
    indexer.finaliseIndex
    exit(0)
  }

}
