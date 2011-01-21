package au.org.ala.util {

class ReflectBean(ref: AnyRef)  {
  def getter(name: String): Any = ref.getClass.getMethods.find(_.getName == name).get.invoke(ref)
  def setter(name: String, value: Any): Unit = {
    var fieldName = name
    if(fieldName=="class"){
     fieldName = "classs"
    }
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

}