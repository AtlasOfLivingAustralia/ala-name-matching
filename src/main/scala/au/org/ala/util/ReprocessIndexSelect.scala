package au.org.ala.util
import au.org.ala.biocache._
import java.io.{BufferedOutputStream,FileOutputStream}
import java.io.File
/**
 * Reprocesses and Reindexes a select set of records.  The records will
 * be obtained through a query to the index.
 *
 */
object ReprocessIndexSelect {
    def main(args: Array[String]): Unit = {
        var query:Option[String]=None;
        var threads = 4
        val parser = new OptionParser("index records options") {
            opt("q", "query", "The query to run", { v:String => query = Some(v) })
            intOpt("t", "thread", "The number of threads to use", {v:Int => threads = v } )
            
        }

        if (parser.parse(args)) {
            if(!query.isEmpty){
                reprocessindex(query.get, threads)
            }
        }
    }

    def reprocessindex(query: String, threads: Int) {
        //get the list of rowKeys to be processed.
        val indexer = Config.getInstance(classOf[IndexDAO]).asInstanceOf[IndexDAO]

        val file = new File("rowkeys.out")
        val out = new BufferedOutputStream(new FileOutputStream(file));        
        indexer.writeRowKeysToStream(query,out)
        out.flush()
        out.close()
        ProcessWithActors.processRecords(threads, file)
        IndexRecords.indexList(file)

    }
}