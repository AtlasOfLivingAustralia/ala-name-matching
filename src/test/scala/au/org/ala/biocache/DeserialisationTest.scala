package au.org.ala.biocache

import com.google.gson.reflect.TypeToken
import java.util.ArrayList
import com.google.gson.Gson
import java.util.UUID
import org.apache.cassandra.thrift._
import org.apache.thrift.transport._

//import org.codehaus.jackson.map
//import org.codehaus.jackson.map.{ObjectMapper, DeserializationConfig}
import scala.reflect._

object DeserialisationTest {

	def main(args : Array[String]) : Unit = {
		
		val uuid2 = UUID.randomUUID.toString
		var qa2 = new QualityAssertion
		qa2.uuid = uuid2
		qa2.assertionCode  = 123
		qa2.positive = true
		qa2.comment = "My comment"
		qa2.userId = "David.Martin@csiro.au"
		qa2.userDisplayName = "Dave Martin"
			

		val array = Array(qa2,qa2)

		val gson = new Gson
		
		//parse back into an array
		val json = gson.toJson(array)
		println("GSON Serialised:" + json)
		val listType = new TypeToken[ArrayList[QualityAssertion]]() {}.getType();
		
		val listOfAssertions = gson.fromJson(json,listType).asInstanceOf[java.util.List[QualityAssertion]]
		println("GSON Deserialised: " + listOfAssertions)
		val iter = listOfAssertions.iterator
		while(iter.hasNext){
			println("GSON Deserialised: " + iter.next.comment)
		}
	}
}