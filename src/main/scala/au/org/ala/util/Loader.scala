package au.org.ala.util
import au.org.ala.biocache.DataLoader
import scala.io.Source
import scala.util.parsing.json.JSON

object Loader {
    
    def main(args:Array[String]){
        
        val l = new Loader
        
        println("Welcome to the loader.")
        print("Please supply a command or hit ENTER to view command list:")
        var input = readLine
        
        while(input != "exit"){
            
            try {
	            input.toLowerCase.trim match {
	            	case it if (it startsWith "describe ") || (it startsWith "d ")  => l.describeResource(it.split(" ").map(x => x.trim).toList.tail)
	                case it if (it startsWith "list") || (it == "l" )  => l.printResourceList
	                case it if (it startsWith "load") || (it startsWith "ld" )  => l.load(it.split(" ").map(x => x.trim).toList.last)
	                case _ => printHelp
	            }
            } catch{ 
            case e:Exception => e.printStackTrace 
            }
            print("\nPlease supply a command or hit ENTER to view command list:")
            input = readLine
        }
        println("Goodbye\n")
    }
    
    def printHelp = {
        println("1)  list - print list of resources")
        println("2)  describe <dr-uid> <dr-uid1> <dr-uid2>... - print list of resources")
        println("3)  load <dr-uid> - load resource")
        println("4)  healthcheck <dr-uid>")
        println("5)  healthcheck all - takes ages....")
        println("6)  exit")
    }
}


class Loader extends DataLoader {

    def describeResource(drlist:List[String]){
        
        drlist.foreach(dr => {
        	val (protocol, url, uniqueTerms, params, customParams) = retrieveConnectionParameters(dr)
            println("UID: "+dr)
        	println("Protocol: "+protocol)
            println("URL: " + url)
	        params.foreach({case(k,v) => println(k+": "+v)})
            customParams.foreach({case(k,v) => println(k +": "+v)})
            println("---------------------------------------")
        })
    }
    
    def printResourceList :Unit = {
      val json = Source.fromURL("http://collections.ala.org.au/ws/dataResource?resourceType=records").getLines.mkString
      val drs = JSON.parseFull(json).get.asInstanceOf[List[Map[String, String]]]
      
      CommandLineTool.printTable(drs)

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