package au.org.ala.util

import au.org.ala.biocache.{Config, CassandraPersistenceManager}
import org.scale7.cassandra.pelops.{Bytes, Pelops}
import org.apache.cassandra.thrift.{ConsistencyLevel, InvalidRequestException, CfDef}
import scala.Array._
import au.com.bytecode.opencsv.CSVWriter
import java.io.PrintWriter

/**
 * Utility for exporting data from the biocache.
 */
object ExportUtil {

  def main(args:Array[String]){
      
    var fieldsToExport = List[String]()
    var fieldsRequired = List[String]()
    var entity = ""
    var maxRecords = Integer.MAX_VALUE
    //var filePath:Option[String] = None
    
	val parser = new OptionParser("export") {
	  arg("<entity>", "the entity (column family in cassandra) to export from", {v: String => entity = v})
	  opt("c", "columns", "<column1 column2 ...>", "space separated list of columns to export", {columns:String => fieldsToExport = columns.split(" ").toList})
	  opt("r", "required-columns", "<column1 column2 ...>", "space separated required columns", {columns:String => fieldsRequired = columns.split(" ").toList})
	  intOpt("m", "max-records", "number of records to export", {v:Int => maxRecords = v } )
	}

    if (parser.parse(args)) {
        
      val outWriter = {
          new PrintWriter(System.out)
      }
        
      val writer = new CSVWriter(outWriter, '\t', '"')
      export(writer, entity,fieldsToExport,fieldsRequired,maxRecords)
      writer.flush
      writer.close
    }
  }
  
  def export(writer:CSVWriter, entity:String, fieldsToExport:List[String], fieldsRequired: List[String], maxRecords:Int) {
    val pm = Config.persistenceManager
    var counter = 0
    //page through and create the index
    pm.pageOverSelect(entity, (guid, map) => {
      if(fieldsRequired.forall(field => map.contains(field))){  
    	  exportRecord(writer, fieldsToExport, guid, map)
      }
      counter += 1
      maxRecords > counter
    }, "", 1000, fieldsToExport:_*)
  }
  
  def exportRecord(writer:CSVWriter, fieldsToExport: List[String], guid: String, map: Map[String,String]) {
    val line = Array(guid) ++ (for(field <- fieldsToExport) yield map.getOrElse(field, ""))
    writer.writeNext(line)
  }
}