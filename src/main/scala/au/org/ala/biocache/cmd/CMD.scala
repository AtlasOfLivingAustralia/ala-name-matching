package au.org.ala.biocache.cmd

import au.org.ala.biocache.load.{DownloadMedia, Loader, DwcCSVLoader}
import au.org.ala.biocache.export._
import au.org.ala.biocache.tool._
import au.org.ala.biocache.util._
import scala.Some
import au.org.ala.biocache.index.IndexRecords
import java.io.File
import au.org.ala.biocache.outliers.SpeciesOutlierTests
import au.org.ala.biocache.qa.ValidationRuleRunner
import au.org.ala.biocache.Config
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.{NameValuePair, HttpClient}
import scala.Some

/**
 * Singleton for running commands.
 */
object CMD {

  /**
   * Attempt to execute the supplied command
   *
   * @param input
   */
  def executeCommand(input: String, throwException:Boolean=false) {
    try {
      val l = new Loader
      input.toLowerCase.trim match {
        case it if (it startsWith "describe ") || (it startsWith "d ") => l.describeResource(it.split(" ").map(x => x.trim).toList.tail)
        case it if (it startsWith "list") || (it == "l") => l.printResourceList
        case it if ((it startsWith "load-local-csv") && (it.split(" ").length == 3)) => {
          val parts = it.split(" ")
          val d = new DwcCSVLoader()
          d.loadFile(new File(parts(2)), parts(1), List(), Map())
        }
        case it if (it == "ingest-all")  => {
          l.resourceList.foreach(resource => {
           val uid = resource.getOrElse("uid", "")
           val name = resource.getOrElse("name", "")
           println(s"Ingesting resource $name, uid: $uid")
           if(uid != ""){
             println("Loading: " + uid)
             l.load(uid)
             println("Sampling: " + uid)
             Sampling.main(Array("-dr", uid))
             println("Processing: " + uid)
             ProcessWithActors.processRecords(4, None, Some(uid))
             println("Indexing: " + uid)
             IndexRecords.index(None, None, Some(uid), false, false)
             println("Finished ingest for: " + uid)
           }
          })
        }
        case it if (it startsWith "ingest")  => {
          val drs = it.split(" ").map(x => x.trim).tail
          drs.foreach(dr => {
           l.load(dr)
           Sampling.main(Array("-dr", dr))
           ProcessWithActors.processRecords(4, None, Some(dr))
           IndexRecords.index(None, None, Some(dr), false, false)
          })
        }
        case it if (it startsWith "force-load") => {
          it.split(" ").map(x => x.trim).tail.foreach(drUid => l.load(drUid, forceLoad=true))
        }
        case it if (it startsWith "load") || (it startsWith "ld") => {
          it.split(" ").map(x => x.trim).tail.foreach(drUid => l.load(drUid))
        }
        case it if (it startsWith "test-load") => {
          it.split(" ").map(x => x.trim).tail.foreach(drUid => l.load(drUid, true))
        }
        case it if (it startsWith "process-single") => {
          it.split(" ").map(x => x.trim).tail.foreach(uuid => ProcessSingleRecord.processRecord(uuid))
        }
        case it if (it == "process-all") => {
          ProcessWithActors.processRecords(4, None, None)
        }
        case it if (it startsWith "process") || (it startsWith "process") => {
          val drs = it.split(" ").map(x => x.trim).toList.tail
          drs.foreach(dr => {
            val (hasRowKeys, filename) = hasRowKey(dr)
            println("Processing " + dr + " incremental=" + hasRowKeys)
            if (!hasRowKeys)
              ProcessWithActors.processRecords(4, None, Some(dr))
            else {
              val p = new RecordProcessor
              p.processFileThreaded(new java.io.File(filename.get), 4)
            }
          }
          )
        }
        case it if (it startsWith "index-delete") => {
          //need to preserve the query case because T and Z mean things in dates
          val query = input.replaceFirst("index-delete ", "")
          val deletor = new QueryDelete(query)
          println("Delete from index using query : " + query)
          deletor.deleteFromIndex
        }
        case it if(it.startsWith("index-query")&& it.indexOf(" ")>0) => {
          val query = input.substring(input.indexOf(" ")).trim
          println("Reindexing based on query " + query)
          ReprocessIndexSelect.main(Array("-q", query , "--index"))
        }
        case it if (it.startsWith("index-file ") && input.split(" ").length >= 2) => {
          val args = input.split(" ").tail
          try {
            if (args.length == 2) {
              println("Indexing from " + args(0) + " using " + args(1) + " threads")
              IndexRecords.indexListThreaded(new File(args(0)), Integer.parseInt(args(1)))
            } else if (args.length == 1) {
              println("Indexing from " + args(0))
              IndexRecords.indexList(new File(args(0)), false)
            }
          } catch {
            case e: Exception => println(e.getMessage())
          }
        }
        case it if (it.startsWith("index-live ") && input.split(" ").length == 2) => {
          val dr = it.split(" ").map(x => x.trim).toList.last
          indexDataResourceLive(dr)
        }
        case it if (it startsWith "index-custom") => {
          if (it.split(" ").length > 2) {
            val (cmdAndDr, additionalFields) = input.split(" ").splitAt(2)
            IndexRecords.index(None, None, Some(cmdAndDr(1)), false, false, miscIndexProperties = additionalFields)
          }
        }
        case it if (it startsWith "force-index")=> {
          val drs = it.split(" ").map(x => x.trim).toList.tail
          drs.foreach(dr => IndexRecords.index(None, None, Some(dr), false, false))
        }
        case it if (it startsWith "index ") || (it startsWith "index") => {
          val drs = it.split(" ").map(x => x.trim).toList.tail
          drs.foreach(dr => {
            val (hasRowKeys, filename) = hasRowKey(dr)
            if (!hasRowKeys){
              IndexRecords.index(None, None, Some(dr), false, false)
            } else {
              IndexRecords.indexListThreaded(new File(filename.get), 4)
            }
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
        case it if (it startsWith "export-stream") => {
          val args = input.split(" ").map(x => x.trim).toArray.tail
          ExportFromIndexStream.main(args)
        }
        case it if (it startsWith "export-facet") => {
          val args = input.split(" ").map(x => x.trim).toArray.tail
          ExportFacet.main(args)
        }
        case it if (it startsWith "export-facet-query") => {
          val args = it.split(" ").map(x => x.trim).toArray.tail
          ExportByFacetQuery.main(args)
        }
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
          val dr = args(0)
          val (hasRowKeys, filename) = hasRowKey(dr)
          if (!hasRowKeys){
            Sampling.main(Array("-dr") ++ args)
          } else {
            Sampling.main(Array("-rf", filename.get, "-dr") ++ args)
          }
        }
        case it if (it startsWith "resample") => {
          println("****** Warning - this requires at least 8g of memory allocation -Xmx8g -Xms8g")
          val query = it.replaceFirst("resample", "")
          ResampleRecordsByQuery.main(Array(query))
        }
        case it if (it startsWith "download-media") => {
          val args = it.split(" ").map(x => x.trim).toArray.tail
          DownloadMedia.main(args)
        }
        case it if (it startsWith "tool") => {
          val args = it.split(" ").map(x => x.trim).toArray.tail
          DuplicationDetection.main(args)
        }
        case it if (it.startsWith("jackknife") || it.startsWith("jacknife")) => {
          val args = it.split(" ").map(x => x.trim).toArray.tail
          SpeciesOutlierTests.main(args)
        }
        case it if (it.startsWith("distribution-outliers") || it.startsWith("distribution-outliers")) => {
          val args = it.split(" ").map(x => x.trim).toArray.tail
          ExpertDistributionOutlierTool.main(args)
        }
        case it if (it.startsWith("endemism") || it.startsWith("endemism")) => {
          val args = it.split(" ").map(x => x.trim).toArray.tail
          CalculatedLayerHelper.main(args)
        }
        case it if (it startsWith "delete-obsolete") =>{
          val args = it.split(" ").map(x => x.trim).toArray.tail
          if(args.length>0){
            val dr = args(0)
            val test = args.length>1
            val extraArgs:Array[String] =  if(test) Array("--test") else Array()
            val (hasRowKeys, filename) = hasRowKey(dr)

            //delete columns that were not updated in the last day
            val lastLoadDate = org.apache.commons.lang.time.DateFormatUtils.format(getLastLoadDate, "yyyy-MM-dd")
            if (hasRowKeys){
              ResourceCleanupTask.main(Array(dr, "columns","-d",lastLoadDate, "-f" ,filename.get) ++ extraArgs)
            } else {
              ResourceCleanupTask.main(Array(dr, "columns","-d",lastLoadDate) ++ extraArgs)
            }
          } else {
            println("Unable to delete-obsolete without a data resource uid being provided")
          }
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
        case it if (it startsWith "mark-deleted") => {
          //need to construct the args that will be used
          val args = it.split(" ").map(x => x.trim).toArray.tail
          val delDate = org.apache.commons.lang.time.DateFormatUtils.format(getLastLoadDate, "yyyy-MM-dd")
          //when no date provided use today minus 1 day.
          val args2:Array[String] = if(args.size == 1) Array(args(0), "rows", "-d", delDate) else if(args.size == 2) Array(args(0), "rows" ,"-d", args(1)) else if(args.size>2) Array(args(0), "rows" ,"-d", args(1), "--test") else Array()

          ResourceCleanupTask.main(args2)
        }
        case it if (it startsWith "remove-deleted-index") => {
          val args = it.split(" ").map(x => x.trim).toArray.tail
          //generate a data that we are happy to delete from
          //TODO base this on the last load date in the collectory
          val delDate = BiocacheConversions.dateToString(getLastLoadDate)
          val query ="data_resource_uid:"+args(0)+ " AND last_load_date:[* TO " + delDate +"]"
          val deletor = new QueryDelete(query)
          println("Delete from index using query : " + query)
          deletor.deleteFromIndex
        }
        case it if (it startsWith "remove-deleted") =>{
          val args = it.split(" ").map(x => x.trim).toArray.tail
          if(args.size==1)
            ResourceCleanupTask.main(Array(args(0), "delete"))
        }
        case it if (it startsWith "delete-columns") =>{
          //first arg is data resource
          val args = input.split(" ").map(x => x.trim).toArray.tail
          if(args.length > 1){
            ResourceCleanupTask.main(Array(args.head)++Array("columns","--delcols","-c")++args.tail)
          }
          else
            println ("Need to supply a data resource and list of columns")
        }
        case it if(it startsWith "delete-missing") =>{
          //first arg is data resource
          val args = input.split(" ").map(x => x.trim).toArray.tail
          if(args.length > 1){
            ResourceCleanupTask.main(Array(args.head)++Array("columns","-c")++args.tail)
          }
          else
            println ("Need to supply a data resource and list of columns")
        }
        case it if (it startsWith "delete-inc") =>{
          //perform an incremental delete based on the rowkeys in the file
          val args = input.split(" ").tail
          //check to see if the file exists
          if(args.size>0){
            val file = getDeleteRowFile(args(0))
            if(file.isDefined){
              val fd = new FileDelete(file.get)
              fd.deleteFromPersistent
              fd.deleteFromIndex
            } else{
              println("There is no incremental delete file for " + args(0))
            }
          }
        }
        case it if (it startsWith "delete") => {
          //need to preserve the query case because T and Z mean things in dates
          val query = input.replaceFirst("delete ", "")
          val deletor = new QueryDelete(query)
          println("Delete from storage using the query: " + query)
          deletor.deleteFromPersistent()
          println("Delete from index")
          deletor.deleteFromIndex
        }
        case it if (it.startsWith("apply-aq") || it.startsWith("apply-validation")) => {
          val apiKey = input.replaceFirst("apply-aq ","").replaceFirst("apply-validation ", "")
          println("Applying the validation rules for the queries supplied by apiKey: " + apiKey)
          val qa = new ValidationRuleRunner()
          qa.apply(apiKey)
        }
        case _ => printHelp
      }
    } catch {
      //NC:2013-05-10: Need to re-throw the exception so that the script tool knows about it.
      case e: Exception => e.printStackTrace; if(throwException) throw e;
    }

    def indexDataResourceLive(dr:String){
      println("Indexing live with URL: " + Config.reindexUrl + ", and params: " + Config.reindexData + "&dataResource=" + dr)
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
  }

  def printHelp = {
    var s = 0
    padAndPrint(s"[${s=s+1;s}]  list - Print the list of resources available for harvesting")
    padAndPrint(s"[${s=s+1;s}]  describe <dr-uid1> <dr-uid2>... - Show the configuration or the resource")
    padAndPrint(s"[${s=s+1;s}]  load <dr-uid1> <dr-uid2>... - Load resource into biocache if the file has been modified after the lastCheckedDate (does not index)")
    padAndPrint(s"[${s=s+1;s}]  force-load <dr-uid1> <dr-uid2>... - Forces the resource to be loaded into biocache ignoring lastCheckedDate (does not index)")
    padAndPrint(s"[${s=s+1;s}]  process-single <uuid1> <uuid2> ... - Process single record (SDS/namematching)")
    padAndPrint(s"[${s=s+1;s}]  process <dr-uid1> <dr-uid2>... - Process resource")
    padAndPrint(s"[${s=s+1;s}]  process-all - Process all records (this takes a long time for full biocache)")
    padAndPrint(s"[${s=s+1;s}]  index <dr-uid1> <dr-uid2>... - Index resource (for offline use only)")
    padAndPrint(s"[${s=s+1;s}]  index-live <dr-uid> - Index resource by calling webservice to index. Dont use for large resources.")
    padAndPrint(s"[${s=s+1;s}]  index-custom <dr-uid> <list-of-misc-fields> - Index resource while indexing miscellanous properties.")
    padAndPrint(s"[${s=s+1;s}]  index-file <file to index> <numberof threads> - Indexes based on the rowKeys contained in the supplied file")
    padAndPrint(s"[${s=s+1;s}]  ingest <dr-uid> - Load, Sample, Process, Index a data resource")
    padAndPrint(s"[${s=s+1;s}]  ingest-all - Load, Sample, Process, Index all listed data resources. Not for production use.")
    padAndPrint(s"[${s=s+1;s}]  createdwc <dr-uid or 'all'> <export directory> - Create a darwin core archive for a resource")
    padAndPrint(s"[${s=s+1;s}]  healthcheck - Do a healthcheck on the configured resources in the collectory")
    padAndPrint(s"[${s=s+1;s}]  export - CSV export of data")
    padAndPrint(s"[${s=s+1;s}]  export-gbif-archives - Comma separated list of data resources or 'all'")
    padAndPrint(s"[${s=s+1;s}]  export-index <output-file> <csv-list-of fields> <solr-query> - export data from index")
    padAndPrint(s"[${s=s+1;s}]  export-stream <output-file> <csv-list-of fields> <solr-query> - export data from index")
    padAndPrint(s"[${s=s+1;s}]  export-facet <facet-field> <facet-output-file> -fq <filter-query> - export data from index")
    padAndPrint(s"[${s=s+1;s}]  export-facet-query <facet-field> <facet-output-file> -fq <filter-query> - export data from index")
    padAndPrint(s"[${s=s+1;s}]  export-for-outliers <index-directory> <export-directory> -fq <filter-query> - export data from index for outlier detection")
    padAndPrint(s"[${s=s+1;s}]  import - CSV import of data")
    padAndPrint(s"[${s=s+1;s}]  optimise - Optimisation of SOLR index (this takes some time)")
    padAndPrint(s"[${s=s+1;s}]  sample-all - Run geospatial sampling for all records")
    padAndPrint(s"[${s=s+1;s}]  sample <dr-uid1> <dr-uid2>... - Run geospatial sampling for records for a data resource")
    padAndPrint(s"[${s=s+1;s}]  resample <query> - Rerun geospatial sampling for records that match a SOLR query")
    padAndPrint(s"[${s=s+1;s}]  delete <solr-query> - Delete records matching a query")
    padAndPrint(s"[${s=s+1;s}]  delete-resource <dr-uid1> <dr-uid2>... - Delete records for a resource. Requires a index reopen (http get on /ws/admin/modify?reopenIndex=true)")
    padAndPrint(s"[${s=s+1;s}]  index-delete <query> - Delete record that satisfies the supplied query from the index ONLY")
    padAndPrint(s"[${s=s+1;s}]  delete-inc <dr> - checks for and will delete all the records in an incremental delete file. This file is created during a load phase if applicable")
    padAndPrint(s"[${s=s+1;s}]  load-local-csv <dr-uid> <filepath>... - Load a local file into biocache. For development use only. Not to be used in production.")
    padAndPrint(s"[${s=s+1;s}]  test-load <dr-uid1> <dr-uid2>... - Performs some testing on the load process.  Please read the output to determine whether or not a load should proceed.")
    padAndPrint(s"[${s=s+1;s}]  download-media - Force the (re)download of media associated with a resource.")
    padAndPrint(s"[${s=s+1;s}]  tool - Run duplication detection over the records.")
    padAndPrint(s"[${s=s+1;s}]  jackknife - Run jackknife outlier detection.")
    padAndPrint(s"[${s=s+1;s}]  distribution outliers -l <speciesLsid> - Run expert distribution outlier detection. If species LSID is supplied, outlier detection is only performed for occurrences of the species with the supplied taxon concept LSID")
    padAndPrint(s"[${s=s+1;s}]  apply-validation <apiKey> - applies the validation rules for the supplied apiKey")
    padAndPrint(s"[${s=s+1;s}]  mark-deleted <dr-uid> <date of last load YYYY-MM-DD> - Marks records as deleted in the data store when that have not been updated on the last load. - only run if a complete data set was loaded.")
    padAndPrint(s"[${s=s+1;s}]  remove-deleted <dr-uid> - removes all records from the data-store that have been marked as deleted")
    padAndPrint(s"[${s=s+1;s}]  delete-columns <dr-uid> <list of columns> - deletes all the columns specified in the space separated list")
    padAndPrint(s"[${s=s+1;s}]  delete-missing <dr-uid> <list of columns> - deletes all the columns that are not specified in the space separated list")
    padAndPrint(s"[${s=s+1;s}]  remove-deleted-index <dr-uid> - removes the records not loaded during the last load from index (NB last load will be interpreted as within the last 24 hours).")
    padAndPrint(s"[${s=s+1;s}]  force-index <dr-uid1> <dr-uid2>  - forces a complete reindex of the supplied data resource (ignoring incremental file)")
    padAndPrint(s"[${s=s+1;s}]  index-query <query to index> - Indexes all the records that satisfy the supplied query")
    padAndPrint(s"[${s=s+1;s}]  delete-obsolete <dr> - deletes the obsolete columns based on a last load date within the last 24 hours")
    padAndPrint(s"[${s=s+1;s}]  exit")
  }

  def getLastLoadDate : java.util.Date = {
    //At the moment return the current date minus 24 hours.
    //TODO obtain this from the last checked or data currency dates. - But not always updated and we don't want to lose data
    org.apache.commons.lang.time.DateUtils.addDays(new java.util.Date(), -1)
  }

  private def getDeleteRowFile(resourceUid:String) : Option[String] = {
    def filename = Config.deletedFileStore + File.separator + resourceUid + File.separator + "deleted.txt"
    if(new File(filename).exists()){
      Some(filename)
    } else {
      None
    }
  }

  def hasRowKey(resourceUid: String): (Boolean, Option[String]) = {
    def filename = "/data/tmp/row_key_" + resourceUid + ".csv"
    def file = new java.io.File(filename)

    if (file.exists()) {
      val date = new java.util.GregorianCalendar()
      date.setTime(new java.util.Date)
      date.add(java.util.Calendar.HOUR, -24)
      //if it is on the same day assume that we want the incremental process or index.
      if (org.apache.commons.io.FileUtils.isFileNewer(file, date.getTime())){
        (true, Some(filename))
      } else {
        //prompt the user
        println("There is an incremental row key file for this resource.  Would you like to perform an incremental process (y/n) ?")
        val answer = readLine
        if (answer.toLowerCase().equals("y") || answer.toLowerCase().equals("yes")) {
          (true, Some(filename))
        } else {
          (false, None)
        }
      }
    } else {
      (false, None)
    }
  }

  def padAndPrint(str: String) = println(padElementTo60(str))

  def padElementTo60(str: String) = padElement(str, 60)

  def padElement(str: String, width: Int) = {
    val indexOfHyphen = str.indexOf(" -")
    str.replace(" - ", Array.fill(width - indexOfHyphen)(' ').mkString + " - ")
  }

  def printTable(table: List[Map[String, String]]) {

    val keys = table(0).keys.toList
    val valueLengths = keys.map(k => {
      (k, table.map(x => x(k).length).max)
    }).toMap[String, Int]
    val columns = table(0).keys.map(k => {
      if (k.length < valueLengths(k)) {
        k + List.fill[String](valueLengths(k) - k.length)(" ").mkString
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
          kv._2 + List.fill[String](valueLengths(kv._1) - kv._2.length)(" ").mkString
        } else {
          kv._2
        }
      }).mkString(" | ", " | ", " |"))
    })

    println(" " + List.fill[String](columns.length - 1)("-").mkString)
  }
}
