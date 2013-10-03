package au.org.ala.util

import au.org.ala.biocache._
import java.net.InetSocketAddress
import org.apache.avro.ipc.SocketTransceiver
import org.apache.avro.specific.SpecificRequestor
import org.apache.avro.util.Utf8
import au.org.ala.bie.rpc.{ ProfileArray, Page, SpeciesProfile }
import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import org.apache.commons.lang.StringUtils
import collection.mutable.ArrayBuffer
import java.text.MessageFormat
import util.parsing.json.JSON

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
    
    def mainold(args: Array[String]){

    println("Starting the load...")
    import scala.collection.JavaConversions._
    
    val host = args(0)
    val port = args(1).toInt
    val client = new SocketTransceiver(new InetSocketAddress(host, port))
    val proxy = SpecificRequestor.getClient(classOf[SpeciesProfile], client).asInstanceOf[SpeciesProfile]
    var page = new Page
    var lastKey = new Utf8("")
    page.startKey = lastKey
    page.pageSize = 1000
    var array = proxy.send(page).asInstanceOf[ProfileArray]
    var count =0

    //iterate through all the profiles
    while(array.profiles.size>0){
	    for (profile <- array.profiles) {
              count+=1
              if (count % 1000 == 0 && count > 0) {
                println(count+">>>" +profile)
              }
	      //add to cache
	      //println(profile)
	      var taxonProfile = new TaxonProfile
	      taxonProfile.guid = profile.guid.toString
	      taxonProfile.scientificName  = profile.scientificName.toString
	      if(profile.commonName!=null){
	    	  taxonProfile.commonName = profile.commonName.toString
	      }
	      if(profile.habitat!=null){
	    	  val habitats = for(habitat<-profile.habitat) yield habitat.toString
	    	  taxonProfile.habitats = habitats.toArray
	      }
          if(profile.left != null){
            taxonProfile.left = profile.left.toString
          }
          if(profile.right != null){
            taxonProfile.right = profile.right.toString
          }
          if(profile.rank != null){
            taxonProfile.rankString = profile.rank.toString
          }
          //TODO work out whatto store from the conservation status
          if(profile.consStatus != null){
              val css = for(cs<-profile.consStatus)yield{
                  val region = if(cs.region != null)cs.region.toString else null
                  val regionId = if(cs.regionId != null) cs.regionId.toString else null
                  val status = if(cs.status != null) cs.status.toString else null
                  val rawStatus = if(cs.rawStatus != null) cs.rawStatus.toString else null
                  
               new ConservationSpecies(region,regionId, status, rawStatus)}
              taxonProfile.conservation = css.toArray
          }
          //store the sensitive species information
          //TODO fix up this so that the zone is obtained from the vocabulary?
          if(profile.sensitiveStatus != null){
            val sss = for(ss <- profile.sensitiveStatus) 
              yield new SensitiveSpecies(ss.sensitivityZone.toString, ss.sensitivityCategory.toString)
              
            taxonProfile.sensitive = sss.toArray
          }
              
	      TaxonProfileDAO.add(taxonProfile)
	      lastKey = profile.guid
	    }
	    page.startKey = lastKey
	    array = proxy.send(page).asInstanceOf[ProfileArray]
    }
    client.close
    println("Finished loading " + count + " profiles ")
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