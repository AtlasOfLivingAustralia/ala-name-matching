package au.org.ala.util

import org.apache.commons.lang.time.DateUtils

class ReflectBean(ref: AnyRef)  {

  def fieldNameCheck(fieldName:String) = if(fieldName=="class") "classs" else fieldName

  def getter(name: String): Any = {
    var fieldName = fieldNameCheck(name)
    ref.getClass.getMethods.find(_.getName == fieldName).get.invoke(ref)
  }
  def setter(name: String, value: Any): Unit = {
    var fieldName = fieldNameCheck(name)
    val method = ref.getClass.getMethods.find(_.getName == fieldName + "_$eq")

    if(!method.isEmpty){
      val typ =method.get.getParameterTypes()(0)
      var v2 = value.asInstanceOf[AnyRef]

     
      typ.getName match{
        case "java.lang.Integer" =>{ v2 = any2Int(v2)
          println("Converting to Integer")}
        case "java.lang.Double" => v2 = any2Double(v2)
        case "java.util.Date" => v2 = any2Date(v2)
        case _ => 
      }
      method.get.invoke(ref, v2 )


    }
  }
  /**
   * Conversions from AnyRef to other data types
   *
   * TODO: Should these methods be returning null when an exception occurs??
   */
  implicit def any2Int(in:AnyRef):Integer ={
    try{
      Integer.parseInt(in.toString)
    }
    catch{
      case e:Exception =>null
    }
  }
  implicit def any2Double(in:AnyRef):java.lang.Double ={
    try{
      java.lang.Double.parseDouble(in.toString)
    }
    catch{
      case e:Exception => null
    }
  }
  implicit def any2Date(in:AnyRef):java.util.Date ={
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
  implicit def file2helper(ref: AnyRef) = new ReflectBean(ref)
}
