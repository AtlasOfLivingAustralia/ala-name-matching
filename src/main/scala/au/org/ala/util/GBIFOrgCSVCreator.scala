package au.org.ala.util
import java.util.zip.ZipOutputStream
import java.io.{FileOutputStream, OutputStreamWriter}
import java.util.zip.ZipEntry
import au.com.bytecode.opencsv.CSVWriter
import scala.io.Source
import org.apache.commons.io.FileUtils
import au.org.ala.biocache.Config
import au.org.ala.biocache.AttributionDAO
import java.io.FileWriter
import java.io.File

object GBIFOrgCSVCreator {

    def main(args: Array[String]): Unit = {

        var resourceUids = ""
        var fileName =""

        val parser = new OptionParser("Create Darwin Core Archive") {
            arg("<data resource UID>", "Comma separated list of data resources or all", {v: String => resourceUids = v})
            arg("<file name>", "The name of the file to create", {v:String => fileName = v } )
        }
        if(parser.parse(args)){
          val creator = new GBIFOrgCSVCreator
            if("all".equals(resourceUids)){
              //get a list of data resource uids from the index.
              val uids = Config.indexDAO.getDistinctValues("*:*","data_resource_uid",200);
              if(uids.isDefined)
                creator.create(fileName,uids.get)
                
            }
            else{
              creator.create(fileName,resourceUids.split(",").toList)
            }
          //shutdown the index so that we can exit naturally
          Config.indexDAO.shutdown
        }
    }
}

/**
 * Creates a CSV file required for GBIF
 */
class GBIFOrgCSVCreator {
    val technicalContactEmail="support@ala.org.au"
    def create(fileName:String,dataResources:List[String]){
      println("Creating CSV for " + dataResources)
      val outWriter = new FileWriter(new File(fileName))
      val writer = new CSVWriter(outWriter, ',', '"')
      writer.writeNext(Array("organisationName","organisationDescription","organisationContactType", "organisationContactEmail","organisationNodeKey","resourceName","resourceDescription","resourceHomePageURL","resourceContactType","resourceContactEmail","dwcaAccessPointURL"))
      //if it has a data provider use it as the organisation name otherwise use the data resources name?
      dataResources.foreach(dr=>{
          val map = AttributionDAO.getDataResourceAsMap(dr)
          //TO DO only add it if it has a publicly available DwCA (but waiting for a change to the collectory)
          //val archiveAvail = map("publicArchiveAvailable") //at the moment this still needs to be configured in the collectory
          val (organisationName:String,organisationDescription:String) ={
            if(map.contains("provider")){
              val provider = map("provider").asInstanceOf[Map[String,AnyRef]]
              //get the data provider details
              val dpmap = AttributionDAO.getDataProviderAsMap(provider("uid").asInstanceOf[String])
              val desc = dpmap("pubDescription")
              (dpmap("name"),if(desc == null) "" else desc)
            }
            else{
              (map("name"),map("pubDescription"))
            }
          }
          val name = map("name")
          val description = map("pubDescription")
          val url = map("websiteUrl")
          
          val accessPointURL = map("publicArchiveUrl")//"http://biocache.ala.org.au/archives/"+dr+"/"+dr+"_ror_dwca.zip" 
          writer.writeNext(Array(organisationName, organisationDescription, "Technical",technicalContactEmail, "au", name, description,url,"Technical",technicalContactEmail, accessPointURL))
          
      }) 
      writer.flush
      writer.close
    }
    
    
}


