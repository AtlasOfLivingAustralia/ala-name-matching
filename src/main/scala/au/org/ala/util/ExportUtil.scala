package au.org.ala.util

import au.org.ala.biocache.{Config, CassandraPersistenceManager}
import org.scale7.cassandra.pelops.{Bytes, Pelops}
import org.apache.cassandra.thrift.{ConsistencyLevel, InvalidRequestException, CfDef}
import scala.Array._

/**
 * Utility for exporting data from the biocache.
 */
object ExportUtil {

  def main(args:Array[String]){
      
    var fieldsToExport = List[String]()
    var fieldsRequired = List[String]()
    var entity = ""
    var maxRecords = Integer.MAX_VALUE
    
	val parser = new OptionParser("export") {
	  arg("<entity>", "the entity (column family in cassandra) to export from", {v: String => entity = v})
	  opt("c", "columns", "<column1 column2 ...>", "space separated list of columns to export", {columns:String => fieldsToExport = columns.split(" ").toList})
	  opt("r", "required-columns", "<column1 column2 ...>", "space separated required columns", {columns:String => fieldsRequired = columns.split(" ").toList})
	  intOpt("m", "max-records", "number of records to export", {v:Int => maxRecords = v } )
	}

    if (parser.parse(args)) {
        export(entity,fieldsToExport,fieldsRequired,maxRecords)
    }
  }
  
  def export(entity:String, fieldsToExport:List[String], fieldsRequired: List[String], maxRecords:Int) {
    val pm = Config.persistenceManager
    var counter = 0
    //page through and create the index
    pm.pageOverSelect(entity, (guid, map) => {
      if(fieldsRequired.forall(field => map.contains(field))){  
    	  exportRecord(fieldsToExport, guid, map)
      }
      counter += 1
      maxRecords > counter
    }, "", maxRecords, fieldsToExport:_*)
    //close db connections
    Pelops.shutdown
  }
  
  def exportRecord(fieldsToExport: List[String], guid: String, map: Map[String,String]) {
    print(guid)
    for(field <- fieldsToExport){
        print('\t')
        print(map.getOrElse(field, ""))
    }
    print('\n')
  }
}