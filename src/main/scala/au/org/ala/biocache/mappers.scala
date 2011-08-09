package au.org.ala.biocache

import collection.immutable.HashSet
import collection.mutable.ArrayBuffer
import java.lang.reflect.Method
import org.apache.commons.lang.StringUtils
import scala.collection.mutable.HashMap

object FullRecordMapper {

    val entityName = "occ"
    val qualityAssertionColumn = "qualityAssertion"
    val userQualityAssertionColumn = "userQualityAssertion"
    val geospatialDecisionColumn = "geospatiallyKosher"
    val taxonomicDecisionColumn = "taxonomicallyKosher"
    val locationDeterminedColumn ="locationDetermined"
    val defaultValuesColumn = "defaultValuesUsed"    
    val alaModifiedColumn = "lastModifiedTime"
    val deletedColumn = "deleted"
    val geospatialQa = "loc"
    val taxonomicalQa = "class"
    val qaFields = Processors.processorMap.values.map(processor=> markAsQualityAssertion(processor.asInstanceOf[Processor].getName))
        
    /**
     * for each field in the definition, check if there is a value to write
     * Change to use the toMap method of a Mappable
     */
    def mapObjectToProperties(anObject: AnyRef, version:Version=Raw): Map[String, String] = {
        
        anObject match {
            //case m:Mappable => m.getMap
            case p:POSO => { p.toMap.map({case(key, value) => 
                (markNameBasedOnVersion(key,version) -> value)})
            }
            case _ => throw new Exception("Unrecognised object. Object isnt a Mappable or a POSO. Class : " + anObject.getClass.getName)
        }
    }
    /**
     * changes the name based on the version
     */
    def markNameBasedOnVersion(name:String, version:Version)={
        version match{
          
          case Processed => markAsProcessed(name)
          case Consensus => markAsConsensus(name)
          case _ => name
        }
    }

    /**
     * Set the property on the correct model object
     */
    def mapPropertiesToObject(anObject: POSO, map: Map[String, String]) =
        map.foreach({case (key,value) => anObject.setProperty(key,value)})

    /**
     * Sets the properties on the supplied object based on 2 maps
     * <ol>
     * <li>Map of source names to values</li>
     * <li>Map of source names to target values</li>
     * </ol>
     */
    def mapmapPropertiesToObject(poso:POSO, valueMap:scala.collection.Map[String,Object], targetMap:scala.collection.Map[String,String]){
      //for(sourceName <- valueMap.keySet){
      valueMap.keys.foreach(sourceName => {
        //get the target name
        val targetName = targetMap.getOrElse(sourceName,"")
        if(targetName != ""){
          //get the setter method
          poso.setProperty(targetName, valueMap.get(sourceName).get.toString)
        }
      })
    }
    
    /**
     * Create a record from a array of tuple properties
     */
    def createFullRecord(rowKey: String, fieldTuples: Array[(String, String)], version: Version): FullRecord = {
        val fieldMap = Map(fieldTuples map {
            s => (s._1, s._2)
        }: _*)
        createFullRecord(rowKey, fieldMap, version)
    }

    /**
     * Creates an FullRecord from the map of properties
     */
    def createFullRecord(rowKey: String, fields: Map[String, String], version: Version): FullRecord = {

        val fullRecord = new FullRecord
        fullRecord.rowKey = rowKey
        fullRecord.uuid = fields.getOrElse("uuid", "")
        fullRecord.lastModifiedTime = fields.getOrElse(markNameBasedOnVersion("lastModifiedTime",version),"")
        val assertions = new ArrayBuffer[String]
        val columns = fields.keySet

        fields.keySet.foreach( fieldName => {
            //ascertain which term should be associated with which object
            val fieldValue = fields.getOrElse(fieldName, "")
            //only set the value if it is no null or empty string
            if (fieldValue != "") {
                fieldName match {
                    case "qualityAssertion" => { }//ignore ?????
                    case it if isQualityAssertion(it) => {
                    	//load the QA field names from the array
                    	if(fieldValue != "true" && fieldValue != "false"){
                    		val arr = Json.toListWithGeneric(fieldValue,classOf[java.lang.Integer])
                    		for(i <- 0 to arr.size-1){
                    			fullRecord.assertions = fullRecord.assertions :+ AssertionCodes.getByCode(arr(i)).get.getName
                    		}
                    	}
                    }
                    case it if taxonomicDecisionColumn.equals(it) => fullRecord.taxonomicallyKosher = "true".equals(fieldValue) 
                    case it if geospatialDecisionColumn.equals(it) => fullRecord.geospatiallyKosher = "true".equals(fieldValue)
                    case it if defaultValuesColumn.equals(it) => fullRecord.defaultValuesUsed = "true".equals(fieldValue)
                    case it if locationDeterminedColumn.equals(it) => fullRecord.locationDetermined ="true".equals(fieldValue)
                    case it if deletedColumn.equals(it) => fullRecord.deleted = "true".equals(fieldValue)
                    case it if isProcessedValue(fieldName) && version == Processed => fullRecord.setProperty(removeSuffix(fieldName), fieldValue)
                    case it if version == Raw => fullRecord.setProperty(fieldName, fieldValue)
                    case _ => //throw new Exception("Unmapped property : " + fieldName)
                }
            }
        })
        fullRecord
    }

    /** FIXME */
    def fieldDelimiter = Config.persistenceManager.fieldDelimiter

    /**Remove the suffix indicating the version of the field */
    def removeSuffix(name: String): String = name.substring(0, name.length - 2)

    /**Is this a "processed" value? */
    def isProcessedValue(name: String): Boolean = name endsWith fieldDelimiter + "p"

    /**Is this a "consensus" value? */
    def isConsensusValue(name: String): Boolean = name endsWith fieldDelimiter + "c"

    /**Is this a "consensus" value? */
    def isQualityAssertion(name: String): Boolean = name endsWith fieldDelimiter + "qa"

    /**Add a suffix to this field name to indicate version type */
    def markAsProcessed(name: String): String = name + fieldDelimiter + "p"

    /**Add a suffix to this field name to indicate version type */
    def markAsConsensus(name: String): String = name + fieldDelimiter + "c"

    /**Add a suffix to this field name to indicate quality assertion field */
    def markAsQualityAssertion(name: String): String = name + fieldDelimiter + "qa"

    /**Remove the quality assertion marker */
    def removeQualityAssertionMarker(name: String): String = name.dropRight(3)
}