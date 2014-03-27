package au.org.ala.biocache.dao

import com.google.inject.Inject
import scala.collection.mutable.ListBuffer
import org.apache.commons.lang3.StringUtils
import au.org.ala.biocache.util.BiocacheConversions
import au.org.ala.biocache.Config
import au.org.ala.biocache.model.ValidationRule
import au.org.ala.biocache.load.FullRecordMapper
import au.org.ala.biocache.persistence.PersistenceManager


class ValidationRuleDAOImpl extends ValidationRuleDAO {

  import BiocacheConversions._
  @Inject
  var persistenceManager: PersistenceManager = _

  def list : List[ValidationRule] = {
    val list = new ListBuffer[ValidationRule]
    persistenceManager.pageOverAll(entityName, (gui, map) => {
      val aq = new ValidationRule
      FullRecordMapper.mapPropertiesToObject(aq, map)
      list += aq
      false
    }, pageSize=10000)
    list.toList
  }

  def get(id:String):Option[ValidationRule]={
    val aq = new ValidationRule
    var map = persistenceManager.get(id, entityName)
    //check to see if the query assertion is being identified by uuid
    if(map.isEmpty){
      map = persistenceManager.getByIndex(id, entityName, "uuid")
    }

    if(map.isDefined){
      FullRecordMapper.mapPropertiesToObject(aq, map.get)
      Some(aq)
    } else {
      None
    }
  }

  //Because most of these will be accessed by index we will need to read each separately ( OR's are not supported)
  def get(ids:List[String]):List[ValidationRule] = {
    ids.map(id => get(id)).collect{case Some(aq) => aq}
  }
  
  def upsert(validationRule:ValidationRule){
    if(validationRule.uuid == null){
      //throw Exxception if apiKey and or id is null ?
      if(StringUtils.isNotBlank(validationRule.id)){
        validationRule.rowKey = validationRule.apiKey + "|" + validationRule.id
      } else {
        validationRule.rowKey = validationRule.apiKey + "|" + createUuid
      }
      validationRule.uuid = Config.validationRuleDAO.createOrRetrieveUuid(validationRule.rowKey)
    }
    val properties = FullRecordMapper.mapObjectToProperties(validationRule)
    persistenceManager.put(validationRule.rowKey,entityName,properties)
  }
  
  def delete(id:String, date:java.util.Date=null, physicallyRemove:Boolean=false){
    if(physicallyRemove){
      persistenceManager.delete(id, entityName)
    } else if(date != null){
      persistenceManager.put(id, entityName, "deletedDate", date)
    }
  }
  
  def pageOver(proc: (Option[ValidationRule] => Boolean),startKey:String="", endKey:String="", pageSize: Int = 1000){
    
    persistenceManager.pageOverAll(entityName, (guid, map) => {
      //retrieve all versions
      val aq = new ValidationRule()
      FullRecordMapper.mapPropertiesToObject(aq, map)
      //pass all version to the procedure, wrapped in the Option
      proc(Some(aq))
    },startKey,endKey, pageSize)
  }

  def createOrRetrieveUuid(uniqueID: String): String = {
    //look up by index
    val recordUUID = getUUIDForUniqueID(uniqueID)
    if (recordUUID.isEmpty) {
      val newUuid = createUuid
      //The uuid will be added when the record is inserted
      //persistenceManager.put(uniqueID, "dr", "uuid", newUuid)
      newUuid
    } else {
      recordUUID.get
    }
  }

  def getUUIDForUniqueID(uniqueID: String) = persistenceManager.get(uniqueID, entityName, "uuid")
}



