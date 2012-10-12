package au.org.ala.util

import au.org.ala.biocache.Config
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.{NameValuePair, HttpClient}
import java.io.{File, BufferedReader, InputStreamReader}
import au.org.ala.biocache.outliers.SpeciesOutlierTests

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
      print("\nPlease supply a command or hit ENTER to view command list. \nbiocache> ")

      var input = readLine
      while (input != "exit" && input != "q" && input != "quit") {
        CMD.executeCommand(input)
        print("\nbiocache> ")
        input = readLine
      }
    } else {
      CMD.executeCommand(args.mkString(" "))
    }
    //close down the data store and index so the program can exit normally
    au.org.ala.biocache.Config.persistenceManager.shutdown
    IndexRecords.indexer.shutdown
    println("Bye.\n")
  }
}

object ScriptTool {

  def main(args:Array[String]){
    val isReader = new InputStreamReader(System.in)
    val bufReader = new BufferedReader(isReader)
    try {
     var inputStr = bufReader.readLine()
     while(inputStr !=null){
        println("Executing command '" + inputStr + "'")
        if(inputStr.trim.length>0) CMD.executeCommand(inputStr)
        inputStr = bufReader.readLine()
     }
     au.org.ala.biocache.Config.persistenceManager.shutdown
     IndexRecords.indexer.shutdown
     println("Script complete.\n")
     System.exit(1)
  } catch {
      case e:Exception => {
        au.org.ala.biocache.Config.persistenceManager.shutdown
        IndexRecords.indexer.shutdown
        System.exit(1)
      }
    }
  }
}

object CMD {

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
        case it if ((it startsWith "load-local-csv") && (it.split(" ").length == 3))  =>  {
          val parts = it.split(" ")
          val d = new DwcCSVLoader()
          d.loadFile(new File(parts(2)),parts(1), List(), Map())
        }
        case it if (it startsWith "load") || (it startsWith "ld") =>  {
          it.split(" ").map(x => x.trim).tail.foreach(drUid => l.load(drUid))
        }
        case it if(it startsWith "test-load") =>{
          it.split(" ").map(x => x.trim).tail.foreach(drUid => l.load(drUid,true))
        }
        case it if (it startsWith "process-single") => {
          it.split(" ").map(x => x.trim).tail.foreach(uuid => ProcessSingleRecord.processRecord(uuid))
        }
        case it if (it startsWith "process") || (it startsWith "process") => {
          val drs = it.split(" ").map(x => x.trim).toList.tail
          drs.foreach(dr =>{
            val (hasRowKeys, filename) = hasRowKey(dr)
            println("Processing " + dr + " incremental=" +hasRowKeys)
            if(!hasRowKeys)
              ProcessWithActors.processRecords(4, None, Some(dr))
            else{
              val p = new RecordProcessor
              p.processFileThreaded(new java.io.File(filename.get), 4)
              //ProcessRecords.main(Array("-f",filename.get, "-t","4"))
            }
          }
            )
        }
        case it if (it startsWith "process-all") => {
          ProcessWithActors.processRecords(4, None, None)
        }
        case it if(it startsWith "index-delete")=> {
          //need to preserve the query case because T and Z mean things in dates
          val query = input.replaceFirst("index-delete ","")
          val deletor = new QueryDelete(query)
          println("Delete from index using query : " + query)
          deletor.deleteFromIndex
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
          println("The data is viewable here: " + Config.reindexViewDataResourceUrl + dr)
        }
        case it if (it startsWith "index-custom") => {
          if (it.split(" ").length > 2){
            val (cmdAndDr, additionalFields) = it.split(" ").splitAt(2)
            IndexRecords.index(None, None, Some(cmdAndDr(1)), false, false, miscIndexProperties = additionalFields)
          }
        }
        case it if (it startsWith "index ") || (it startsWith "index") => {
          val drs = it.split(" ").map(x => x.trim).toList.tail
          drs.foreach(dr => {
            val (hasRowKeys, filename) = hasRowKey(dr)
            if(!hasRowKeys)
              IndexRecords.index(None, None, Some(dr), false, false)
            else 
              IndexRecords.indexListThreaded(new File(filename.get), 4)
            })
        }
        case it if (it startsWith "createdwc") => {
          val args = it.split(" ").map(x => x.trim).toArray.tail
          DwCACreator.main(args)
        }
        case it if (it startsWith "optimise") => {
          IndexRecords.indexer.optimise
        }
        case it if (it startsWith "healthcheck") => l.healthcheck
        case it if (it startsWith "export-for-outlier") => {
          val args = input.split(" ").map(x => x.trim).toArray.tail
          ExportForOutliers.main(args)
        }    
        case it if (it startsWith "gbif-csv") => {
          val args = it.split(" ").map(x => x.trim).toArray.tail
           GBIFOrgCSVCreator.main(args)
        }        
        case it if (it startsWith "export-index") => {
          val args = it.split(" ").map(x => x.trim).toArray.tail
          ExportFromIndex.main(args)
        }
        case it if (it startsWith "export-facet") => {
          val args = it.split(" ").map(x => x.trim).toArray.tail
          ExportFacet.main(args)
        }
        case it if (it startsWith "export-facet-query") => {
          val args = it.split(" ").map(x => x.trim).toArray.tail
          ExportByFacetQuery.main(args)
        }
        case it if (it startsWith  "export") => {
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
          val dr = args(0)
          val (hasRowKeys, filename) = hasRowKey(dr)
          if(!hasRowKeys)
            Sampling.main(Array("-dr") ++ args )
          else
            Sampling.main(Array("-rf", filename.get,"-dr") ++ args)
        }
        case it if (it startsWith "resample") => {
          println("****** Warning - this requires at least 8g of memory allocation -Xmx8g -Xms8g")
          val query = it.replaceFirst("resample","")
          ResampleRecordsByQuery.main(Array(query))
        }
        case it if (it startsWith "download-media") => {
          val args = it.split(" ").map(x => x.trim).toArray.tail
          DownloadMedia.main(args)
        }  
        case it if (it startsWith "dedup") => {
          val args = it.split(" ").map(x => x.trim).toArray.tail
          DuplicationDetection.main(args)
        }   
        case it if (it.startsWith("jackknife") || it.startsWith("jacknife")) => {
          val args = it.split(" ").map(x => x.trim).toArray.tail
          SpeciesOutlierTests.main(args)
        }           
        case it if (it startsWith "delete-resource") => {
          val args = it.split(" ").map(x => x.trim).toArray.tail
          args.foreach(drUid => {
            val drvd = new DataResourceDelete(drUid)
            println("Delete from storage: " + drUid)
            drvd.deleteFromPersistent
            println("Delete from index: " + drUid)
            drvd.deleteFromIndex
            println("Finished delete for : " + drUid)
          })
        }
        case it if (it startsWith "delete") => {
          //need to preserve the query case because T and Z mean things in dates
          val query = input.replaceFirst("delete ","")
          val deletor = new QueryDelete(query)
          println("Delete from storage using the query: " + query)
          deletor.deleteFromPersistent()
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
    padAndPrint(" [1]  list - Print the list of resources available for harvesting")
    padAndPrint(" [2]  describe <dr-uid1> <dr-uid2>... - Show the configuration or the resource")
    padAndPrint(" [3]  load <dr-uid1> <dr-uid2>... - Load resource into biocache (does not index)")
    padAndPrint(" [4]  process-single <uuid1> <uuid2> ... - Process single record (SDS/namematching)")
    padAndPrint(" [5]  process <dr-uid1> <dr-uid2>... - Process resource")
    padAndPrint(" [6]  process-all - Process all records (this takes a long time for full biocache)")
    padAndPrint(" [7]  index <dr-uid1> <dr-uid2>... - Index resource (for offline use only)")
    padAndPrint(" [8]  index-live <dr-uid> - Index resource by calling webservice to index. Dont use for large resources.")
    padAndPrint(" [9]  index-custom <dr-uid> <list-of-misc-fields> - Index resource while indexing miscellanous properties.")
    padAndPrint("[10]  createdwc <dr-uid or 'all'> <export directory> - Create a darwin core archive for a resource")
    padAndPrint("[11]  healthcheck - Do a healthcheck on the configured resources in the collectory")
    padAndPrint("[12]  export - CSV export of data")
    padAndPrint("[13]  export-gbif-archives - Comma separated list of data resources or 'all'")    
    padAndPrint("[14]  export-index <output-file> <csv-list-of fields> <solr-query> - export data from index")
    padAndPrint("[15]  export-facet <facet-field> <facet-output-file> -fq <filter-query> - export data from index")
    padAndPrint("[16]  export-facet-query <facet-field> <facet-output-file> -fq <filter-query> - export data from index")
    padAndPrint("[17]  export-for-outliers <index-directory> <export-directory> -fq <filter-query> - export data from index for outlier detection")   
    padAndPrint("[18]  import - CSV import of data")
    padAndPrint("[19]  optimise - Optimisation of SOLR index (this takes some time)")
    padAndPrint("[20]  sample-all - Run geospatial sampling for all records")
    padAndPrint("[21]  sample <dr-uid1> <dr-uid2>... - Run geospatial sampling for records for a data resource")
    padAndPrint("[22]  resample <query> - Rerun geospatial sampling for records that match a SOLR query")
    padAndPrint("[23]  delete <solr-query> - Delete records matching a query")
    padAndPrint("[24]  delete-resource <dr-uid1> <dr-uid2>... - Delete records for a resource. Requires a index reopen (http get on /ws/admin/modify?reopenIndex=true)")
    padAndPrint("[25]  index-delete <query> - Delete record that satisfies the supplied query from the index ONLY")
    padAndPrint("[26]  load-local-csv <dr-uid> <filepath>... - Load a local file into biocache. For development use only. Not to be used in production.")
    padAndPrint("[27]  test-load <dr-uid1> <dr-uid2>... - Performs some testing on the load process.  Please read the output to determine whether or not a load should proceed.")
    padAndPrint("[28]  download-media - Force the (re)download of media associated with a resource.")
    padAndPrint("[29]  dedup - Run duplication detection over the records.")
    padAndPrint("[30]  jackknife - Run jackknife outlier detection.")
    padAndPrint("[31]  exit")
  }
  
  def hasRowKey(resourceUid:String):(Boolean, Option[String])={
    def filename = "/data/tmp/row_key_"+resourceUid+".csv"
    def file = new java.io.File(filename)
    
    if(file.exists()){
      val date = new java.util.GregorianCalendar()
      date.setTime(new java.util.Date)
      date.add(java.util.Calendar.HOUR, -24)
      //if it is on the same day assume that we want the incremental process or index.
      if(org.apache.commons.io.FileUtils.isFileNewer(file,date.getTime()))
        (true, Some(filename))
      else{
        //prompt the user 
        println("There is an incremental row key file for this resource.  Would you like to perform an incremental process (y/n) ?")
        val answer = readLine
        if(answer.toLowerCase().equals("y") || answer.toLowerCase().equals("yes"))
          (true,Some(filename))
        else
          (false, None)
      }
    }
    else
      (false,None)
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