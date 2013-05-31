package au.org.ala.biocache

import org.scalatest.FunSuite
import org.junit.Ignore
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import au.org.ala.util.DuplicateRecordDetails
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.reflect.BeanProperty
import com.fasterxml.jackson.annotation.JsonInclude.Include
@Ignore
class DAOLayerTest extends ConfigFunSuite {
  val occurrenceDAO = Config.occurrenceDAO
  val persistenceManager = Config.persistenceManager
  val rowKey = "test-rowKey"
  val uuid = "35b3ff3-test-uuid"

  test("Write and lookup occ record") {
    val record = new FullRecord(rowKey, uuid)
    record.classification.scientificName = "Test species"
    occurrenceDAO.updateOccurrence(rowKey, record, Versions.RAW)
    val newrecord = occurrenceDAO.getByRowKey(rowKey);
    //val newrecord = occurrenceDAO.getByUuid(uuid)

    expect(rowKey) {
      newrecord.get.getRowKey
    }
    expect(uuid) {
      newrecord.get.uuid
    }
  }

  test("Write Double value to processed record then read it out") {
    val record = new FullRecord(rowKey, uuid)
    val processedRecord = record.createNewProcessedRecord
    processedRecord.location.setProperty("distanceOutsideExpertRange", "1.23456")
    val retrievedDistance = processedRecord.location.getProperty("distanceOutsideExpertRange")
    expect("1.23456") {
      retrievedDistance.get
    }
  }

  test("Write map value to processed record then read it out") {
    val record = new FullRecord(rowKey, uuid)
    val processedRecord = record.createNewProcessedRecord
    processedRecord.occurrence.setProperty("originalSensitiveValues", "{\"a\":\"1\",\"b\":\"2\"}")
    val retrievedMap = processedRecord.occurrence.getProperty("originalSensitiveValues")
    expect("{\"a\":\"1\",\"b\":\"2\"}") {
      retrievedMap.get
    }
  }

  test("User Assertions addition and deletion") {
    val qa = QualityAssertion(AssertionCodes.GEOSPATIAL_ISSUE, true)
    qa.comment = "My comment"
    qa.userId = "Natasha.Carter@csiro.au"
    qa.userDisplayName = "Natasha Carter"
    occurrenceDAO.addUserAssertion(rowKey, qa)
    expect(1) {
      val userAssertions = occurrenceDAO.getUserAssertions(rowKey)
      userAssertions.size
    }
    val qaRowKey = rowKey + "|" + qa.getUserId + "|" + qa.getCode
    val qatest = persistenceManager.get(qaRowKey, "qa")
    println(qatest)

    expect(true) {
      !qatest.isEmpty
    }
    val qa2 = QualityAssertion(AssertionCodes.GEOSPATIAL_ISSUE, false)
    qa2.comment = "My comment"
    qa2.userId = "Natasha.Carter@csiro.au"
    qa2.userDisplayName = "Natasha Carter"
    occurrenceDAO.addUserAssertion(rowKey, qa2)
    expect(2) {
      val userAssertions = occurrenceDAO.getUserAssertions(rowKey)
      userAssertions.size
    }
    occurrenceDAO.deleteUserAssertion(rowKey, qa2.uuid)
    expect(1) {
      val userAssertions = occurrenceDAO.getUserAssertions(rowKey)
      userAssertions.size
    }
  }

  test("JSON parsing for Duplicates"){
    val json ="""{"rowKey":"dr376|CANB|CANB708196","uuid":"bd2d23c3-5fd8-43d1-9b54-e52776bdc78c","taxonConceptLsid":"urn:lsid:biodiversity.org.au:apni.taxon:373696","year":"1981","month":"11","day":"10","point1":"-28,153","point0_1":"-27.5,152.7","point0_01":"-27.52,152.75","point0_001":"-27.523,152.748","point0_0001":"-27.5233,152.7483","latLong":"-27.5233,152.7483","rawScientificName":"Erythrina numerosa A.R.Bean","collector":"[Bird, L.]","status":"R","druid":"dr376","duplicates":[{"rowKey":"dr376|MEL|MEL2332425A","uuid":"4587c30c-92f9-4d7d-bdb0-92f00122d673","taxonConceptLsid":"urn:lsid:biodiversity.org.au:apni.taxon:373696","year":"1981","month":"11","day":"10","point1":"-28,153","point0_1":"-27.5,152.7","point0_01":"-27.52,152.75","point0_001":"-27.523,152.748","point0_0001":"-27.5233,152.7483","latLong":"-27.5233,152.7483","rawScientificName":"Erythrina numerosa A.R.Bean","collector":"[Bird, L.]","status":"D1","druid":"dr376","dupTypes":[{"id":6},{"id":4}]}]}"""
    val mapper = new ObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper.setSerializationInclusion(Include.NON_NULL)
    val t = new OutlierResult("Natasha",0)
    println(mapper.writeValueAsString(t))
    val d = new DuplicateRecordDetails("drtest|dfghui|34", "sdf2-34-3", "urn:lsid:biodiversity.org.au:apni.taxon:373696", "1981","11","10","-27,152","-27.5,152.7","-27.52,152.74","-27.523,152.748","-27.5233,152.7483","-27.5233,152.7483","Erythrina numerosa A.R.Bean","[Bird, L.]","","")
    println(mapper.writeValueAsString(d))
    /*
    @BeanProperty var rowKey:String, @BeanProperty var uuid:String, @BeanProperty var taxonConceptLsid:String,
                    @BeanProperty var year:String, @BeanProperty var month:String, @BeanProperty var day:String,
                    @BeanProperty var point1:String, @BeanProperty var point0_1:String,
                    @BeanProperty var point0_01:String, @BeanProperty var point0_001:String,
                    @BeanProperty var point0_0001:String,@BeanProperty var latLong:String,
                    @BeanProperty var rawScientificName:String, @BeanProperty var collector:String,
                    @BeanProperty var oldStatus:String, @BeanProperty var oldDuplicateOf:String
     */
    //mapper.registerModule(new DefaultScalaModule())
    //mapper.readValue[DuplicateRecordDetails](json,classOf[DuplicateRecordDetails])
    mapper.readValue[OutlierResult]("""{"testUuid":"dr376|CANB|CANB708196","outlierForLayersCount":0}""", classOf[OutlierResult])

  }

//  class Test(@BeanProperty name:String, @BeanProperty value:String){
//    def this() = this(null,null)
//  }


}
