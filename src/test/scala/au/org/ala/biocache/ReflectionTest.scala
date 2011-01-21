package au.org.ala.biocache

object ReflectionTest {
  def main(args: Array[String]): Unit = {

    val occurrence = new Occurrence

    implicit def ReflectBean(ref: AnyRef) = new {
      def getter(name: String): Any = ref.getClass.getMethods.find(_.getName == name).get.invoke(ref)
      def setter(name: String, value: Any): Unit = ref.getClass.getMethods.find(_.getName == name + "_$eq").get.invoke(ref, value.asInstanceOf[AnyRef])
    }

    occurrence.setter("basisOfRecord", "PreservedSpecimen")
    println("resourceID : " + occurrence.basisOfRecord)
    
    
    val methods = occurrence.getClass.getMethods
    for(method <- methods){
    	println(method.getName)
    }
  }
}
