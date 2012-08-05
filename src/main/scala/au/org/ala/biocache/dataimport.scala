package au.org.ala.biocache
import scala.io.Source
import scala.util.parsing.json.JSON
import org.gbif.dwc.terms.TermFactory
import org.gbif.dwc.terms.ConceptTerm
import java.io.File
import org.apache.commons.io.FileUtils
import java.io.FileOutputStream
import au.org.ala.util.FileHelper
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import scalaj.http.Http
import collection.mutable.ArrayBuffer
import collection.JavaConversions
import com.jcraft.jsch.JSch
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.ChannelSftp._
import java.io.FileInputStream
import com.jcraft.jsch.Session
import org.rev6.scf._
import org.apache.commons.lang3.StringUtils


class SimpleLoader extends DataLoader

class MapDataLoader extends DataLoader{
  import JavaConversions._
  def load(dataResourceUid:String, values:List[java.util.Map[String,String]], uniqueTerms:List[String]):List[String]={
    val rowKeys = new ArrayBuffer[String]
    values.foreach(jmap =>{
        val map = jmap.toMap[String,String]
        val uniqueTermsValues = uniqueTerms.map(t => map.getOrElse(t,""))
        val fr = FullRecordMapper.createFullRecord("", map, Versions.RAW)
        load(dataResourceUid, fr, uniqueTermsValues)
        rowKeys + fr.rowKey
    })
    rowKeys.toList
  }
}

/**
 * A trait with utility code for loading data
 */
trait DataLoader {
    
    import FileHelper._
    val user = "harvest services"
    val api_key = "Venezuela"
    val logger = LoggerFactory.getLogger("DataLoader")
    val temporaryFileStore = "/data/biocache-load/"
    val registryUrl = "http://collections.ala.org.au/ws/dataResource/"
    val pm = Config.persistenceManager
    val loadTime = org.apache.commons.lang.time.DateFormatUtils.format(new java.util.Date, "yyyy-MM-dd'T'HH:mm:ss'Z'")
    
    def emptyTempFileStore(resourceUid:String)=FileUtils.deleteQuietly(new File(temporaryFileStore+File.separator+resourceUid))
    
    def getDataResourceDetailsAsMap(resourceUid:String) : Map[String, String] = {
      val json = Source.fromURL(registryUrl + resourceUid + ".json").getLines.mkString
      JSON.parseFull(json).get.asInstanceOf[Map[String, String]]
    }
    def getTestDRAsMap(resourceUid:String) :Map[String,String] ={
      val json ="""{

    "name": "Australia's Virtual Herbarium",
    "acronym": "AVH",
    "uid": "dr376",
    "guid": "urn:lsid:biocol.org:col:15596",
    "address": {
        "street": null,
        "city": null,
        "state": null,
        "postcode": null,
        "country": "Australia",
        "postBox": null
    },
    "phone": null,
    "email": null,
    "pubDescription": "Australia's Virtual Herbarium records - AVH specimen and observation records",
    "techDescription": null,
    "focus": null,
    "state": null,
    "websiteUrl": "http://chah.gov.au/avh/",
    "alaPublicUrl": "http://collections.ala.org.au/public/show/dr376",
    "networkMembership": [
        {
            "name": "Council of Heads of Australasian Herbaria",
            "acronym": "CHAH",
            "logo": "http://collections.ala.org.au/data/network/CHAH_logo_col_70px_white.gif"
        }
    ],
    "hubMembership": [
        {
            "uid": "dh2",
            "name": "Australia's Virtual Herbarium",
            "uri": "http://collections.ala.org.au/ws/dataHub/dh2"
        }
    ],
    "taxonomyCoverageHints": [
        {
            "kingdom": "plantae"
        },
        {
            "kingdom": "fungi"
        },
        {
            "kingdom": "chromista"
        },
        {
            "kingdom": "protozoa"
        },
        {
            "kingdom": "bacteria"
        }
    ],
    "attributions": [ ],
    "dateCreated": "2010-10-26T23:54:15Z",
    "lastUpdated": "2012-05-02T23:07:06Z",
    "userLastModified": "David.Martin@csiro.au",
    "provider": {
        "name": "Australia's Virtual Herbarium",
        "uri": "http://collections.ala.org.au/ws/dataProvider/dp36",
        "uid": "dp36"
    },
    "rights": "You acknowledge that the Data may contain errors and omissions and that you employ the Data at your own risk. Neither the Council of Heads of Australasian Herbaria nor its member Herbaria nor any other Data Custodian will accept liability for any loss, damage, cost or expenses that you may incur as a result of the use of or reliance upon the Data. Note that the Data are continuously updated, corrected and added to. You should use the current Data from the AVH portal and not rely on material you have previously printed or downloaded.",
    "licenseType": "CC BY",
    "licenseVersion": "3.0",
    "citation": "You must attribute the source of these Data as: Australia's Virtual Herbarium, a resource of the Council of Heads of Australasian Herbaria and its member Herbaria listed at www.chah.gov.au.\r\n\r\nYou should cite the data as: The Council of Heads of Australasian Herbaria (1999-) Australia's Virtual Herbarium www.chah.gov.au/avh [Accessed <date of access>]",
    "resourceType": "records",
    "dataGeneralizations": null,
    "informationWithheld": null,
    "permissionsDocument": "http://www2.ala.org.au/datasets/occurrence/dr376/license/ALA re AVH data licensing.pdf",
    "permissionsDocumentType": "Email",
    "contentTypes": [
        "point occurrence data"
    ],
    "linkedRecordConsumers": [
        {
            "name": "State Herbarium of South Australia",
            "uri": "http://collections.ala.org.au/ws/collection/co48",
            "uid": "co48"
        },
        {
            "name": "National Herbarium of Victoria",
            "uri": "http://collections.ala.org.au/ws/collection/co55",
            "uid": "co55"
        },
        {
            "name": "National Herbarium of New South Wales",
            "uri": "http://collections.ala.org.au/ws/collection/co54",
            "uid": "co54"
        },
        {
            "name": "Queensland Herbarium ",
            "uri": "http://collections.ala.org.au/ws/collection/co49",
            "uid": "co49"
        },
        {
            "name": "Western Australian Herbarium",
            "uri": "http://collections.ala.org.au/ws/collection/co75",
            "uid": "co75"
        },
        {
            "name": "Australian National Herbarium",
            "uri": "http://collections.ala.org.au/ws/collection/co12",
            "uid": "co12"
        },
        {
            "name": "Tasmanian Herbarium",
            "uri": "http://collections.ala.org.au/ws/collection/co60",
            "uid": "co60"
        },
        {
            "name": "Northern Territory Herbarium",
            "uri": "http://collections.ala.org.au/ws/collection/co25",
            "uid": "co25"
        },
        {
            "name": "Northern Territory Herbarium (Alice Springs)",
            "uri": "http://collections.ala.org.au/ws/collection/co24",
            "uid": "co24"
        }
    ],
    "connectionParameters": {
        "protocol": "DwC",
        "csv_text_enclosure": "\"",
        "termsForUniqueKey": [
            "institutionCode",
            "catalogNumber"
        ],
        "csv_eol": "\n",
        "csv_delimiter": "\\t",
        "automation": false,
        "strip": true,
        "csv_escape_char": "|",
        "url": ["sftp://upload.ala.org.au:avh/AVH_CNS.zip"]
    },
    "defaultDarwinCoreValues": { },
    "hasMappedCollections": true,
    "status": "dataAvailable",
    "provenance": "Published dataset",
    "harvestFrequency": 0,
    "lastChecked": null,
    "dataCurrency": null,
    "harvestingNotes": null,
    "publicArchiveAvailable": false,
    "publicArchiveUrl": "http://biocache.ala.org.au/archives/dr376/dr376_ror_dwca.zip",
    "downloadLimit": 0

}"""
        JSON.parseFull(json).get.asInstanceOf[Map[String, String]]
    }
    def getDataProviderDetailsAsMap(uid:String) : Map[String, String] = {
      val json = Source.fromURL("http://collections.ala.org.au/ws/dataProvider/" + uid + ".json").getLines.mkString
      JSON.parseFull(json).get.asInstanceOf[Map[String, String]]
    }

    def getInstitutionDetailsAsMap(uid:String) : Map[String, String] = {
      val json = Source.fromURL("http://collections.ala.org.au/ws/institution/" + uid + ".json").getLines.mkString
      JSON.parseFull(json).get.asInstanceOf[Map[String, String]]
    }

    def retrieveConnectionParameters(resourceUid: String) : (String, List[String], List[String], Map[String,String], Map[String,String]) = {

      //full document
      val map = getDataResourceDetailsAsMap(resourceUid)

      //connection details
      val connectionParameters = map("connectionParameters").asInstanceOf[Map[String,AnyRef]]
      val protocol = connectionParameters("protocol").asInstanceOf[String]
      val urlsObject = connectionParameters.getOrElse("url", List[String]())
      val urls = {
        if(urlsObject.isInstanceOf[List[String]]){
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
      (protocol, urls.asInstanceOf[List[String]], uniqueTerms, map("connectionParameters").asInstanceOf[Map[String,String]], customParams)
    }
    
    def mapConceptTerms(terms: List[String]): List[org.gbif.dwc.terms.ConceptTerm] = {
      val termFactory = new TermFactory  
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
       load(dataResourceUid:String, fr:FullRecord, identifyingTerms:List[String], true, false,false,None)
    }

    def load(dataResourceUid:String, fr:FullRecord, identifyingTerms:List[String], updateLastModified:Boolean) : Boolean = {
      load(dataResourceUid:String, fr:FullRecord, identifyingTerms:List[String], updateLastModified, false,false,None)
    }
    
    def load(dataResourceUid:String, fr:FullRecord, identifyingTerms:List[String], updateLastModified:Boolean, downloadMedia:Boolean):Boolean ={
      load(dataResourceUid, fr, identifyingTerms, updateLastModified, downloadMedia, false,None)
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
        if(rowKeyWriter.isDefined)
          rowKeyWriter.get.write(fr.rowKey+"\n")
        
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
    
    def downloadArchive(url:String, resourceUid:String) : String = {
      //when the url starts with SFTP need to SCP the file from the supplied server.
      val (file,isZipped,isGzipped) ={
        if(url.startsWith("sftp://"))      
          downloadSecureArchive(url,resourceUid)
        else
          downloadStandardArchive(url, resourceUid)
      }
      //extract the file
        if (isZipped){
          println("Extracting ZIP " + file.getAbsolutePath)
          file.extractZip
          val fileName = FilenameUtils.removeExtension(file.getAbsolutePath)
          println("Archive extracted to directory: " + fileName)
          fileName
        } else if (isGzipped){
          println("Extracting GZIP " + file.getAbsolutePath)
          file.extractGzip
          //need to remove the gzip file so the loader doesn't attempt to load it.
          FileUtils.forceDelete(file)
          val fileName = FilenameUtils.removeExtension(file.getAbsolutePath)
          println("Archive extracted to directory: " + fileName)
          (new File(fileName)).getParentFile.getAbsolutePath
        } else {
          file.getParentFile.getAbsolutePath
        }
      
    }
    val sftpPattern = """sftp://([a-zA-z\.]*):([a-zA-Z_/\.]*)""".r
    def downloadSecureArchive(url:String, resourceUid:String) : (File,Boolean,Boolean) = {
      url match{
        case sftpPattern(server,filename)=>{
          val (targetfile, isZipped, isGzipped) = {
          if (url.endsWith(".zip") ){
            val f = new File(temporaryFileStore + resourceUid + ".zip")
            f.createNewFile()
            (f, true, false)
          } else if (url.endsWith(".gz")){
            val f = new File(temporaryFileStore + resourceUid + File.separator + resourceUid +".gz")
            println("  creating file: " + f.getAbsolutePath)
            FileUtils.forceMkdir(f.getParentFile())
            f.createNewFile()
            (f, false, true)
          } else {
            val f = new File(temporaryFileStore + resourceUid + File.separator + resourceUid +".csv")
            println("  creating file: " + f.getAbsolutePath)
            FileUtils.forceMkdir(f.getParentFile())
            f.createNewFile()
            (f, false, false)
          }}
          val file = scpFile(server,Config.getProperty("uploadUser"), Config.getProperty("uploadPassword"),filename,targetfile)          
          (if(file.isDefined)targetfile else null,isZipped,isGzipped)
        }
        case _ => (null,false,false)
      }
      
    }
    //SCP the remote file from the supplied host into localFile
    def scpFile(host:String, user:String, password:String, remoteFile:String, localFile:File):Option[String]={
      if(StringUtils.isEmpty(user) || StringUtils.isEmpty(password))
        logger.error("SCP User or password has not been supplied. Please supply as part of the biocache.properties")
      var  ssh:SshConnection = null
      try{
        ssh= new SshConnection(host,user,password)
        ssh.connect()
        
        FileUtils.forceMkdir(localFile.getParentFile())
        val scpFile = new ScpFile(localFile, remoteFile)      
        ssh.executeTask(new ScpDownload(scpFile))
        Some(localFile.getAbsolutePath())
      }catch{
         case e:Exception => logger.error("Unable to SCP " + remoteFile ,e);None
       }
       finally{
         if(ssh != null)
           ssh.disconnect()
         None
       }
        
        
      }
    def downloadStandardArchive(url:String, resourceUid:String) : (File,Boolean,Boolean) = {
        val tmpStore = new File(temporaryFileStore)
        if(!tmpStore.exists){
        	FileUtils.forceMkdir(tmpStore)
        }

        print("Downloading zip file from "+ url)
        val urlConnection = new java.net.URL(url.replaceAll(" " ,"%20")).openConnection()
        //handle the situation where the files name is not supplied in the URL but in the Content-Disposition
        val contentDisp = urlConnection.getHeaderField("Content-Disposition");
        if(contentDisp != null)
            println(" Content-Disposition: " + contentDisp)
        val in = urlConnection.getInputStream()
        val (file, isZipped, isGzipped) = {
          if (url.endsWith(".zip") || (contentDisp != null && contentDisp.endsWith(""".zip""""))){
            val f = new File(temporaryFileStore + resourceUid + ".zip")
            f.createNewFile()
            (f, true, false)
          } else if (url.endsWith(".gz") || (contentDisp != null && contentDisp.endsWith(""".gz""""))){
            val f = new File(temporaryFileStore + resourceUid + File.separator + resourceUid +".gz")
            println("  creating file: " + f.getAbsolutePath)
            FileUtils.forceMkdir(f.getParentFile())
            f.createNewFile()
            (f, false, true)
          } else {
            val f = new File(temporaryFileStore + resourceUid + File.separator + resourceUid +".csv")
            println("  creating file: " + f.getAbsolutePath)
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

        printf("\nDownloaded. File size: ", counter / 1024 +"kB, " + file.getAbsolutePath +", is zipped: " + isZipped+"\n")

        (file,isZipped,isGzipped)
    }

    /**
     * Calls the collectory webservice to update the last loaded time for a data resource
     */
    def updateLastChecked(resourceUid:String) :Boolean ={
        try {
          //set the last check time for the supplied resourceUid
          val map = Map("user"-> user, "api_key"-> api_key, "lastChecked" ->loadTime)
          //turn the map of values into JSON representation
          val data = map.map(pair => "\""+pair._1 +"\":\"" +pair._2 +"\"").mkString("{",",", "}")
          //"http://woodfired.ala.org.au:8080/Collectory/ws/dataResource/"
          println(Http.postData(registryUrl+resourceUid,data).header("content-type", "application/json").responseCode)
          true
        } catch {
          case e:Exception => e.printStackTrace();false
        }
    }
}


class SecureDataLoader extends DataLoader{
      import JavaConversions._

      val connectionPattern = """sftp://([a-zA-Z]*):([a-zA-Z]*)@([a-zA-z\.]*):([a-zA-Z/]*)""".r
      println(connectionPattern)
      
      var channelSftp:ChannelSftp = null
      var session:Session = null
      
      def sftpLatestArchive(url:String, resourceUid:String):Option[String]={
        
        val (user,password,host,directory) ={
          url match{
          case connectionPattern(user, password, host, directory) =>{
            (user, password, host, directory)      
          }
          case sftpPattern(host,directory) =>{
            val u=Config.getProperty("uploadUser")
            val p =Config.getProperty("uploadPassword")
            if(StringUtils.isEmpty(u) || StringUtils.isEmpty(p))
              logger.error("SCP User or password has not been supplied. Please supply as part of the biocache.properties")
             (u,p,host,directory)
          }
          case _=>logger.error("Unable to connect to " + url);(null,null,null,null)
        }}
        connect(host, user, password)
        val lastFile = getLatestFile(directory, "*.*")
        disconnect
        if(lastFile.isDefined){
          val dir = temporaryFileStore + resourceUid
          //scp the file is faster than sftp
          scpFile(host,user,password,lastFile.get,new File(dir+ File.separator+lastFile.get))              
          }
          else{             
            logger.error("No latest file for " + url); None
          } 
      }
      
      def connect(host:String,  user:String, password:String,port:Int=22){
        val jsch = new JSch()
        session = jsch.getSession(user,host,port)
        val config = new java.util.Properties()
        config.put("StrictHostKeyChecking", "no")
        session.setConfig(config)
        session.setPassword(password)
        session.connect()
        val channel = session.openChannel("sftp")
        channel.connect()
        channelSftp =channel.asInstanceOf[ChannelSftp]
      }
      def disconnect(){
        channelSftp.disconnect()
        session.disconnect()
      }
      
      
      
      def sftpFile(serverFile:String, localDir:String):Option[String]={
        //FileUtils.forceMkdir(localDir)
//        val file = getLatestFile(dir)
//        if(file.isDefined){
        try{
          val inputStream = channelSftp.get(serverFile)
          //val outputStream = new FileOutputStream(new File(localDir + File.separator + file.get))//put(new FileInputStream(localDir+ File.separator+file.get), file.get);
          //Source.fromInputStream(inputStream).foreach(c => outputStream.write(c))
          org.apache.commons.io.FileUtils.copyInputStreamToFile(inputStream,new File(localDir + File.separator + serverFile))
          Some(localDir + File.separator + serverFile)
        }
        catch{
          case e:Exception=>None
        }
//        }        
      }
      
      def getLatestFile(dir:String, filePattern:String):Option[String]={
        
        val list = listFiles(dir+File.separator+filePattern)
        if(list.size>0){
          val item=list.reduceLeft((a,b)=>if(a.getAttrs().getMTime() > b.getAttrs().getMTime()) a else b)
                    
          Some(dir + File.separator+item.getFilename)
        }
        else{
          None
        }
      }
      /*
       * An ordering that sorts a list of SFTP files by the last modified time.
       */
      implicit val o = Ordering.by((p: ChannelSftp#LsEntry) => (p.getAttrs().getMTime()))
      
      def listFiles(filePattern:String):List[ChannelSftp#LsEntry]={

        val vector =channelSftp.ls(filePattern)
        
        vector.asInstanceOf[java.util.Vector[ChannelSftp#LsEntry]].toList.sorted(o.reverse)//.sort()
      }
}