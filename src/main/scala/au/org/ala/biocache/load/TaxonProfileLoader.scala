package au.org.ala.biocache.load

import au.org.ala.biocache._
import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import collection.mutable.ArrayBuffer
import java.text.MessageFormat
import au.org.ala.names.model.LinnaeanRankClassification
import au.org.ala.biocache.caches.{WebServiceLoader, TaxonSpeciesListDAO, TaxonProfileDAO}
import au.org.ala.biocache.model.TaxonProfile
import scala.util.parsing.json.JSON

object GenericTaxonProfileLoader {
  def main(args:Array[String]){
    //lsid, lft,rgt,rank,scientificname
    val file = if(args.size==1) args(0) else "/data/biocache-load/merge-taxon-profile.csv"
    val reader =  new CSVReader(new FileReader(file), '\t', '"', '~')
    var currentLine = reader.readNext
    var count =0
    while(currentLine != null){
      var tp = new TaxonProfile()
      tp.setGuid(currentLine(0))
      tp.setLeft(currentLine(1))
      tp.setRight(currentLine(2))
      tp.setRankString(currentLine(3))
      tp.setScientificName(currentLine(4))
      TaxonProfileDAO.add(tp)
      count+=1
      if(count%10000 == 0){
        println("Loaded " + count + " taxon >>>> " + currentLine.mkString(","))
      }
      currentLine = reader.readNext()
    }

  }
}
object HabitatLoader {
  def main(args:Array[String]){
    //get the habitat information
    val files = if(args.size == 0)Array("/data/biocache-load/species_list.txt","/data/biocache-load/family_list.txt", "/data/biocache-load/genus_list.txt")else args
    files.foreach( file=>{
      processFile(file)
    })
  }
  def processFile(file:String){

    println("Loading habitats from " + file)
    val reader =  new CSVReader(new FileReader(file), '\t', '"', '~')
    var currentLine = reader.readNext()
    var previousScientificName=""
    var count =0
    while(currentLine != null){
      var currentScientificName = currentLine(1)
      var habitat:String=null
      if(currentScientificName!=null && !currentScientificName.equalsIgnoreCase(previousScientificName)){
        val cl = new LinnaeanRankClassification()
        cl.setScientificName(currentScientificName)
        if (currentLine.length == 12){
          //we are dealing with a family
          cl.setFamily(currentScientificName)
          cl.setKingdom(currentLine(2))
          habitat = getValue(currentLine(4))
        } else if (currentLine.length==13){
          //dealing with genus or species
          val isGenus = !currentScientificName.contains(" ")
          if(isGenus){
            cl.setGenus(currentLine(1))
            if(currentLine(2).contains("unallocated")){
              cl.setFamily(currentLine(2));
            }
          }else{
            cl.setGenus(currentLine(2));
          }
        }
        val guid = Config.nameIndex.searchForAcceptedLsidDefaultHandling(cl,false);
        previousScientificName = currentScientificName;
        if(guid != null && habitat != null){
          //add the habitat status
          Config.persistenceManager.put(guid,"taxon","habitats",habitat);
          //println("Adding " +habitat + " for " +guid)
        }
        count+=1
        if(count%10000 == 0){
          println("Loaded " + count + " taxon >>>> " + currentLine.mkString(","))
        }
      }
      currentLine = reader.readNext()
    }

  }

  def getValue(v:String):String={
    v match{
      case it if it == 'M' => "Marine"
      case it if it == "N" => "Non-Marine"
      case it if it == "MN" => "Marine and Non-marine"
      case _ =>null
    }
  }
}

/**
 * Loads the taxon profile information from the species list tool
 */
object TaxonSpeciesListLoader {
  val guidUrl = Config.listToolUrl + "/speciesList/{0}/taxa"
  val guidsArray = new ArrayBuffer[String]()
  def main(args:Array[String]){
    // grab a list of distinct guids that form the list
    val lists = Config.getProperty("specieslists","").split(",")
    lists.foreach(v =>{
      //get the guids on the list
      val url = MessageFormat.format(guidUrl, v)
      val response = WebServiceLoader.getWSStringContent(url)
      if(response != ""){
        val list = JSON.parseFull(response).get.asInstanceOf[List[String]]
        guidsArray ++= list.filter(_ != null)
      }
    })
    val guids = guidsArray.toSet
    //now load all the details
    println("The number of distinct species " + guids.size)
    guids.foreach(g =>{
      //get the values from the cache
      val (lists, props) =TaxonSpeciesListDAO.getListsForTaxon(g, true)
      //now add the values to the DB
      TaxonSpeciesListDAO.addToPM(g, lists, props)
    })
  }

}