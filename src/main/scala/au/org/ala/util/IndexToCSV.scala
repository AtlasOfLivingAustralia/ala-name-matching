
package au.org.ala.util

import au.com.bytecode.opencsv.CSVWriter
import au.org.ala.biocache.IndexDAO
import au.org.ala.biocache.Config
import au.org.ala.biocache.PersistenceManager
import java.io.OutputStreamWriter
import org.slf4j.LoggerFactory

/**
 * Constructs a CSV file to create the index from.
 * @author Natasha Carter
 */
object IndexToCSV {
 val logger = LoggerFactory.getLogger("IndexToCSV")

  def main(args: Array[String]): Unit = {

    val indexer = Config.getInstance(classOf[IndexDAO]).asInstanceOf[IndexDAO]
    val persistentManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]

    val filename = if(args.size==1) args(0) else "/data/biocache/occurrence/occurrences.csv"
    var counter = 0
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis
    var items = List[Array[String]]()
    val csvWriter = new CSVWriter(new OutputStreamWriter(new java.io.FileOutputStream(filename)), ',', '"')
    //write the header
    csvWriter.writeNext(indexer.getHeaderValues)
    persistentManager.pageOverAll("occ", (guid, map)=> {
        counter += 1
        val item =indexer.getOccIndexModel(guid, map)
         csvWriter.writeNext(item)
         
        if (counter % 1000 == 0) {
          csvWriter.flush          
          finishTime = System.currentTimeMillis
          logger.info(counter + " >> Last key : " + guid + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
          //logger.info("Last item added:\n"+item.reduceLeft(_+","+_))
          startTime = System.currentTimeMillis
        }
        true
    }
  )
  csvWriter.flush
  csvWriter.close
  }
}
