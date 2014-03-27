package au.org.ala.biocache.dao

import au.org.ala.biocache.model.DuplicateRecordDetails

/**
 * DAO for duplicate records
 */
trait DuplicateDAO {

  def getDuplicateInfo(uuid:String) : Option[DuplicateRecordDetails]

  def getDuplicatesFor(lsid:String, year:String, month:String, day:String):List[DuplicateRecordDetails]

  def getLastDuplicationRun():Option[String]

  def setLastDuplicationRun(date:java.util.Date)

  def deleteObsoleteDuplicate(uuid:String)
}
