package au.org.ala.util
import au.org.ala.biocache.DataLoader
import scala.io.Source
import scala.util.parsing.json.JSON
import scala.collection.JavaConversions
import scala.collection.mutable.HashMap

/**
 * Runnable loader that just takes a resource UID and delegates based on the protocol.
 */
object Loader {

  def main(args:Array[String]){

    var dataResourceUid:String = ""
    val parser = new OptionParser("index records options") {
        arg("data-resource-uid","The data resource to process", {v:String => dataResourceUid = v})
    }

    if(parser.parse(args)){
      println("Starting to load resource: " + dataResourceUid)
      val l = new Loader
      l.load(dataResourceUid)
      println("Completed loading resource: " + dataResourceUid)
      au.org.ala.biocache.Config.persistenceManager.shutdown
    }
  }
}

class Loader extends DataLoader {

    import scalaj.collection.Imports
    import JavaConversions._
    import StringHelper._

    def describeResource(drlist:List[String]){
        drlist.foreach(dr => {
        	val (protocol, url, uniqueTerms, params, customParams) = retrieveConnectionParameters(dr)
          println("UID: " + dr)
          println("Protocol: "+ protocol)
          println("URL: " + url.mkString(";"))
          println("Unique terms: " + uniqueTerms.mkString(","))
	        params.foreach({case(k,v) => println(k+": "+v)})
            customParams.foreach({case(k,v) => println(k +": "+v)})
            println("---------------------------------------")
        })
    }
    
    def printResourceList {
      val json = Source.fromURL("http://collections.ala.org.au/ws/dataResource?resourceType=records").getLines.mkString
      val drs = JSON.parseFull(json).get.asInstanceOf[List[Map[String, String]]]
      CMD.printTable(drs)
    }

    def load(dataResourceUid: String, test:Boolean=false) {
      try {
        val (protocol, url, uniqueTerms, params, customParams) = retrieveConnectionParameters(dataResourceUid)
        protocol.toLowerCase match {
          case "dwc" => {
            println("Darwin core headed CSV loading")
            val l = new DwcCSVLoader
            l.load(dataResourceUid, false,test)
          }
          case "dwca" => {
            println("Darwin core archive loading")
            val l = new DwCALoader
            l.load(dataResourceUid, false,test)
          }
          case "digir" => {
            println("digir webservice loading")
            val l = new DiGIRLoader
            if(!test)
              l.load(dataResourceUid)
            else
              println("TESTING is not supported for DiGIR")
          }
          case "flickr" => {
            println("flickr webservice loading")
            val l = new FlickrLoader
            if(!test)
              l.load(dataResourceUid)
            else
              println("TESTING is not supported for Flickr")
          }
          case "customwebservice" => {
            println("custom webservice loading")
            if(!test){
              val className = customParams.getOrElse("classname", null)
              if (className == null) {
                println("Classname of custom harvester class not present in parameters")
              } else {
                val wsClass = Class.forName(className)
                val l = wsClass.newInstance()
                if (l.isInstanceOf[CustomWebserviceLoader]) {
                  l.asInstanceOf[CustomWebserviceLoader].load(dataResourceUid)
                } else {
                  println("Class " + className + " is not a subtype of au.org.ala.util.CustomWebserviceLoader")
                }
              }
            }
            else
              println("TESTING is not suppported for custom web service")
          }
          case "autofeed" => {
            println("AutoFeed Darwin core headed CSV loading")
            val l = new AutoDwcCSVLoader
            if(!test)
              l.load(dataResourceUid)
            else
              println("TESTING is not supported for autofeed")
          }
          case _ => println("Protocol " + protocol + " currently unsupported.")
        }
      } catch {
        case e: Exception => e.printStackTrace
      }
      
      if(test){
        println("Check the output for any warning/error messages.")
        println("If there are any new institution and collection codes ensure that they are handled correctly in the Collectory.")
        println("""Don't forget to check that number of NEW records.  If this is high for updated data set it may indicate that the "unique values" have changed format.""")
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

            val urlsObject = connParams.getOrElse("url", List[String]())
            val urls:Seq[String] = {
              if(urlsObject.isInstanceOf[Seq[String]]) urlsObject.asInstanceOf[Seq[String]]
              else List(connParams("url").asInstanceOf[String])
            }

            urls.foreach(url => {
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
            })
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