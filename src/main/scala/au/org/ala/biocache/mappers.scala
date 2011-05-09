package au.org.ala.biocache

import collection.immutable.HashSet
import collection.mutable.ArrayBuffer
import au.org.ala.util.ReflectBean

object FullRecordMapper {

    import ReflectBean._

    val entityName = "occ"
    val qualityAssertionColumn = "qualityAssertion"
    val userQualityAssertionColumn = "userQualityAssertion"
    val geospatialDecisionColumn = "geospatiallyKosher"
    val taxonomicDecisionColumn = "taxonomicallyKosher"
    val deletedColumn = "deleted"

    //read in the object mappings using reflection
    val attributionDefn = loadDefn(classOf[Attribution])
    val occurrenceDefn = loadDefn(classOf[Occurrence])
    val classificationDefn = loadDefn(classOf[Classification])
    val locationDefn = loadDefn(classOf[Location])
    val eventDefn = loadDefn(classOf[Event])
    val identificationDefn = loadDefn(classOf[Identification])
    val measurementDefn = loadDefn(classOf[Measurement])

    //index definitions
    val occurrenceIndexDefn = loadDefn(classOf[OccurrenceIndex])
    //PROBABLY NOT THE BEST PLACE FOR THIS

    /**Retrieve the set of fields for the supplied class */
    protected def loadDefn(theClass: java.lang.Class[_]): Set[String] = {
        HashSet() ++ theClass.getDeclaredFields.map(_.getName).toList
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
            for (field <- defn) {
                val fieldValue = anObject.getter(field).asInstanceOf[String]
                if (fieldValue != null && !fieldValue.isEmpty) {
                    properties.put(field, fieldValue)
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
        for (fieldName <- defn) {
            val fieldValue = map.get(fieldName)
            if (!fieldValue.isEmpty && !fieldValue.get.trim.isEmpty) {
                anObject.setter(fieldName, fieldValue.get)
            }
        }
    }

    /**
     * Retrieve a object definition (simple ORM mapping)
     */
    def getDefn(anObject: Any): Set[String] = {
        anObject match {
            case l: Location => locationDefn
            case o: Occurrence => occurrenceDefn
            case e: Event => eventDefn
            case c: Classification => classificationDefn
            case a: Attribution => attributionDefn
            case i: Identification => identificationDefn
            case m: Measurement => measurementDefn
            case oi: OccurrenceIndex => occurrenceIndexDefn
            case _ => throw new RuntimeException("Unrecognised entity. No definition registered for: " + anObject)
        }
    }

    /**
     * Set the property on the correct model object
     */
    protected def setProperty(fullRecord: FullRecord, fieldName: String, fieldValue: String) {
        if (occurrenceDefn.contains(fieldName)) {
            fullRecord.occurrence.setter(fieldName, fieldValue)
        } else if (classificationDefn.contains(fieldName)) {
            fullRecord.classification.setter(fieldName, fieldValue)
        } else if (locationDefn.contains(fieldName)) {
            fullRecord.location.setter(fieldName, fieldValue)
        } else if (eventDefn.contains(fieldName)) {
            fullRecord.event.setter(fieldName, fieldValue)
        } else if (attributionDefn.contains(fieldName)) {
            fullRecord.attribution.setter(fieldName, fieldValue)
        } else if (identificationDefn.contains(fieldName)) {
            fullRecord.identification.setter(fieldName, fieldValue)
        } else if (measurementDefn.contains(fieldName)) {
            fullRecord.measurement.setter(fieldName, fieldValue)
        } else if (isQualityAssertion(fieldName)) {
            if (fieldValue equals "true") {
                fullRecord.assertions = fullRecord.assertions :+ removeQualityAssertionMarker(fieldName)
            }
        }
    }

    /**
     * Create a record from a array of tuple properties
     */
    def createFullRecord(uuid: String, fieldTuples: Array[(String, String)], version: Version): FullRecord = {
        val fieldMap = Map(fieldTuples map {
            s => (s._1, s._2)
        }: _*)
        createFullRecord(uuid, fieldMap, version)
    }

    /**
     * Creates an FullRecord from the map of properties
     */
    def createFullRecord(uuid: String, fields: Map[String, String], version: Version): FullRecord = {

        var fullRecord = new FullRecord
        fullRecord.uuid = uuid
        var assertions = new ArrayBuffer[String]
        val columns = fields.keySet
        for (fieldName <- columns) {

            //ascertain which term should be associated with which object
            val fieldValue = fields.get(fieldName)
            if (!fieldValue.isEmpty) {
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