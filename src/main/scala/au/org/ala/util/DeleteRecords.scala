/*
 * Deletes records from cassandra and the index.
 */

package au.org.ala.util

import au.org.ala.biocache._
import java.io.File
import java.io.BufferedOutputStream
import java.io.FileOutputStream

object DeleteRecords {

  val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
  val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]

  def main(args: Array[String]){
      
      var query:Option[String]=None
      var dr:Option[String]=None
      val parser = new OptionParser("delete records options") {
            opt("q", "query", "The query to run to obtain the records for deletion e.g. 'year:[2001 TO *]' or 'taxon_name:Macropus'",
              { v:String => query = Some(v) }
            )
            opt("dr", "resource", "The data resource to process", {v:String => dr = Some(v)})
        }
      if(parser.parse(args)){
          val deletor:Option[RecordDeletor] = {
              if(!query.isEmpty) Some(new QueryDelete(query.get))
              else if(!dr.isEmpty) Some(new DataResourceDelete(dr.get))
              else None
          }
          println("Starting delete " + query + " " + dr)
          if(!deletor.isEmpty){
              deletor.get.deleteFromPersistent
              deletor.get.deleteFromIndex
              deletor.get.close
          }
      }
  }
}

trait RecordDeletor {
    val pm = Config.persistenceManager
    val indexer = Config.indexDAO
    val occurrenceDAO = Config.occurrenceDAO
    def deleteFromPersistent
    def deleteFromIndex
    def close {
      pm.shutdown
      indexer.shutdown
    }
}

/**
 * A deletor that marks occurrence records, for a specific data resource, as deleted before 
 * removing them at a later time.
 *  
 */
class DataResourceVirtualDelete(dataResource:String) extends RecordDeletor{
    override def deleteFromPersistent {
        var count = 0
        val start = System.currentTimeMillis
        val startUuid = dataResource +"|"
        val endUuid = startUuid + "~"
        
        pm.pageOverSelect("occ", (guid,map)=>{
            occurrenceDAO.setDeleted(guid, true)
            count= count +1
            true
        }, startUuid, endUuid, 1000, "rowKey", "uuid")
        val finished = System.currentTimeMillis
      
      println("Marked " + count + " records as deleted in "  + (finished -start).toFloat / 60000f + " minutes.") 
    }

    override def deleteFromIndex = indexer.removeByQuery("data_resource_uid:" + dataResource)

    /**
     * Physically deletes all records where deleted=true in the persistence manager.
     */
    def physicallyDeleteMarkedRecords {
        var count = 0
        val start = System.currentTimeMillis
        val startUuid = dataResource +"|"
        val endUuid = startUuid + "~"
        pm.pageOverSelect("occ", (guid,map)=>{
            val delete = map.getOrElse(FullRecordMapper.deletedColumn, "false")
            if("true".equals(delete)){
                //pm.delete(guid, "occ")
                //use the occ DAO to delete so that the record is added to the dellog cf
                occurrenceDAO.delete(guid,false,true)
                count= count +1
            }
            true
        }, startUuid, endUuid, 1000, "rowKey", "uuid", FullRecordMapper.deletedColumn)
        val finished = System.currentTimeMillis
      
      println("Deleted " + count + " records in "  + (finished -start).toFloat / 60000f + " minutes.")
    }
}

class DataResourceDelete(dataResource:String) extends RecordDeletor{   
   
    override def deleteFromPersistent {
        //page over all the records for the data resource deleting them
        var count = 0
        val start = System.currentTimeMillis
        val startUuid = dataResource +"|"
        val endUuid = startUuid + "~"
        
        pm.pageOverSelect("occ", (guid,map)=>{
//            pm.delete(guid, "occ")
            //use the occ DAO to delete so that the record is added to the dellog cf
            occurrenceDAO.delete(guid,false,true)
            count= count +1
            true
        }, startUuid, endUuid, 1000, "rowKey", "uuid")
        val finished = System.currentTimeMillis
      
      println("Deleted " + count + " records in "  + (finished -start).toFloat / 60000f + " minutes.") 
    }
    override def deleteFromIndex {
        indexer.removeByQuery("data_resource_uid:" +dataResource)        
    }
}

class ListDelete(rowKeys:List[String]) extends RecordDeletor{
  override def deleteFromPersistent() ={
    rowKeys.foreach(rowKey=>{
      //pm.delete(rowKey, "occ")
      //use the occ DAO to delete so that the record is added to the dellog cf
      occurrenceDAO.delete(rowKey,false,true)
    })
  }
  override def deleteFromIndex {
   val query = "row_key:\"" + rowKeys.mkString("\" OR row_key:\"") +"\""
   indexer.removeByQuery(query)
  }
}

class QueryDelete(query :String) extends RecordDeletor{
    import FileHelper._
     override def deleteFromPersistent() ={
        val file = new File("/data/tmp/delrowkeys.out")
        var count =0
        val start = System.currentTimeMillis
        val out = new BufferedOutputStream(new FileOutputStream(file))        
        indexer.writeRowKeysToStream(query,out)
        out.flush
        out.close
        file.foreachLine(line=>{
            //pm.delete(line, "occ")
            //use the occ DAO to delete so that the record is added to the dellog cf
            occurrenceDAO.delete(line,false,true)
            count = count+1
        }) 
        val finished = System.currentTimeMillis
      
      println("Deleted " + count + " records in "  + (finished -start).toFloat / 60000f + " minutes.") 
     }
     override def deleteFromIndex = indexer.removeByQuery(query)
}