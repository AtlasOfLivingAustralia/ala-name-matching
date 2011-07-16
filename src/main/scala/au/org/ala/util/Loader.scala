package au.org.ala.util
import au.org.ala.biocache.DataLoader
import scala.io.Source
import scala.util.parsing.json.JSON

object Loader {
    
    def main(args:Array[String]){
        
        val l = new Loader
        
        println("Welcome to the loader.")
        print("Please supply a resource uid:")
        var input = readLine
        
        while(input != "exit"){
            input.trim match {
                case "" => l.printResourceList
                case it if it startsWith "list" => l.printResourceList
                case _ => l.load(input.trim.toLowerCase)
            }
            print("Please supply a resource uid or type 'list':")
            input = readLine
        }
        println("Goodbye")
    }
}


class Loader extends DataLoader {

    def printResourceList :Unit = {
      val json = Source.fromURL("http://collections.ala.org.au/ws/dataResource").getLines.mkString
      val drs = JSON.parseFull(json).get.asInstanceOf[List[Map[String, String]]]
      
      
      val keys = drs(0).keys.toList
      val valueLengths = keys.map(k => { (k, drs.map(x => x(k).length).max) }).toMap[String, Int]
      
      
      val columns = drs(0).keys.map(k => {
              if(k.length < valueLengths(k)){
                  k + (List.fill[String](valueLengths(k) - k.length)(" ").mkString)
              } else {
                  k
          	  }
      }).mkString(" | ", " | ", " |")
      
      val sep = " " + List.fill[String](columns.length-1)("-").mkString
      println(sep)
      println(columns)
      println(" |" + List.fill[String](columns.length-3)("-").mkString+"|")
      
      
      drs.foreach(dr => {
          println(dr.map(kv => {
              if(kv._2.length < valueLengths(kv._1)){
                  kv._2 + (List.fill[String](valueLengths(kv._1) - kv._2.length)(" ").mkString)
              } else {
                  kv._2
          	  }
          }).mkString(" | ", " | ", " |"))
      })
      
      println(" " + List.fill[String](columns.length-1)("-").mkString)
    }
    
    def load(dataResourceUid:String){
        try {
	        val (protocol, url, uniqueTerms, params, customParams) = retrieveConnectionParameters(dataResourceUid)
	        protocol.toLowerCase match {
	            case "dwc" => {
	                println("Darwin core headed CSV loading")
	                val l = new DwcCSVLoader
	                l.load(dataResourceUid)
	            }
	            case "dwca" => {
	                println("Darwin core archive loading")
	                val l = new DwCALoader
	                l.load(dataResourceUid)
	            }
	            case "digir" => {
	                println("digir webservice loading")
	                val l = new DiGIRLoader
	                l.load(dataResourceUid)
	            }
	            case "flickr" => {
	                println("flickr webservice loading")
	                val l = new FlickrLoader
	                l.load(dataResourceUid)
	            }
	            case "custom" => println("custom webservice loading")
	            case _ => println("Protocol "+ protocol + " currently unsupported.")
	        }
        } catch {
            case e:Exception => e.printStackTrace
        }
    }
}