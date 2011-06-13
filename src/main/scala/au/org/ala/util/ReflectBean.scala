package au.org.ala.util

import au.org.ala.biocache.CassandraPersistenceManager
import org.apache.commons.lang.time.DateUtils
import au.org.ala.biocache.Json
import java.lang.reflect.Method
import org.apache.commons.lang.StringUtils

/**
 * A class that provided java bean style functionality for classes.
 */
class ReflectBean(ref: AnyRef)  {

  def fieldNameCheck(fieldName:String) = if(fieldName=="class") "classs" else fieldName

  def getter(name: String): Any = {
    var fieldName = fieldNameCheck(name)
    ref.getClass.getMethods.find(_.getName == fieldName).get.invoke(ref)
  }

  /**
   * Returns the result wrapped in an option.
   */
  def getterWithOption(name: String): Option[Any] = {
    var fieldName = fieldNameCheck(name)
    try {
        val getter = {
          if(fieldName.startsWith("get")){
             fieldName
          } else {
             "get" + fieldName
          }
        }
        val result = ref.getClass.getMethods.find(_.getName equalsIgnoreCase getter).get.invoke(ref)
        if(result!=null){
            Some(result)
        } else {
            None
        }
    } catch {
        case e:Exception => None
    }
  }


  def setField(name:String, value:Any): Unit = {
   ref.getClass.getField(name).set(this,value)
  }
  /**
   * Runs the supplied setter method with the supplied value.
   *
   * Conversion to the correct data type will be performed.
   */
  def setter(method: Method, value:Any): Unit ={
//    try{
    if(method != null){
      val typ = method.getParameterTypes()(0)
      var v2 = value.asInstanceOf[AnyRef]

      typ.getName  match{
        case "java.lang.Integer" => v2 = any2Int(v2)
        case "java.lang.Double" => v2 = any2Double(v2)
        case "java.util.Date" => v2 = any2Date(v2)
        case "[Ljava.lang.String;"  => {
            //NC This feels like a hack.
            v2.getClass().getName match{
              case "java.lang.String" =>v2 = try{Json.toArray(v2.asInstanceOf[String], classOf[String].asInstanceOf[java.lang.Class[AnyRef] ])}
              //case "java.util.ArrayList" => v2 = v2.asInstanceOf[java.util.ArrayList[String]].toArray.map(( o: Object ) => o.toString.replaceAll("=",":") )
              case _=>
            }
          }
        case _ =>
      }
      //field.set(ref, v2)
      method.invoke(ref, v2 )

    }
//    }
//    catch{
//      case e:Exception=> println("Unable to set value " + value);e.printStackTrace();
//    }
  }

  def setter(name: String, value:Any): Unit = {
    var fieldName = fieldNameCheck(name)
    val method = ref.getClass.getMethods.find(_.getName equalsIgnoreCase fieldName + "_$eq")
    
    if(!method.isEmpty){
      val typ = method.get.getParameterTypes()(0)
      var v2 = value.asInstanceOf[AnyRef]

      typ.getName  match{
        case "java.lang.Integer" => v2 = any2Int(v2)
        case "java.lang.Double" => v2 = any2Double(v2)
        case "java.util.Date" => v2 = any2Date(v2)
        case "[Ljava.lang.String;"  => {
            //NC This feels like a hack. 
            v2.getClass().getName match{
              case "java.lang.String" =>v2 = Json.toArray(v2.asInstanceOf[String], classOf[String].asInstanceOf[java.lang.Class[AnyRef] ])
             // case "java.util.ArrayList" => v2 = v2.asInstanceOf[java.util.ArrayList[String]].toArray.map(( o: Object ) => o.toString.replaceAll("=",":") )
              case _=>
            }
          }
        case _ => 
      }
      try{
      method.get.invoke(ref, v2 )
      }
      catch{
        case e:Exception => println("Unable to setter " + name + ":"+value+":" + v2.getClass.toString)
      }
    }
  }
  /**
   * Conversions from AnyRef to other data types
   *
   * TODO: Should these methods be returning null when an exception occurs??
   */
  implicit def any2Int(in:AnyRef): java.lang.Integer ={
    try{
      Integer.parseInt(in.toString)
    }
    catch{
      case e:Exception =>null
    }
  }
  implicit def any2Double(in:AnyRef): java.lang.Double ={
    try{
      java.lang.Double.parseDouble(in.toString)
    }
    catch{
      case e:Exception => null
    }
  }
  implicit def any2Date(in:AnyRef): java.util.Date ={
    try{
      DateUtils.parseDate(in.toString, Array("yyyy-MM-dd"))
    }
    catch{
      case e:Exception => null
    }
  }
}

/**
 * Define a extensions to java.io.File
 */
object ReflectBean{
  implicit def beanHelper(ref: AnyRef) = new ReflectBean(ref)
}
