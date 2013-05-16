package au.org.ala.biocache

import org.apache.commons.lang.StringUtils
import java.lang.reflect.Method
import scala.reflect.BeanProperty
import scala.collection.mutable.HashMap
import scala.collection.immutable.Map
import scala.util.parsing.json.JSON
import org.codehaus.jackson.annotate.JsonIgnore
import org.slf4j.LoggerFactory

/**
 * Holds the details of a property for a bean
 */
case class ModelProperty(name: String, typeName: String, getter: Method, setter: Method)

/**
 * A singleton that keeps a cache of POSO reflection metadata.
 */
object ReflectionCache {

  var posoLookupCache = new HashMap[Class[_], Map[String, ModelProperty]]
  var compositeLookupCache = new HashMap[Class[_], Map[String, Method]]

  def getCompositeLookup(cposo: CompositePOSO): Map[String, Method] = {

    val result = compositeLookupCache.get(cposo.getClass)

    if (result.isEmpty) {
      val map = new HashMap[String, Method]()
      cposo.getClass.getDeclaredFields.map(field => {
        val name = field.getName
        try {
          val getter = cposo.getClass.getDeclaredMethod("get" + StringUtils.capitalize(name));
          val isAPoso = !(getter.getReturnType.getInterfaces.forall(i => i == classOf[POSO]))
          if (isAPoso) {
            val poso = getter.invoke(cposo).asInstanceOf[POSO]
            poso.propertyNames.foreach(name => map += (name -> getter))
          }
        } catch {
          case e: Exception =>
        }
      })
      val fieldMap = map.toMap
      compositeLookupCache.put(cposo.getClass, fieldMap)
      fieldMap
    } else {
      result.get
    }
  }

  def getPosoLookup(poso: POSO): Map[String, ModelProperty] = {

    val result = posoLookupCache.get(poso.getClass)

    if (result.isEmpty) {
      val posoLookupMap = poso.getClass.getDeclaredFields.map(field => {
        val name = field.getName
        try {
          val getter = poso.getClass.getDeclaredMethod("get" + StringUtils.capitalize(name))
          val setter = poso.getClass.getDeclaredMethod("set" + StringUtils.capitalize(name), field.getType)
          Some((name -> ModelProperty(name, field.getType.getName, getter, setter)))
        } catch {
          case e: Exception => None
        }
      }).filter(x => !x.isEmpty).map(y => y.get).toMap

      posoLookupCache.put(poso.getClass, posoLookupMap)
      posoLookupMap
    } else {
      result.get
    }
  }
}

trait CompositePOSO extends POSO {

  val posoGetterLookup = ReflectionCache.getCompositeLookup(this)
  val nestedProperties = posoGetterLookup.keys

  override def hasProperty(name: String) = (!lookup.get(name).isEmpty || !posoGetterLookup.get(name).isEmpty)

  def hasNestedProperty(name: String) = !posoGetterLookup.get(name).isEmpty

  override def getPropertyNames: List[String] = nestedProperties.toList

  override def setProperty(name: String, value: String) = lookup.get(name) match {

    case Some(property) => {
      //println(name + " :  " + property.typeName + " : " +  value)
      if (property.typeName == "scala.collection.immutable.Map") {
        val jsonOption = JSON.parseFull(value)
        if (!jsonOption.isEmpty) {
          try {
            property.setter.invoke(this, jsonOption.get.asInstanceOf[Map[String, String]])
          } catch {
            case e: Exception => logger.error(e.getMessage, e)
          }
        }
      } else if (property.typeName == "[Ljava.lang.String;") {
        val jsonOption = JSON.parseFull(value)
        if (!jsonOption.isEmpty && jsonOption.get.isInstanceOf[Array[String]]) {
          try {
            val stringArray = jsonOption.get.asInstanceOf[Array[String]]
            if (!stringArray.isEmpty) {
              property.setter.invoke(this, jsonOption.get.asInstanceOf[Array[String]])
            }
          } catch {
            case e: Exception => logger.error(e.getMessage, e)
          }
        }
      } else if (property.typeName == "java.util.Map") {
        val stringMap = Json.toJavaStringMap(value)
        if (!stringMap.isEmpty) {
          try {
            property.setter.invoke(this, stringMap)
          } catch {
            case e: Exception => logger.error(e.getMessage, e)
          }
        }
      } else {
        //println(property.name + " : "+property.typeName)
        property.setter.invoke(this, value)
      }
    }
    case None => setNestedProperty(name, value)
  }

  override def getProperty(name: String): Option[String] = lookup.get(name) match {
    case Some(property) => Some(property.getter.invoke(this).toString)
    case None => getNestedProperty(name)
  }

  def getNestedProperty(name: String): Option[String] = {
    val getter = posoGetterLookup.get(name)
    getter match {
      case Some(method) => {
        val poso = method.invoke(this).asInstanceOf[POSO]
        poso.getProperty(name)
      }
      case None => None
    }
  }

  def setNestedProperty(name: String, value: String) {
    val getter = posoGetterLookup.get(name)
    getter match {
      case Some(method) => {
        val poso = method.invoke(this).asInstanceOf[POSO]
        poso.setProperty(name, value)
      }
      case None => //do nothing
    }
  }
}

trait POSO {

  import scala.collection.JavaConversions._
  import BiocacheConversions._
  val logger = LoggerFactory.getLogger("POSO")
  protected val lookup = ReflectionCache.getPosoLookup(this)
  val propertyNames = lookup.keys

  def hasProperty(name: String) = !lookup.get(name).isEmpty

  def setProperty(name: String, value: String) = lookup.get(name) match {
    case Some(property) => {
      property.typeName match {
        case "java.lang.String" => property.setter.invoke(this, value)
        case "[Ljava.lang.String;" => {
          try {
            val array = Json.toArray(value, classOf[String].asInstanceOf[java.lang.Class[AnyRef]])
            property.setter.invoke(this, array)
          } catch {
            case e: Exception => logger.error(e.getMessage, e)
          }
        }
        case "int" => property.setter.invoke(this, Integer.parseInt(value).asInstanceOf[AnyRef])
        case "double" => property.setter.invoke(this, java.lang.Double.parseDouble(value).asInstanceOf[AnyRef])
        case "boolean" => property.setter.invoke(this, java.lang.Boolean.parseBoolean(value).asInstanceOf[AnyRef])
        case "scala.collection.immutable.Map" => {
          try {
            val fromJson = JSON.parseFull(value)
            if (fromJson.isDefined && !fromJson.isEmpty)
              property.setter.invoke(this, fromJson.get.asInstanceOf[Map[String, String]])
          }
          catch {
            case e: Exception => logger.warn("Unable to set POSO map property. " + e.getMessage)
          }
        }
        case "java.util.Map" => property.setter.invoke(this, Json.toJavaStringMap(value))
        case "java.util.Date" =>{ 
          def date = DateParser.parseStringToDate(value)
          if(date.isDefined)
            property.setter.invoke(this, date.get)        
        }
        case _ => println("Unhandled data type: " + property.typeName)
      }
    }
    case None => {} //println("Property not mapped: " +name +", on " + this.getClass.getName)
  }

  def getProperty(name: String): Option[String] = lookup.get(name) match {

    case Some(property) => {
      val value = {
        property.typeName match {
          case "java.lang.String" => property.getter.invoke(this)
          case "[Ljava.lang.String;" => {
            try {
              val array = property.getter.invoke(this)
              if (array != null)
                Json.toJSON(array.asInstanceOf[Array[AnyRef]])
              else
                null
            } catch {
              case e: Exception => logger.error(e.getMessage, e); null
            }
          }
          case "int" => property.getter.invoke(this)
          case "double" => property.getter.invoke(this)
          case "boolean" => property.getter.invoke(this)
          case "scala.collection.immutable.Map" => {
            try {
              val map = property.getter.invoke(this)
              if(map != null)
                Json.toJSONMap(map.asInstanceOf[Map[String,Any]])
              else None
            }
            catch {
              case e: Exception => logger.error(e.getMessage, e); null
            }
          }
          case "java.util.Map" => {
            try {
              val javaMap = property.setter.invoke(this)
              if (javaMap != null) {
                Json.toJSONMap(javaMap.asInstanceOf[Map[String, Any]])
              } else {
                null;
              }
            } catch {
              case e: Exception => logger.error(e.getMessage, e); null
            }
          }
          case _ => null
        }
      }
      if (value != null) {
        Some(value.toString)
      } else {
        None
      }
    }
    case None => None //println("Property not mapped " +name +", on " + this.getClass.getName); None;
  }

  @JsonIgnore
  def getPropertyNames: List[String] = lookup.values.map(v => v.name).toList

  def toMap: Map[String, String] = {

    val map = new HashMap[String, String]
    lookup.values.foreach(property => {

      //println("************* POSO.toMap field name: " + property.name)
      val unparsed = property.getter.invoke(this)

      if (unparsed != null) {
        property.typeName match {
          case "java.lang.String" => {
            val value = unparsed.asInstanceOf[String]
            if (value != null && value != "") {
              map.put(property.name, value)
            }
          }
          case "[Ljava.lang.String;" => {
            try {
              val value = unparsed.asInstanceOf[Array[AnyRef]]
              if (value.length > 0) {
                val array = Json.toJSON(value)
                map.put(property.name, array)
              }
            } catch {
              case e: Exception => logger.error(e.getMessage, e)
            }
          }
          case "int" => {
            val value = unparsed.asInstanceOf[Int]
            map.put(property.name, value.toString)
          }
          case "double" => {
            val value = unparsed.asInstanceOf[Double]
            map.put(property.name, value.toString)
          }
          case "boolean" => {
            val value = unparsed.asInstanceOf[Boolean]
            map.put(property.name, value.toString)
          }
          case "scala.collection.immutable.Map" => {
            val value = unparsed.asInstanceOf[Map[String, String]]
            if (!value.isEmpty) {
              val stringValue = Json.toJSON(value)
              map.put(property.name, stringValue)
            }
          }
          case "java.util.Map" => {
            //val value = unparsed.asInstanceOf[Map[String,String]]
            val value = unparsed.asInstanceOf[java.util.Map[String, String]]
            if (!value.isEmpty) {
              val stringValue = Json.toJSON(value)
              map.put(property.name, stringValue)
            }
          }
          case "java.util.Date" => {
            map.put(property.name, unparsed.asInstanceOf[java.util.Date])
          }
          case _ => {
            if (unparsed.isInstanceOf[POSO]) {
              map ++ unparsed.asInstanceOf[POSO].toMap
            } else {
              throw new UnsupportedOperationException("Unsupported field type " + property.typeName)
            }
          }
        }
      }
    })
    map.toMap
  }
}
