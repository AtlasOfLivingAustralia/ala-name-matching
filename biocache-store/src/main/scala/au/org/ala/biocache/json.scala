package au.org.ala.biocache

import org.codehaus.jackson.map.{DeserializationConfig, ObjectMapper}
import org.codehaus.jackson.map.annotate.JsonSerialize
import scala.collection.JavaConversions
import org.codehaus.jackson.map.`type`.TypeFactory
import org.codehaus.jackson.map.{DeserializationConfig, ObjectMapper}
import org.codehaus.jackson.map.annotate.JsonSerialize
import java.util.ArrayList

object Json {
	
    import JavaConversions._
    import scalaj.collection.Imports._

    def toJSONWithGeneric[A](list:List[A]) : String = {
        val mapper = new ObjectMapper
        mapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL)
        mapper.writeValueAsString(list.asJava)
    }

    /**
     * Convert the supplied list to JSON
     */
    def toJSON(list:List[AnyRef]) : String = {
        val mapper = new ObjectMapper
        mapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL)
        mapper.writeValueAsString(list.asJava)
    }
    
    /**
     * Convert Array to JSON
     */
    def toJSON(arr:Array[AnyRef]) : String ={
      val mapper = new ObjectMapper
      mapper.writeValueAsString(arr)
    }

    /**
     * Converts a string to the supplied array type
     */
    def toArray(jsonString:String, theClass:java.lang.Class[AnyRef]) : Array[AnyRef] ={
      val mapper = new ObjectMapper
      val valueType = TypeFactory.arrayType(theClass)
      mapper.readValue[Array[AnyRef]](jsonString, valueType)
    }

    /**
     * Convert the supplied list from JSON
     */
    def toList(jsonString:String, theClass:java.lang.Class[AnyRef]) : List[AnyRef] = {
        var mapper = new ObjectMapper
        mapper.getDeserializationConfig().set(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val valueType = TypeFactory.collectionType(classOf[ArrayList[AnyRef]], theClass)
        var listOfObject = mapper.readValue[ArrayList[AnyRef]](jsonString, valueType)
        listOfObject.asScala[AnyRef].toList
    }

    /**
     * Convert the supplied list from JSON
     */
    def toListWithGeneric[A](jsonString:String,theClass:java.lang.Class[_]) : List[A] = {
        var mapper = new ObjectMapper
        mapper.getDeserializationConfig().set(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val valueType = TypeFactory.collectionType(classOf[ArrayList[_]], theClass)
        var listOfObject = mapper.readValue[ArrayList[_]](jsonString, valueType)
        listOfObject.asScala.toList.asInstanceOf[List[A]]
    }

   def toMap(jsonString:String): scala.collection.Map[String,Object]={
     try {
        val mapper = new ObjectMapper
        mapper.readValue(jsonString,classOf[java.util.Map[String,Object]]).asScala[String,Object];
     } catch {
        case e:Exception => Map()
     }
   }
}