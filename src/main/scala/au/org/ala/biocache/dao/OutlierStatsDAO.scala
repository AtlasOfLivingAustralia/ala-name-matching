package au.org.ala.biocache.dao

import au.org.ala.biocache.outliers.{RecordJackKnifeStats, SampledRecord, JackKnifeStats}

/**
 * DAO for outlier stats
 */
trait OutlierStatsDAO {

  def getJackKnifeStatsFor(guid:String) : java.util.Map[String, JackKnifeStats]

  def getJackKnifeOutliersFor(guid:String) : java.util.List[(String,java.util.List[SampledRecord])]

  def getJackKnifeRecordDetailsFor(uuid:String) : java.util.List[RecordJackKnifeStats]
}
