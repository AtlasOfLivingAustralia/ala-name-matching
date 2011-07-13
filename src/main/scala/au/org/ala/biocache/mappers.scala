package au.org.ala.biocache

import collection.immutable.HashSet
import collection.mutable.ArrayBuffer
import au.org.ala.util.ReflectBean
import java.lang.reflect.Method
import org.apache.commons.lang.StringUtils

object FullRecordMapper {

    import ReflectBean._

    val entityName = "occ"
    val qualityAssertionColumn = "qualityAssertion"
    val userQualityAssertionColumn = "userQualityAssertion"
    val geospatialDecisionColumn = "geospatiallyKosher"
    val taxonomicDecisionColumn = "taxonomicallyKosher"
    val deletedColumn = "deleted"
    val geospatialQa = "loc"
    val taxonomicalQa = "class"

    //read in the object mappings using reflection
    val attributionDefn = loadDefn(classOf[Attribution])
    val occurrenceDefn = loadDefn(classOf[Occurrence])
    val classificationDefn = loadDefn(classOf[Classification])
    val locationDefn = loadDefn(classOf[Location])
    val eventDefn = loadDefn(classOf[Event])
    val identificationDefn = loadDefn(classOf[Identification])
    val measurementDefn = loadDefn(classOf[Measurement])
    val environmentalDefn = loadDefn(classOf[EnvironmentalLayers])
    val contextualDefn = loadDefn(classOf[ContextualLayers])


    //index definitions
    val occurrenceIndexDefn = loadDefn(classOf[OccurrenceIndex])
    //PROBABLY NOT THE BEST PLACE FOR THIS

    /**Retrieve the set of fields and their corresponding getter and setter methods for the supplied class */
    protected def loadDefn(theClass: java.lang.Class[_]): Map[String, (Method,Method)] = {
      val defn = scala.collection.mutable.Map[String, (Method,Method)]()
      //HashSet() ++ theClass.getDeclaredFields.map(_.getName).toList
      for(field<-theClass.getDeclaredFields){
        val name = field.getName
        val typ = field.getType
        try{
          val getter = theClass.getDeclaredMethod("get"+StringUtils.capitalize(name))
          val setter = theClass.getDeclaredMethod("set"+StringUtils.capitalize(name), typ)
          defn.put(name, (getter->setter))
        }
        catch{
          case e:Exception =>println("Not loading def : "+ name)
        }
      }
      defn.toMap

    }

    protected def fileToSet(filePath: String): Set[String] =
        scala.io.Source.fromURL(getClass.getResource(filePath), "utf-8").getLines.toList.map(_.trim).toSet

    /**
     * for each field in the definition, check if there is a value to write
     * Change to use the toMap method of a Mappable
     */
    def mapObjectToProperties(anObject: AnyRef): Map[String, String] = {
        var properties = scala.collection.mutable.Map[String, String]()
        if (anObject.isInstanceOf[Mappable]) {

            val map = anObject.asInstanceOf[Mappable].getMap
            map foreach {
                case (key, value) => properties.put(key, value)
            }

        } else {
            val defn = getDefn(anObject)
            for (field <- defn.keySet) {
                //val fieldValue = anObject.getter(field).asInstanceOf[String]
                //Use the cached version of the getter method
                val getter = defn.get(field).get.asInstanceOf[(Method,Method)]._1
                val fieldValue = getter.invoke(anObject)
                if (fieldValue != null) {
                    properties.put(field.toString, fieldValue.toString)
                }
            }
        }
        properties.toMap
    }

    /**
     * Set the property on the correct model object
     */
    def mapPropertiesToObject(anObject: AnyRef, map: Map[String, String]) {
        //TODO supplied properties will be less that properties in object this could be an optimisation
        val defn = getDefn(anObject)
        for (fieldName <- defn.keySet) {
            val fieldValue = map.get(fieldName)
            if (!fieldValue.isEmpty && !fieldValue.get.trim.isEmpty) {
                //anObject.setter(fieldName, fieldValue.get)
                //Use the cached version of the setter method 
                anObject.setter(defn.get(fieldName).get.asInstanceOf[(Method,Method)]._2, fieldValue.get)
            }
        }
    }

  /**
   * Sets the properties on the supplied object based on 2 maps
   * <ol>
   * <li>Map of source names to values</li>
   * <li>Map of source names to target values</li>
   * </ol>
   */
  def mapmapPropertiesToObject(anObject:AnyRef, valueMap:scala.collection.Map[String,Object], targetMap:scala.collection.Map[String,String]){
      val defn = getDefn(anObject)
      for(sourceName <- valueMap.keySet){
        //get the target name
        val targetName = targetMap.getOrElse(sourceName,"")
        if(targetName.length>0){
          //get the setter method
          val methods = defn.get(targetName)
          if(!methods.isEmpty){
            val setter = methods.get._2
            anObject.setter(setter, valueMap.get(sourceName).get)
          }
        }
      }
  }

    /**
     * Retrieve a object definition (simple ORM mapping)
     */
    def getDefn(anObject: Any): Map[String,(Method,Method)] = {
        anObject match {
            case l: Location => locationDefn
            case o: Occurrence => occurrenceDefn
            case e: Event => eventDefn
            case c: Classification => classificationDefn
            case a: Attribution => attributionDefn
            case i: Identification => identificationDefn
            case m: Measurement => measurementDefn
            case oi: OccurrenceIndex => occurrenceIndexDefn
            case el: EnvironmentalLayers =>environmentalDefn
            case cl: ContextualLayers => contextualDefn
            case _ => throw new RuntimeException("Unrecognised entity. No definition registered for: " + anObject)
        }
    }

    /**
     * Set the property on the correct model object
     */
    protected def setProperty(fullRecord: FullRecord, fieldName: String, fieldValue: String) {
        if (occurrenceDefn.contains(fieldName)) {
            //fullRecord.occurrence.setter(fieldName, fieldValue)
            fullRecord.occurrence.setter(occurrenceDefn.get(fieldName).get.asInstanceOf[(Method,Method)]._2, fieldValue)
        } else if (classificationDefn.contains(fieldName)) {
            //fullRecord.classification.setter(fieldName,fieldValue)
            fullRecord.classification.setter(classificationDefn.get(fieldName).get.asInstanceOf[(Method,Method)]._2, fieldValue)
        } else if (locationDefn.contains(fieldName)) {
            //fullRecord.location.setter(fieldName, fieldValue)
            fullRecord.location.setter(locationDefn.get(fieldName).get.asInstanceOf[(Method,Method)]._2, fieldValue)
        } else if (eventDefn.contains(fieldName)) {
            //fullRecord.event.setter(fieldName, fieldValue)
            fullRecord.event.setter(eventDefn.get(fieldName).get.asInstanceOf[(Method,Method)]._2, fieldValue)
        } else if (attributionDefn.contains(fieldName)) {
            //fullRecord.attribution.setter(fieldName, fieldValue)
            fullRecord.attribution.setter(attributionDefn.get(fieldName).get.asInstanceOf[(Method,Method)]._2, fieldValue)
        } else if (identificationDefn.contains(fieldName)) {
            //fullRecord.identification.setter(fieldName, fieldValue)
            fullRecord.identification.setter(identificationDefn.get(fieldName).get.asInstanceOf[(Method,Method)]._2, fieldValue)
        } else if (measurementDefn.contains(fieldName)) {
            //fullRecord.identification.setter(fieldName, fieldValue)
            fullRecord.measurement.setter(measurementDefn.get(fieldName).get.asInstanceOf[(Method,Method)]._2, fieldValue)
        } else if (environmentalDefn.contains(fieldName)){
            fullRecord.location.environmentalLayers.setter(environmentalDefn.get(fieldName).get.asInstanceOf[(Method,Method)]._2, fieldValue)
        } else if (contextualDefn.contains(fieldName)) {
            fullRecord.location.contextualLayers.setter(contextualDefn.get(fieldName).get.asInstanceOf[(Method,Method)]._2, fieldValue)
        } else if (isQualityAssertion(fieldName)) {
            //load the QA field names from the array
            if(fieldValue != "true" && fieldValue != "false"){
            val arr = Json.toListWithGeneric(fieldValue,classOf[java.lang.Integer])
            for(i <- 0 to arr.size-1)
              fullRecord.assertions = fullRecord.assertions :+ AssertionCodes.getByCode(arr(0)).get.getName
            
            }
//            if (fieldValue equals "true") {
//                fullRecord.assertions = fullRecord.assertions :+ removeQualityAssertionMarker(fieldName)
//            }
        }
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

        var fullRecord = new FullRecord
        fullRecord.rowKey = rowKey
        fullRecord.uuid = fields.getOrElse("uuid", "")
        fullRecord.lastLoadTime = fields.getOrElse("lastLoadTime","")
        var assertions = new ArrayBuffer[String]
        val columns = fields.keySet
        for (fieldName <- columns) {

            //ascertain which term should be associated with which object
            val fieldValue = fields.get(fieldName)
            //only set the value if it is no null or empty string
            if (!fieldValue.isEmpty && StringUtils.isNotEmpty(fieldValue.get)) {
                if (isQualityAssertion(fieldName)) {
                    setProperty(fullRecord, fieldName, fieldValue.get)
                } else if (taxonomicDecisionColumn.equals(fieldName)) {
                    fullRecord.taxonomicallyKosher = "true".equals(fieldValue.get)
                } else if (geospatialDecisionColumn.equals(fieldName)) {
                    fullRecord.geospatiallyKosher = "true".equals(fieldValue.get)
                } else if (deletedColumn.equals(fieldName)) {
                    fullRecord.deleted = "true".equals(fieldValue.get)
                } else if (isProcessedValue(fieldName) && version == Processed) {
                    setProperty(fullRecord, removeSuffix(fieldName), fieldValue.get)
                } else if (isConsensusValue(fieldName) && version == Consensus) {
                    setProperty(fullRecord, removeSuffix(fieldName), fieldValue.get)
                } else if (version == Raw) {
                    setProperty(fullRecord, fieldName, fieldValue.get)
                }
            }
        }
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