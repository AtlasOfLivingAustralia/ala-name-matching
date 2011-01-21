package au.org.ala.biocache

import java.util.UUID
object EqualsTest {

  def main(args: Array[String]): Unit = { 

	  	val uuid1 = UUID.randomUUID.toString
		var qa1 = new QualityAssertion
		qa1.uuid = uuid1
		qa1.assertionCode  = 123
		qa1.positive = true
		qa1.comment = "My comment"
		qa1.userId = "David.Martin@csiro.au"
		qa1.userDisplayName = "Dave Martin"
	  
	  	val uuid2 = UUID.randomUUID.toString
		var qa2 = new QualityAssertion
		qa2.uuid = uuid2
		qa2.assertionCode  = 123
		qa2.positive = true
		qa2.comment = "My comment"
		qa2.userId = "David.Martin@csiro.au"
		qa2.userDisplayName = "Dave Martin"
	  
		
		println(qa1 equals qa2)
			
	
			
	  
	  
	  
	  
  }

}