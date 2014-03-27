package au.org.ala.biocache.dao

import au.org.ala.biocache.util.BiocacheConversions
import org.slf4j.LoggerFactory
import com.google.inject.Inject
import com.fasterxml.jackson.databind.ObjectMapper
import au.org.ala.biocache.persistence.PersistenceManager
import au.org.ala.biocache.model.DuplicateRecordDetails

class DuplicateDAOImpl extends DuplicateDAO {
  import BiocacheConversions._
  protected val logger = LoggerFactory.getLogger("DuplicateDAO")
  @Inject
  var persistenceManager: PersistenceManager = _

  val mapper = new ObjectMapper
  val lastRunRowKey = "DDLastRun"

  override def deleteObsoleteDuplicate(uuid:String){
    val duplicate = getDuplicateInfo(uuid)
    if(duplicate.isDefined){
      println("Deleting " + duplicate.get.getRowKey() + " - " + uuid)
      //now construct the row key for the "duplicates" column family
      val otherKey = duplicate.get.taxonConceptLsid+ "|" + duplicate.get.year + "|" + duplicate.get.month + "|" + duplicate.get.day
      persistenceManager.delete(uuid, "occ_duplicates")
      //now delete the column
      persistenceManager.deleteColumns(otherKey, "duplicates",uuid)

    }
  }

  override def getDuplicateInfo(uuid:String):Option[DuplicateRecordDetails]={
    val stringValue = persistenceManager.get(uuid, "occ_duplicates", "value")
    if(stringValue.isDefined){
      //println(stringValue.get.replaceAll("\"\"\",","###,").replaceAll("\"\"\"", "\"\\\\\"").replaceAll("\"\"","\\\\\"").replaceAll("###","\\\\\"\""))
      //handle """, at the end of attribute and """ at the beginning of attribute and "" in the attribute
      //FIXME - is this because of bad data at the DB level ?
      Some(mapper.readValue[DuplicateRecordDetails](stringValue.get.replaceAll("\"\"\",","###,").replaceAll("\"\"\"", "\"\\\\\"").replaceAll("\"\"","\\\\\"").replaceAll("###","\\\\\"\""), classOf[DuplicateRecordDetails]))
    } else {
      None
    }
  }
  /*
   * Returns the existing duplicates for the supplied species and date information.
   *
   * Will allow incremental checks for records that have changed...
   */
  override def getDuplicatesFor(lsid:String, year:String, month:String, day:String):List[DuplicateRecordDetails] ={
    def kvpMap=persistenceManager.get(lsid + "|" +year+"|" +month+"|"+day,"duplicates")
    if(kvpMap.isDefined){
      def buf = new scala.collection.mutable.ArrayBuffer[DuplicateRecordDetails]()
      kvpMap.get.foreach{case (key,value)=>{
        buf +=  mapper.readValue[DuplicateRecordDetails](value, classOf[DuplicateRecordDetails])
      }}
      buf.toList
    } else {
      List()
    }
  }

  /**
   * Returns the last time that the duplication detection was run.
   */
  override def getLastDuplicationRun():Option[String] ={
    persistenceManager.get(lastRunRowKey, "duplicates", lastRunRowKey)
  }
  /**
   * Updates the last duplication detection run with the supplied date.
   */
  override def setLastDuplicationRun(date:java.util.Date) {
    persistenceManager.put(lastRunRowKey, "duplicates", lastRunRowKey, date)
  }
}
