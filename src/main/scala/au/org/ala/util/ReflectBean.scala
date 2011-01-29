package au.org.ala.util

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
      method.get.invoke(ref, value.asInstanceOf[AnyRef])
    }
  }
}

/**
 * Define a extensions to java.io.File
 */
object ReflectBean{
  implicit def file2helper(ref: AnyRef) = new ReflectBean(ref)
}
