package au.org.ala.biocache
import org.apache.commons.lang.StringUtils
import java.lang.reflect.Method
import scala.reflect.BeanProperty
import scala.collection.mutable.HashMap
import scala.collection.immutable.Map
import scala.util.parsing.json.JSON

/**
 * Holds the details of a property for a bean
 */
case class ModelProperty(name: String, typeName: String, getter: Method, setter: Method)

/**
 * A singleton that keeps a cache of POSO metadata.
 */
object ReflectionCache {

    var posoLookupCache = new HashMap[Class[_], Map[String, ModelProperty]]
    var compositeLookupCache = new HashMap[Class[_], Map[String, Method]]

    def getCompositeLookup(cposo: CompositePOSO): Map[String, Method] = {

        val result = compositeLookupCache.get(cposo.getClass)

        if (result.isEmpty) {
            val map = new HashMap[String, Method]()
            cposo.getClass.getDeclaredFields.map(field => {
                val name = field.getName;
                val typ = field.getType;
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

    override def setProperty(name: String, value: String) = lookup.get(name) match {
        case Some(property) => property.setter.invoke(this, value)
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
            case None => None //println("Set nested - Unrecognised property: " + name); None
        }
    }

    def setNestedProperty(name: String, value: String) {
        val getter = posoGetterLookup.get(name)
        getter match {
            case Some(method) => {
                val poso = method.invoke(this).asInstanceOf[POSO]
                poso.setProperty(name, value)
            }
            case None => //println("Set nested - Unrecognised property: " + name); None
        }
    }
}

trait POSO {

    protected val lookup = ReflectionCache.getPosoLookup(this)
    val propertyNames = lookup.keys

    def setProperty(name: String, value: String) = lookup.get(name) match {
        case Some(property) => {
            property.typeName match {
                case "java.lang.String" => property.setter.invoke(this, value)
                case "[Ljava.lang.String;" => {
                    try {
                        val array = Json.toArray(value, classOf[String].asInstanceOf[java.lang.Class[AnyRef]])
                        property.setter.invoke(this, array)
                    } catch {
                        case e: Exception => e.printStackTrace
                    }
                }
                case "int" => property.setter.invoke(this, Integer.parseInt(value).asInstanceOf[AnyRef])
                case "boolean" => property.setter.invoke(this, java.lang.Boolean.parseBoolean(value).asInstanceOf[AnyRef])
                case "scala.collection.immutable.Map"=>property.setter.invoke(this, JSON.parseFull(value).get.asInstanceOf[Map[String,String]])
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
                		if(array != null)
                			Json.toJSON(array.asInstanceOf[Array[AnyRef]])
                		else null
                        
                    } catch {
                        case e:Exception => e.printStackTrace; null
                    }
                  }
                case _=> null
                }
            }
            if(value != null){              
            	Some(value.toString)
            } else {
                None
            }
        }
        case None => None //println("Property not mapped " +name +", on " + this.getClass.getName); None;
    }

    def toMap: Map[String, String] = {

        val map = new HashMap[String, String]
        lookup.values.foreach(property => {

            val unparsed = property.getter.invoke(this)

            if (unparsed != null) {
                property.typeName match {
                    case "java.lang.String" => {
                        val value = unparsed.asInstanceOf[String]
                        if(value!=null && value!=""){
                        	map.put(property.name, value)
                        }
                    }
                    case "[Ljava.lang.String;" => {
                        try {
                            val value = unparsed.asInstanceOf[Array[AnyRef]]
                            val array = Json.toJSON(value)
                            map.put(property.name, array)
                        } catch {
                            case e: Exception => e.printStackTrace
                        }
                    }
                    case "int" =>{
                        val value = unparsed.asInstanceOf[Int]
                        map.put(property.name, value.toString)
                    }
                    case "boolean" =>{
                        val value = unparsed.asInstanceOf[Boolean]
                        map.put(property.name, value.toString)
                    }
                    case "scala.collection.immutable.Map" =>{
                        val value = unparsed.asInstanceOf[Map[String,String]]
                        val stringValue = value.map(pair => "\""+pair._1 +"\":\"" +pair._2 +"\"").mkString("{",",", "}")
                        map.put(property.name, stringValue)
                    }
                    case _ => throw new UnsupportedOperationException("Unsupported field type " + property.typeName)
                }
            }
        })
        map.toMap
    }
}
