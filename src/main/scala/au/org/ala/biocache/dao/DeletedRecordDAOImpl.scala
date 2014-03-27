package au.org.ala.biocache.dao

import scala.collection.mutable.ArrayBuffer
import au.org.ala.biocache.Config

/**
 * Created by mar759 on 17/02/2014.
 */
class DeletedRecordDAOImpl extends DeletedRecordDAO {

  /**
   * returns all the uuids that have been deleted since startDate inclusive.
   */
  def getUuidsForDeletedRecords(startDate:String) : Array[String] = {
    val recordBuffer = new ArrayBuffer[String]
    Config.persistenceManager.pageOverColumnRange("dellog",(rowKey,map) => {
      recordBuffer ++= map.values
      true
    },startDate,"",1000,"dr","dr~")

    recordBuffer.toArray
  }
}
