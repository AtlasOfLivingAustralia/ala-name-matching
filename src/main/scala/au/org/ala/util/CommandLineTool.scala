package au.org.ala.util

import au.org.ala.biocache.Config
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.{NameValuePair, HttpClient}

/**
 * Command line tool that allows administrators to run commands on
 * the biocache. Any additional tools should be incorporated into this executable.
 */
object CommandLineTool {

  def main(args: Array[String]) {

    if(args.isEmpty){

      println("----------------------------")
      println("| Biocache management tool |")
      println("----------------------------")
      print("\n\nWarning: this tool may hurt your eyes with spurious logging levels.")
      print("\nPlease supply a command or hit ENTER to view command list:")

      var input = readLine
      while (input != "exit" && input != "q" && input != "quit") {
        executeCommand(input)
        print("\nPlease supply a command or hit ENTER to view command list: ")
        input = readLine
      }
    } else {
      executeCommand(args.mkString(" "))
    }
    //close down the data store and index so the program can exit normally
    au.org.ala.biocache.Config.persistenceManager.shutdown
    IndexRecords.indexer.shutdown
    println("Goodbye\n")
  }

  /**
   * Attempt to execute the supplied command
   * 
   * @param input
   */
  def executeCommand(input:String){
    try {
      val l = new Loader
      input.toLowerCase.trim match {
        case it if (it startsWith "describe ") || (it startsWith "d ") => l.describeResource(it.split(" ").map(x => x.trim).toList.tail)
        case it if (it startsWith "list") || (it == "l") => l.printResourceList
        case it if (it startsWith "load") || (it startsWith "ld") =>  l.load(it.split(" ").map(x => x.trim).toList.last)
        case it if (it startsWith "process-single") => {
          ProcessSingleRecord.processRecord(it.split(" ").map(x => x.trim).toList.last)
        }
        case it if (it startsWith "process") || (it startsWith "process") => {
          val drs = it.split(" ").map(x => x.trim).toList.tail
          for (dr <- drs) {
            ProcessWithActors.processRecords(4, None, Some(dr))
          }
        }
        case it if (it startsWith "process-all") => {
          ProcessWithActors.processRecords(4, None, None)
        }
        case it if (it.startsWith("index-live ") && input.split(" ").length == 2) => {
          val dr = it.split(" ").map(x => x.trim).toList.last

          println("Indexing live with URL: " + Config.reindexUrl +", and params: " + Config.reindexData + "&dataResource=" + dr)
          val http = new HttpClient
          val post = new PostMethod(Config.reindexUrl)

          val nameValuePairs = {
            val keyValue = Config.reindexData.split("&")
            val nvpairs = keyValue.map(kv => {
              val parts = kv.split("=")
              new NameValuePair(parts(0), parts(1))
            })
            nvpairs.toArray ++ Array(new NameValuePair("dataResource", dr))
          }
          post.setRequestBody(nameValuePairs)
          val responseCode = http.executeMethod(post)
          println("Response: " + responseCode)
        }
        case it if (it startsWith "index ") || (it startsWith "index") => {
          val drs = it.split(" ").map(x => x.trim).toList.tail
          for (dr <- drs) {
            IndexRecords.index(None, Some(dr), false, false)
          }
        }
        case it if (it startsWith "createdwc") => {
          val args = it.split(" ").map(x => x.trim).toArray.tail
          DwCACreator.main(args)
        }
        case it if (it startsWith "optimise") => {
          IndexRecords.indexer.optimise
        }
        case it if (it startsWith "healthcheck") => l.healthcheck
        case it if (it startsWith "export") => {
          val args = it.split(" ").map(x => x.trim).toArray.tail
          ExportUtil.main(args)
        }
        case it if (it startsWith "import") => {
          val args = it.split(" ").map(x => x.trim).toArray.tail
          ImportUtil.main(args)
        }
        case it if (it startsWith "sample-all") => {
          println("****** Warning - this requires at least 8g of memory allocation -Xmx8g -Xms8g")
          Sampling.main(Array())
        }
        case it if (it startsWith "sample") => {
          println("****** Warning - this requires at least 8g of memory allocation -Xmx8g -Xms8g")
          val args = it.split(" ").map(x => x.trim).toArray.tail
          Sampling.main(Array("-dr") ++ args )
        }
        case it if (it startsWith "delete-resource") => {
          val args = it.split(" ").map(x => x.trim).toArray.tail
          val drvd = new DataResourceDelete(args.last)
          println("Delete from storage")
          drvd.deleteFromPersistent
          println("Delete from index")
          drvd.deleteFromIndex
          println("Finished delete.")
        }
        case it if (it startsWith "delete") => {
          val deletor = new QueryDelete(it.replaceFirst("delete ",""))
          println("Delete from storage")
          deletor.deleteFromPersistent()
          println("Delete from index")
          deletor.deleteFromIndex
        }
        case it if(it startsWith "index-delete")=> {
          val deletor = new QueryDelete(it.replaceFirst("delete ",""))
          println("Delete from index")
          deletor.deleteFromIndex
        }
        case _ => printHelp
      }
    } catch {
      case e: Exception => e.printStackTrace
    }    
  }
  
  
  def printHelp = {
    padAndPrint(" [1]  list - print list of resources")
    padAndPrint(" [2]  describe <dr-uid> <dr-uid1> <dr-uid2> - show the configuration or the resource")
    padAndPrint(" [3]  load <dr-uid> - load resource into biocache (does not index)")
    padAndPrint(" [4]  process-single <uuid> - process single record (SDS/namematching)")
    padAndPrint(" [5]  process <dr-uid> - process resource")
    padAndPrint(" [6]  process-all - process all records (this takes a long time for full biocache)")
    padAndPrint(" [7]  index <dr-uid> - index resource (for offline use only)")
    padAndPrint(" [8]  index-live <dr-uid> - index resource by calling webservice to index. Dont use for large resources.")
    padAndPrint(" [9]  createdwc <dr-uid> <export directory> - create a darwin core archive for a resource")
    padAndPrint("[10]  healthcheck - Do a healthcheck on the configured resources in the collectory")
    padAndPrint("[11]  export - CSV export of data")
    padAndPrint("[12]  import - CSV import of data")
    padAndPrint("[13]  optimise - Optimisation of SOLR index (this takes some time)")
    padAndPrint("[14]  sample-all - Run geospatial sampling for all records")
    padAndPrint("[15]  sample <dr-uid> - Run geospatial sampling for records for a data resource")
    padAndPrint("[16]  delete <solr-query> - Delete records matching a query")
    padAndPrint("[17]  delete-resource <data-resource-uid> - Delete records for a resource. Requires a index reopen (http get on /ws/admin/modify?reopenIndex=true)")
    padAndPrint("[18]  index-delete <query> - Delete record that satisfies the supplied query from the index ONLY")
    padAndPrint("[19]  exit")
  }

  def padAndPrint(str:String) = println(padElementTo60(str))

  def padElementTo60(str:String) = padElement(str, 60)

  def padElement(str:String, width:Int) = {
    val indexOfHyphen = str.indexOf(" -")
    str.replace(" - ",  Array.fill(width - indexOfHyphen)(' ').mkString  + " - " )
  }

  def printTable(table: List[Map[String, String]]) {

    val keys = table(0).keys.toList
    val valueLengths = keys.map(k => {
      (k, table.map(x => x(k).length).max)
    }).toMap[String, Int]
    val columns = table(0).keys.map(k => {
      if (k.length < valueLengths(k)) {
        k + (List.fill[String](valueLengths(k) - k.length)(" ").mkString)
      } else {
        k
      }
    }).mkString(" | ", " | ", " |")

    val sep = " " + List.fill[String](columns.length - 1)("-").mkString
    println(sep)
    println(columns)
    println(" |" + List.fill[String](columns.length - 3)("-").mkString + "|")

    table.foreach(dr => {
      println(dr.map(kv => {
        if (kv._2.length < valueLengths(kv._1)) {
          kv._2 + (List.fill[String](valueLengths(kv._1) - kv._2.length)(" ").mkString)
        } else {
          kv._2
        }
      }).mkString(" | ", " | ", " |"))
    })

    println(" " + List.fill[String](columns.length - 1)("-").mkString)
  }
}