package au.org.ala.biocache.dao

import org.slf4j.LoggerFactory
import com.google.inject.Inject
import au.org.ala.biocache.outliers.{RecordJackKnifeStats, SampledRecord, JackKnifeStats}
import com.fasterxml.jackson.databind.ObjectMapper
import au.org.ala.biocache.persistence.PersistenceManager

/**
 * Created by mar759 on 17/02/2014.
 */
class OutlierStatsDAOImpl extends OutlierStatsDAO {

  protected val logger = LoggerFactory.getLogger("OutlierStatsDAO")
  @Inject
  var persistenceManager: PersistenceManager = _

  def getJackKnifeStatsFor(guid:String) : java.util.Map[String, JackKnifeStats] = {

    logger.debug("Getting outlier stats for: " + guid)
    val mapper = new ObjectMapper
    //mapper.registerModule(DefaultScalaModule)
    val stringValue = persistenceManager.get(guid,"outliers", "jackKnifeStats").getOrElse("{}")

    logger.debug("Retrieved outlier stats for: " + stringValue)
    val obj = mapper.readValue(stringValue, classOf[java.util.Map[String, JackKnifeStats]])
    obj.asInstanceOf[java.util.Map[String, JackKnifeStats]]
  }

  def getJackKnifeOutliersFor(guid:String) : java.util.List[(String,java.util.List[SampledRecord])] = {

    logger.debug("Getting outlier stats for: " + guid)
    val mapper = new ObjectMapper
    //mapper.registerModule(DefaultScalaModule)
    val stringValue = persistenceManager.get(guid,"outliers", "jackKnifeOutliers").getOrElse("{}")

    logger.debug("Retrieved outlier stats for: " + stringValue)
    //val obj = mapper.readValue(stringValue, classOf[java.util.Map[String, Array[String]]])
    val obj = mapper.readValue(stringValue, classOf[java.util.List[(String,java.util.List[SampledRecord])]])
    //obj.asInstanceOf[java.util.Map[String, Array[String]]]
    obj.asInstanceOf[java.util.List[(String,java.util.List[SampledRecord])]]
  }

  def getJackKnifeRecordDetailsFor(uuid:String) : java.util.List[RecordJackKnifeStats] = {

    logger.debug("Getting outlier stats for record: " + uuid)
    val mapper = new ObjectMapper
    //mapper.registerModule(DefaultScalaModule)
    val stringValue = persistenceManager.get(uuid,"occ_outliers", "jackKnife").getOrElse("[]")

    logger.debug("Retrieved outlier stats for: " + stringValue)
    val obj = mapper.readValue(stringValue, classOf[java.util.List[RecordJackKnifeStats]])
    obj.asInstanceOf[java.util.List[RecordJackKnifeStats]]
  }
}
