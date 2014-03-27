package au.org.ala.biocache.dao

/**
 * DAO for deleted records
 */
trait DeletedRecordDAO {

  /**
   * @param startDate must be in the form yyyy-MM-dd
   */
  def getUuidsForDeletedRecords(startDate:String) : Array[String]
}
