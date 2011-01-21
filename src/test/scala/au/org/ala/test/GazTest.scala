package au.org.ala.util

import java.io.File
import java.net.{URLConnection, URL, URLEncoder}
import scala.io.Source
import org.apache.commons.httpclient._
import org.apache.commons.httpclient.methods._

object GazTest {

  def main(args: Array[String]): Unit = { 
	  
	 import FileHelper._;
	 
	 val file = new File("/data/points.txt")
	 val client = new HttpClient
	 var counter = 0
	 var start = System.currentTimeMillis
	 var finish = System.currentTimeMillis

	 file.foreachLine {line => {
		 	val parts = line.split("\t")
		 	val latitude = (parts(1).toFloat)/10000
		 	val longitude = (parts(2).toFloat)/10000
		 	
		 	try {
		 		
		 		val g = new GetMethod("http://spatial.ala.org.au/gazetteer/search?lon="+longitude+"&lat="+latitude)
		 		client.executeMethod(g)
		 		println(counter + " " + g.getStatusCode)
		 		counter+=1
		 		if(counter % 100 == 0){
		 			finish = System.currentTimeMillis
		 			println("last 100, time taken = "+(finish-start)/1000+" seconds. Time for one lookup = "+ ((finish.toFloat-start.toFloat)/1000f)/100f)
		 			start = System.currentTimeMillis
		 		}
		 	} catch {
		 		case e => println(e.getMessage)
		 	}
	 	}
	 }
  }
}