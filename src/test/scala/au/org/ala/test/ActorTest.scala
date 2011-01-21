package au.org.ala.cluster

import scala.actors.Actor
import scala.actors.Actor._

object ActorTest {
  def main(args : Array[String]) : Unit = {
	  
	  val paul = actor {
	 	  loop {
		 	 receive {
		 		 case message:String => println(message)
		 		 case exit:Int => { 
		 			 println("exit code")
		 			 return
		 		 }
		 		 case _ => println("dead")
		 	 }
	 	  }
	 	  
	  }
	  paul ! "test" 
	  paul ! "test2"
	  paul ! "test3"
	  paul ! 123
  }
}
