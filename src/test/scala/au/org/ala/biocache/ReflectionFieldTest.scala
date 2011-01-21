package au.org.ala.biocache

object ReflectionFieldTest {

  def main(args: Array[String]): Unit = {  
	  
	  
	  for(term <-TypeStatus.all){
		println(term.canonical)
	  }
  }

}