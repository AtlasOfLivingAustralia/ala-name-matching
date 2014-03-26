package au.org.ala.util

import au.org.ala.biocache._

import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import org.apache.commons.lang.StringUtils
import collection.mutable.ArrayBuffer
import java.text.MessageFormat
import util.parsing.json.JSON
import au.org.ala.data.model.LinnaeanRankClassification

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
 * Primes the cache of species profiles. To run this, you need to have
 * bie-rpc server running on top of an existing BIE install.
 * 
 * See bie-rpc for more details.
 */
object TaxonProfileLoader {

  def main(args: Array[String]) {
      //this version of the load uses a CSV version of the information
      val file = if(args.size==1) args(0) else "/data/biocache-load/taxon_profile.csv"
      val reader =  new CSVReader(new FileReader(file), '\t', '"', '~')
      val header = reader.readNext()
      println(header.length + " " + header.toList)
      var currentLine = reader.readNext
      var count =0
      
      val conservationPattern = """rawCode=([A-Za-z0-9 /\*]*), rawStatus=([A-Za-z 0-9\(\)]*), region=([A-Za-z ]*), regionId=([\x00-\x7F\s]*), status=([A-Z/a-z ]*), system=([\x00-\x7F\s]*), regions=([\x00-\x7F\s]*)""".r
      
      var conservationPattern(rawCode, rawStatus, region, regionId, status, system, regions) = "rawCode=R, rawStatus=Rare, region=South Australia, regionId=aus_states/South Australia, status=Near Threatened, system=National Parks and Wildlife Act 1972, regions=null"
          println (rawCode + "  " + rawStatus + " " + region + " " + regionId + " " + status + " "  + system)
      
      while(currentLine!=null){
          count+=1
          if (count % 1000 == 0 && count > 0) {
             println(count+">>>" +currentLine.mkString("\t"))
          }
          if(currentLine.length == 9){
              var taxonProfile = new TaxonProfile
              taxonProfile.guid = currentLine(0)
              taxonProfile.scientificName  = currentLine(1)
              taxonProfile.left = StringUtils.stripToNull(currentLine(2))
              taxonProfile.right = StringUtils.stripToNull(currentLine(3))
              taxonProfile.rankString = StringUtils.stripToNull(currentLine(4))
              taxonProfile.commonName = StringUtils.stripToNull(currentLine(5))
              //habitat is a CSV list of single values
              if(StringUtils.isNotEmpty(currentLine(6))){
                  taxonProfile.habitats = currentLine(6).split(",");
              }
              //conservation status is a CSV list of ConservationStatus [rawCode=R, rawStatus=Rare, region=South Australia, regionId=aus_states/South Australia, status=Near Threatened, system=National Parks and Wildlife Act 1972, regions=null],ConservationStatus [rawCode=V, rawStatus=Vulnerable, region=New South Wales, regionId=aus_states/New South Wales, status=Endangered, system=Threatened Species Conservation Act 1995, regions=null]
              if(StringUtils.isNotEmpty(currentLine(7))){
                  val arrVals = currentLine(7).split(",ConservationStatus")
                  val cs = for(value <-arrVals) yield{
                      val modValue = value.replaceAll("ConservationStatus \\[","").replaceAll("\\]","").replaceAll("\\[","").trim
                      var conservationPattern(rawCode, rawStatus, region, regionId, status, system,regions) = modValue
                      new ConservationSpecies(region,regionId, status, rawStatus)}
                  taxonProfile.conservation = cs.toArray
              }
              TaxonProfileDAO.add(taxonProfile)
          }
          else{
              println("Issue with " + count + " ("+currentLine.length +") " + currentLine.toList)
          }
          currentLine = reader.readNext
      }
  }
    

  println("Finished species profile loading.")
}

/**
 * Loads the taxon profile information from the species list tool
 */
object TaxonSpeciesListLoader {
  val guidUrl="http://lists.ala.org.au/ws/speciesList/{0}/taxa"
  val guidsArray = new ArrayBuffer[String]()
  def main(args:Array[String]){
    // grab a list of distinct guids that form the list
    val lists = Config.getProperty("specieslists","").split(",")
    lists.foreach(v =>{
      //get the guids on the list
      val url = MessageFormat.format(guidUrl, v)
      val response = WebServiceLoader.getWSStringContent(url)
      if(response != ""){
        val list =JSON.parseFull(response).get.asInstanceOf[List[String]]
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