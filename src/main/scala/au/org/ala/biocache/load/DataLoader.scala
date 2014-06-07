package au.org.ala.biocache.load

import au.org.ala.biocache.util.{SFTPTools, FileHelper, BiocacheConversions}
import org.slf4j.LoggerFactory
import au.org.ala.biocache.Config
import org.apache.commons.io.{FilenameUtils, FileUtils}
import java.io.{FileOutputStream, File}
import scala.io.Source
import scala.util.parsing.json.JSON
import java.util.Date
import au.org.ala.biocache.parser.DateParser
import org.gbif.dwc.terms.TermFactory
import scala.collection.mutable.ArrayBuffer
import scalaj.http.Http
import au.org.ala.biocache.model.FullRecord

/**
 * A trait with utility code for loading data
 */
trait DataLoader {

  import BiocacheConversions._
  import FileHelper._

  val user = "harvest services"
    val api_key = "Venezuela"
    val logger = LoggerFactory.getLogger("DataLoader")
    val temporaryFileStore = Config.loadFileStore //"/data/biocache-load/"
    val pm = Config.persistenceManager
    val loadTime = org.apache.commons.lang.time.DateFormatUtils.format(new java.util.Date, "yyyy-MM-dd'T'HH:mm:ss'Z'")

    def emptyTempFileStore(resourceUid:String) = FileUtils.deleteQuietly(new File(temporaryFileStore + File.separator + resourceUid))

  /**
   * Returns the file writer to be used to store the row keys that need to be deleted for a data resource
   * @param resourceUid
   * @return
   */
    def getDeletedFileWriter(resourceUid:String):java.io.FileWriter ={
      val file =  new File(Config.deletedFileStore +File.separator + resourceUid+File.separator+"deleted.txt")
      FileUtils.forceMkdir(file.getParentFile)
      new java.io.FileWriter(file)
    }

    def deleteOldRowKeys(resourceUid:String){
      //delete the row key file so that it only exists if the load is configured to
      //thus processing and indexing of the data resource should check to see if a file exists first
      FileUtils.deleteQuietly(new File("/data/tmp/row_key_"+resourceUid+".csv"))
    }

    def getRowKeyWriter(resourceUid:String, writeRowKeys:Boolean):Option[java.io.Writer]={
      if(writeRowKeys){
        FileUtils.forceMkdir(new File("/data/tmp/"))
        //the file is deleted first so we set it up to append.  allows resources with multiple files to have row keys recorded
        Some(new java.io.FileWriter("/data/tmp/row_key_"+resourceUid+".csv", true))
      }
      else None
    }

    //Sampling, Processing and INdexing look for the row key file.
    // An empty file should be enough to prevent the phase from going ahead...
    //TODO We should probably change this to be more robust.
    def setNotLoadedForOtherPhases(resourceUid:String){
      def writer = getRowKeyWriter(resourceUid, true)
      if(writer.isDefined){
        writer.get.flush
        writer.get.close
      }
    }

    def getDataResourceDetailsAsMap(resourceUid:String) : Map[String, String] = {
      val json = Source.fromURL(Config.registryUrl + "/dataResource/" + resourceUid + ".json").getLines.mkString
      JSON.parseFull(json).get.asInstanceOf[Map[String, String]]
    }

    def getDataProviderDetailsAsMap(uid:String) : Map[String, String] = {
      val json = Source.fromURL(Config.registryUrl + "/dataProvider/" + uid + ".json").getLines.mkString
      JSON.parseFull(json).get.asInstanceOf[Map[String, String]]
    }

    def getInstitutionDetailsAsMap(uid:String) : Map[String, String] = {
      val json = Source.fromURL(Config.registryUrl + "/institution/" + uid + ".json").getLines.mkString
      JSON.parseFull(json).get.asInstanceOf[Map[String, String]]
    }

    def retrieveConnectionParameters(resourceUid: String) : (String, List[String], List[String], Map[String,String], Map[String,String],Option[Date]) = {

      //full document
      val map = getDataResourceDetailsAsMap(resourceUid)

      //connection details
      val connectionParameters = map("connectionParameters").asInstanceOf[Map[String,AnyRef]]
      val protocol = connectionParameters("protocol").asInstanceOf[String]
      val urlsObject = connectionParameters.getOrElse("url", List[String]())
      val urls = {
        if(urlsObject.isInstanceOf[List[_]]){
          urlsObject
        } else {
          val singleValue = connectionParameters("url").asInstanceOf[String]
          List(singleValue)
        }
      }

      val uniqueTerms = connectionParameters.getOrElse("termsForUniqueKey", List[String]()).asInstanceOf[List[String]]

      //optional config params for custom services
      val customParams = protocol.asInstanceOf[String].toLowerCase match {
          case "customwebservice" => {
            val params = connectionParameters.getOrElse("params", "").asInstanceOf[String]
            JSON.parseFull(params).getOrElse(Map[String,String]()).asInstanceOf[Map[String, String]]
          }
          case _ => Map[String,String]()
      }
      //last checked date
      val lastChecked = map("lastChecked").asInstanceOf[String]
      val dateLastChecked = DateParser.parseStringToDate(lastChecked)
      (protocol, urls.asInstanceOf[List[String]], uniqueTerms, map("connectionParameters").asInstanceOf[Map[String,String]], customParams,dateLastChecked)
    }

    def mapConceptTerms(terms: List[String]): List[org.gbif.dwc.terms.Term] = {
      val termFactory =  TermFactory.instance()
      terms.map(term => termFactory.findTerm(term))
    }

    def exists(dataResourceUid:String, identifyingTerms:List[String]) : Boolean = {
      !Config.occurrenceDAO.getUUIDForUniqueID(createUniqueID(dataResourceUid, identifyingTerms)).isEmpty
    }

    protected def createUniqueID(dataResourceUid:String,identifyingTerms:List[String], stripSpaces:Boolean=false) : String = {
      val uniqueId =(List(dataResourceUid) ::: identifyingTerms).mkString("|").trim
      if(stripSpaces)
        uniqueId.replaceAll("\\s","")
      else
        uniqueId
    }

    def load(dataResourceUid:String, fr:FullRecord, identifyingTerms:List[String]) : Boolean = {
       load(dataResourceUid:String, fr:FullRecord, identifyingTerms:List[String], true, false, false, None)
    }

    def load(dataResourceUid:String, fr:FullRecord, identifyingTerms:List[String], updateLastModified:Boolean) : Boolean = {
      load(dataResourceUid:String, fr:FullRecord, identifyingTerms:List[String], updateLastModified, false, false, None)
    }

    def load(dataResourceUid:String, fr:FullRecord, identifyingTerms:List[String], updateLastModified:Boolean, downloadMedia:Boolean):Boolean ={
      load(dataResourceUid, fr, identifyingTerms, updateLastModified, downloadMedia, false, None)
    }

    def load(dataResourceUid:String, fr:FullRecord, identifyingTerms:List[String], updateLastModified:Boolean, downloadMedia:Boolean, stripSpaces:Boolean, rowKeyWriter:Option[java.io.Writer]) : Boolean = {

      //the details of how to construct the UniqueID belong in the Collectory
      val uniqueID = if(identifyingTerms.isEmpty) None else Some(createUniqueID(dataResourceUid,identifyingTerms,stripSpaces))

      //lookup the column
      val (recordUuid, isNew) = {
        if(fr.uuid != null && fr.uuid.trim != ""){
          (fr.uuid, false)
        } else {
          uniqueID match {
            case Some(value) => Config.occurrenceDAO.createOrRetrieveUuid(value)
            case None => (Config.occurrenceDAO.createUuid, true)
          }
        }
      }

      //add the full record
      fr.uuid = recordUuid
      //The row key is the uniqueID for the record. This will always start with the dataResourceUid
      fr.rowKey = if(uniqueID.isEmpty) dataResourceUid +"|"+recordUuid else uniqueID.get
      //write the rowkey to file if a writer is provided. allows large data resources to be incrementally updated and only process/index changes
      if(rowKeyWriter.isDefined){
        rowKeyWriter.get.write(fr.rowKey+"\n")
      }
      //The last load time
      if(updateLastModified){
        fr.lastModifiedTime = loadTime
      }
      if(isNew){
        fr.firstLoaded = loadTime
      }
      fr.attribution.dataResourceUid = dataResourceUid

      //download the media - checking if it exists already
      if (fr.occurrence.associatedMedia != null){
        val filesToImport = fr.occurrence.associatedMedia.split(";")
        val associatedMediaBuffer = new ArrayBuffer[String]
        filesToImport.foreach(fileToStore => {
          val (filePath, exists) = MediaStore.exists(fr.uuid, dataResourceUid, fileToStore)
          if (!exists){
            MediaStore.save(fr.uuid, fr.attribution.dataResourceUid, fileToStore)
          }
          associatedMediaBuffer += filePath
        })
        fr.occurrence.associatedMedia = associatedMediaBuffer.toArray.mkString(";")
      }

      Config.occurrenceDAO.addRawOccurrence(fr)
      true
    }

    def downloadArchive(url:String, resourceUid:String, lastChecked:Option[Date]) : (String,Date) = {
      //when the url starts with SFTP need to SCP the file from the supplied server.
      val (file,date,isZipped,isGzipped) ={
        if(url.startsWith("sftp://"))
          downloadSecureArchive(url,resourceUid,lastChecked)
        else
          downloadStandardArchive(url, resourceUid,lastChecked)
      }
      if(file != null){
      //extract the file
        if (isZipped){
          logger.info("Extracting ZIP " + file.getAbsolutePath)
          file.extractZip
          val fileName = FilenameUtils.removeExtension(file.getAbsolutePath)
          logger.info("Archive extracted to directory: " + fileName)
          (fileName,date)
        } else if (isGzipped){
          logger.info("Extracting GZIP " + file.getAbsolutePath)
          file.extractGzip
          //need to remove the gzip file so the loader doesn't attempt to load it.
          FileUtils.forceDelete(file)
          val fileName = FilenameUtils.removeExtension(file.getAbsolutePath)
          logger.info("Archive extracted to directory: " + fileName)
          ((new File(fileName)).getParentFile.getAbsolutePath,date)
        } else {
          (file.getParentFile.getAbsolutePath,date)
        }
      } else {
        logger.info("Unable to extract a new file for " +resourceUid + " at " + url)
        (null,null)
      }
    }

    val sftpPattern = """sftp://([a-zA-z\.]*):([0-9a-zA-Z_/\.\-]*)""".r

    def downloadSecureArchive(url:String, resourceUid:String, lastChecked:Option[Date]) : (File, Date,Boolean,Boolean) = {
      url match {
        case sftpPattern(server,filename) => {
          val (targetfile,date, isZipped, isGzipped,downloaded) = {
          if (url.endsWith(".zip") ){
            val f = new File(temporaryFileStore + resourceUid + ".zip")
            f.createNewFile()
            (f,null, true, false,false)
          } else if (url.endsWith(".gz")){
            val f = new File(temporaryFileStore + resourceUid + File.separator + resourceUid +".gz")
            logger.info("  creating file: " + f.getAbsolutePath)
            FileUtils.forceMkdir(f.getParentFile())
            f.createNewFile()
            (f,null, false, true,false)
          } else if (filename.contains(".")) {
            val f = new File(temporaryFileStore + resourceUid + File.separator + resourceUid +".csv")
            logger.info("  creating file: " + f.getAbsolutePath)
            FileUtils.forceMkdir(f.getParentFile())
            f.createNewFile()
            (f,null, false, false,false)
          } else {
            logger.info("SFTP the most recent from " + url)
            def fileDetails = SFTPTools.sftpLatestArchive(url, resourceUid, temporaryFileStore,lastChecked)
            if(fileDetails.isDefined){
              val (file, date) = fileDetails.get
              logger.info("The most recent file is " + file + " with last modified date : " + date)
              (new File(file),date,file.endsWith("zip"),file.endsWith("gz"),true)
            }
            else
              (null,null,false,false,false)
          }}

          val fileDetails = if(targetfile == null) None else if(!downloaded)SFTPTools.scpFile(server,Config.getProperty("uploadUser"), Config.getProperty("uploadPassword"),filename,targetfile) else Some((targetfile,date))
          if(fileDetails.isDefined){
            val (file, date) = fileDetails.get
            (targetfile,date,isZipped,isGzipped)
          }
          else
            (null,null,false,false)
        }
        case _ => (null,null,false,false)
      }
    }

    def sftpLatestArchive(url:String, resourceUid:String, afterDate:Option[Date]):Option[(String,Date)]={
      SFTPTools.sftpLatestArchive(url, resourceUid, temporaryFileStore,afterDate)
    }

    def downloadStandardArchive(url:String, resourceUid:String, afterDate:Option[Date]) : (File,Date,Boolean,Boolean) = {
      val tmpStore = new File(temporaryFileStore)
      if(!tmpStore.exists){
        FileUtils.forceMkdir(tmpStore)
      }

      logger.info("Downloading zip file from "+ url)
      val urlConnection = new java.net.URL(url.replaceAll(" " ,"%20")).openConnection()
      val date = if(urlConnection.getLastModified() == 0) new Date() else new Date(urlConnection.getLastModified())
      //logger.info("URL Last Modified: " +urlConnection.getLastModified())
      if(afterDate.isEmpty || urlConnection.getLastModified() ==0 || afterDate.get.getTime() < urlConnection.getLastModified()){
        //handle the situation where the files name is not supplied in the URL but in the Content-Disposition
        val contentDisp = urlConnection.getHeaderField("Content-Disposition")
        if(contentDisp != null){
            logger.info(" Content-Disposition: " + contentDisp)
        }
        val in = urlConnection.getInputStream()
        val (file, isZipped, isGzipped) = {
          if (url.endsWith(".zip") || (contentDisp != null && contentDisp.endsWith(""".zip""""))){
            val f = new File(temporaryFileStore + resourceUid + ".zip")
            f.createNewFile()
            (f, true, false)
          } else if (url.endsWith(".gz") || (contentDisp != null && contentDisp.endsWith(""".gz""""))){
            val f = new File(temporaryFileStore + resourceUid + File.separator + resourceUid +".gz")
            logger.info("  creating file: " + f.getAbsolutePath)
            FileUtils.forceMkdir(f.getParentFile())
            f.createNewFile()
            (f, false, true)
          } else {
            val f = new File(temporaryFileStore + resourceUid + File.separator + resourceUid +".csv")
            logger.info("  creating file: " + f.getAbsolutePath)
            FileUtils.forceMkdir(f.getParentFile())
            f.createNewFile()
            (f, false, false)
          }
        }
        val out = new FileOutputStream(file)
        val buffer: Array[Byte] = new Array[Byte](40960)
        var numRead = 0
        var counter = 0
        while ({ numRead = in.read(buffer); numRead != -1 }) {
          counter += numRead
          out.write(buffer, 0, numRead)
          out.flush
        }
        out.flush
        in.close
        out.close
        logger.info("\nDownloaded. File size: ", counter / 1024 +"kB, " + file.getAbsolutePath +", is zipped: " + isZipped+"\n")
        (file,date,isZipped,isGzipped)
      } else {
        logger.info("\nThe file has not changed since the last time it  was loaded.  To load the data a force-load  will need to be performed")
        (null,null,false,false)
      }
    }

    /**
     * Calls the collectory webservice to update the last loaded time for a data resource
     */
    def updateLastChecked(resourceUid:String,dataCurrency:Option[Date]=None) :Boolean ={
        try {
          //set the last check time for the supplied resourceUid only if configured to allow updates
          if(Config.allowCollectoryUpdates == "true"){
            val map =new  scala.collection.mutable.HashMap[String,String]()
            map ++= Map("user"-> user, "api_key"-> api_key, "lastChecked" ->loadTime)
            if(dataCurrency.isDefined)
              map += ("dataCurrency" -> dataCurrency.get)
            //turn the map of values into JSON representation
            val data = map.map(pair => "\""+pair._1 +"\":\"" +pair._2 +"\"").mkString("{",",", "}")

            val responseCode = Http.postData(Config.registryUrl + "/dataResource/" +resourceUid,data).header("content-type", "application/json").responseCode
            logger.info("Registry response code: " + responseCode)
          }
          true
        } catch {
          case e:Exception => e.printStackTrace();false
       }
    }
}
