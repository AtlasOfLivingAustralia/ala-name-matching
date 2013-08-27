package au.org.ala.biocache

import org.codehaus.jackson.map.{DeserializationConfig, ObjectMapper}
import scala.collection.JavaConversions
import org.codehaus.jackson.map.`type`.TypeFactory
import org.codehaus.jackson.map.{DeserializationConfig, ObjectMapper}
import org.codehaus.jackson.map.annotate.JsonSerialize
import java.util.ArrayList

object Json {

  import JavaConversions._
  import scala.collection.JavaConverters._

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
   * Convert the supplied list to JSON
   */
  def toJSON(a:AnyRef) : String = {
    val mapper = new ObjectMapper
    mapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL)
    mapper.writeValueAsString(a)
  }

  /**
   * Convert the supplied list to JSON
   */
  def toJSON(a:Map[String,Any]) : String = {
    val mapper = new ObjectMapper
    mapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL)
    mapper.writeValueAsString(a.asJava)
  }

  /**
   * Convert the supplied list to JSON
   */
  def toJSONMap(a:Map[String,Any]) : String = {
    val mapper = new ObjectMapper
    mapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL)
    mapper.writeValueAsString(a.asJava)
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
  def toStringArray(jsonString:String) : Array[String] = {
    if(jsonString != null && jsonString != ""){
      val mapper = new ObjectMapper
      val valueType = TypeFactory.arrayType(classOf[java.lang.String])
      mapper.readValue[Array[String]](jsonString, valueType)
    } else {
      Array()
    }
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
      listOfObject.asScala.toList
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

  /**
   * Convert the supplied list from JSON
   */
  def toIntArray(jsonString:String) : Array[Int] = {
    if (jsonString =="" || jsonString =="[]") return Array()
    jsonString.replace("[","").replace("]","").split(",").map(x => x.toInt).toArray
  }

  def toMap(jsonString:String): scala.collection.Map[String,Object]={
   try {
     val mapper = new ObjectMapper
     mapper.readValue(jsonString,classOf[java.util.Map[String,Object]]).asScala
   } catch {
     case e:Exception => Map()
   }
  }

  def toStringMap(jsonString:String): scala.collection.Map[String,String] = {
   try {
     val mapper = new ObjectMapper
     mapper.readValue(jsonString,classOf[java.util.Map[String,String]]).asScala
   } catch {
     case e:Exception => Map()
   }
  }

  def toJavaMap(jsonString:String): java.util.Map[String,Object] = {
   try {
     val mapper = new ObjectMapper
     mapper.readValue(jsonString,classOf[java.util.Map[String,Object]])
   } catch {
     case e:Exception => new java.util.HashMap()
   }
  }

  def toJavaStringMap(jsonString:String): java.util.Map[String,String] = {
   try {
     val mapper = new ObjectMapper
     mapper.readValue(jsonString,classOf[java.util.Map[String,String]])
   } catch {
     case e:Exception => new java.util.HashMap()
   }
  }
}