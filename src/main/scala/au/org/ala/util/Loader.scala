package au.org.ala.util
import au.org.ala.biocache.DataLoader
import scala.io.Source
import scala.util.parsing.json.JSON
import scala.collection.JavaConversions
import scala.collection.mutable.HashMap

class Loader extends DataLoader {

    import scalaj.collection.Imports
    import JavaConversions._
    import StringHelper
._    
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
    
    def healthcheck = {
      val json = Source.fromURL("http://collections.ala.org.au/ws/dataResource/harvesting.json").getLines.mkString
      val drs = JSON.parseFull(json).get.asInstanceOf[List[Map[String, String]]]
      // UID, name, protocol, URL, 
      var digirCache = new HashMap[String, Map[String, String]]()
      //iterate through the resources
      drs.foreach(dr => {
          
          val drUid = dr.getOrElse("uid", "")
          val drName = dr.getOrElse("name", "")
          val connParams  =  dr.getOrElse("connectionParameters", Map[String,AnyRef]()).asInstanceOf[Map[String,AnyRef]]
          
          if(connParams != null){
	          val protocol = connParams.getOrElse("protocol", "").asInstanceOf[String].toLowerCase
	          val url = connParams.getOrElse("url", "").asInstanceOf[String]
	          
	          val status = protocol match {
	              case "dwc" => checkArchive(drUid, url)
	              case "dwca" => checkArchive(drUid, url)
	              case "digir" => {
	                   if(url == null || url ==""){
	                	   Map("Status" -> "NOT CONFIGURED")
	                   } else if(!digirCache.get(url).isEmpty){
	                       digirCache.get(url).get 
	                   } else {
	                       val result = checkDigir(drUid, url)
	                       digirCache.put(url,result)
	                       result
	                   }
	              }
	              case _ => Map("Status" -> "IGNORED")
	          }
	           
	        if(status.getOrElse("Status", "NO STATUS") != "IGNORED"){
	            
	            val fileSize = status.getOrElse("Content-Length", "N/A")
	            val displaySize = {
		            if(fileSize!= "N/A"){
		                (fileSize.toInt / 1024).toString +"kB"
		            } else {
		                fileSize
		            }
	            }
	        	println(drUid.fixWidth(5)+ "\t"+protocol.fixWidth(8)+"\t" + 
	        	        status.getOrElse("Status", "NO STATUS").fixWidth(15) +"\t" + drName.fixWidth(65) + "\t" + url.fixWidth(85) + "\t" + 
	        	        displaySize) 
	        }
          }
      })
    }
    
    def checkArchive(drUid:String, url:String) : Map[String,String] = {
      if(url != ""){
          val conn = (new java.net.URL(url)).openConnection()
          val headers = conn.getHeaderFields()
          val map = new HashMap[String,String]
          headers.foreach({case(header, values) => {
	          map.put(header, values.mkString(","))
   		  }})
   		  map.put("Status", "OK")
   		  map.toMap
      } else {
          Map("Status" -> "UNAVAILABLE")
      }
    }
    
    def checkDigir(drUid:String, url:String) : Map[String,String] = {
            	  
      try {
    	  val conn = (new java.net.URL(url)).openConnection()
          val headers = conn.getHeaderFields()
          val hdrs = new HashMap[String,String]()
          headers.foreach( {case(header, values) => {
              hdrs.put(header,values.mkString(","))
          }})
          (hdrs + ("Status" -> "OK")).toMap
      } catch {
          case e:Exception => { 
              e.printStackTrace
              Map("Status" -> "UNAVAILABLE")
          }
      }
    }
}