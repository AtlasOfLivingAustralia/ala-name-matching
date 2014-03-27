package au.org.ala.biocache.poso

import scala.util.parsing.json.JSON
import scala.collection.immutable.Map
import au.org.ala.biocache.util.Json

/**
 * A POSO with nested POSOs
 */
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
        //NC The print statement below can be enabled for debug purposes but please don't check it back without commenting it out because it creates a lot of output when loading a DwcA
        //println(property.name + " : "+property.typeName + " : " + value)
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