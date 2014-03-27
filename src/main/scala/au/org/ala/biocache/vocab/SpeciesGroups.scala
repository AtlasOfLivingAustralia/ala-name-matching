package au.org.ala.biocache.vocab

import scala.collection.JavaConversions
import org.slf4j.LoggerFactory
import scala.io.Source
import scala.util.parsing.json.JSON
import au.org.ala.biocache.Config
import au.org.ala.biocache.model.Classification
import org.apache.commons.lang.StringUtils

/**
 * The species groups to test classifications against
 */
object SpeciesGroups {

  import au.org.ala.biocache.util.ReflectBean._
  import JavaConversions._
  import au.org.ala.biocache.util.BiocacheConversions._
  val logger = LoggerFactory.getLogger("SpeciesGroups")

  val groups = List(
   createSpeciesGroup("Animals", "kingdom", Array("Animalia"), Array(), null),
   createSpeciesGroup("Mammals", "classs", Array("Mammalia"),Array(), "Animals"),
   createSpeciesGroup("Birds", "classs", Array("Aves"), Array(), "Animals"),
   createSpeciesGroup("Reptiles", "classs", Array("Reptilia"), Array(), "Animals"),
   createSpeciesGroup("Amphibians", "classs", Array("Amphibia"), Array(),"Animals"),
   createSpeciesGroup("Fish", "classs", Array("Agnatha", "Chondrichthyes", "Osteichthyes", "Actinopterygii", "Sarcopterygii"), Array(), "Animals"),
   createSpeciesGroup("Molluscs", "phylum", Array("Mollusca"), Array(), "Animals"),
   createSpeciesGroup("Arthropods", "phylum", Array("Arthropoda"), Array(), "Animals"),
   createSpeciesGroup("Crustaceans", "classs" , Array("Branchiopoda", "Remipedia", "Maxillopoda", "Ostracoda", "Malacostraca"), Array(), "Arthropods"),
   createSpeciesGroup("Insects",  "classs", Array("Insecta"), Array(), "Arthropods"),
   createSpeciesGroup("Plants", "kingdom", Array("Plantae"), Array(), null),
   createSpeciesGroup("Bryophytes","phylum",Array("Bryophyta","Marchantiophyta","Anthocerotophyta"),Array(),"Plants"), //new group for AVH
   createSpeciesGroup("Gymnosperms","subclass", Array("Pinidae", "Cycadidae"), Array(), "Plants"), //new group for AVH
   createSpeciesGroup("FernsAndAllies","subclass", Array("Equisetidae", "Lycopodiidae", "Marattiidae", "Ophioglossidae", "Polypodiidae","Psilotidae"), Array(), "Plants"),
   //new groups for AVH
   createSpeciesGroup("Angiosperms", "subclass",Array("Magnoliidae"), Array(), "Plants"),//new group for AVH
   createSpeciesGroup("Monocots", "superorder", Array("Lilianae"), Array(), "Angiosperms"), //new group for AVH
   createSpeciesGroup("Dicots", "subclass", Array("Magnoliidae"),  Array("Lilianae"), "Angiosperms"), //new group for AVH
   createSpeciesGroup("Fungi", "kingdom", Array("Fungi"), Array(), null),
   createSpeciesGroup("Chromista","kingdom", Array("Chromista"), Array(), null),
   createSpeciesGroup("Protozoa", "kingdom", Array("Protozoa"), Array(), null),
   createSpeciesGroup("Bacteria", "kingdom", Array("Bacteria"), Array(), null),
   createSpeciesGroup("Algae","phylum", Array("Bacillariophyta","Chlorophyta","Cyanidiophyta","Prasinophyta","Rhodophyta",
                                               "Cryptophyta","Ochrophyta","Sagenista","Cercozoa","Euglenozoa","Cyanobacteria"),Array(),null)
  )

  def main(args:Array[String]){
    println(subgroups)
    println(groups)
    //362235 362236
    println(getSpeciesGroups("362235","362236"))
    println(getSpeciesSubGroups("362235","362236"))
    println(getSpeciesGroups("32353","32354"))
    println(getSpeciesSubGroups("32353","32354"))

  }

  val subgroups = {
    //look up the JSON String
    //FIXME this should be a URL from a webservice. This URL isnt stable!!!
    val json = Source.fromURL("https://ala-bie.googlecode.com/svn/trunk/bie-profile/src/main/resources/subgroups.json").getLines.mkString

    val list = JSON.parseFull(json).get.asInstanceOf[List[Map[String,Object]]]//.get(0).asInstanceOf[Map[String, String]]
    val subGroupBuffer = new scala.collection.mutable.ArrayBuffer[SpeciesGroup]
    //println(list)
    list.foreach{map =>{
      if(map.containsKey("taxonRank")){
        val rank = map.getOrElse("taxonRank","class").toString
        val taxaList = map.get("taxa").get.asInstanceOf[List[Map[String,String]]]
        taxaList.foreach{taxaMap =>
            subGroupBuffer += createSpeciesGroup(taxaMap.getOrElse("common","").trim, rank,Array(taxaMap.getOrElse("name","").trim), Array(), null)
        }
      } else{
        //it might be the "Plants" in which case we map the corresponding species group to the subgroup
        //IT needs to be represented in the JSON in the format below, ie name is the same as a species_group and common is the name to be used for the species_subgroup
        /*

          {
          "speciesGroup": "Plants",
          "facetName": "species_group",
          "taxa": [
            {
                "name": "Monocots",
                "common": "Monocots"
            },
            {
                "name": "Dicots",
                "common": "Dicots"
            },
            {
                "name": "Angiosperms",
                "common": "Flowering plants"
            },
            {
                "name": "FernsAndAllies",
                "common": "Ferns and Allies"
            },
            {
                "name": "Gymnosperms",
                "common": "Conifers, Cycads"
            }
          ]
          }
           */

        if (map.getOrElse("speciesGroup","none") == "Plants"){
          val taxaList = map.get("taxa").get.asInstanceOf[List[Map[String,String]]]
          taxaList.foreach{taxaMap =>
            //search for the sub group in the species group
            val g=groups.find(_.name == taxaMap.getOrElse("name","NONE"))
            if (g.isDefined){
              subGroupBuffer += createSpeciesGroup(taxaMap.getOrElse("common","").trim, g.get.rank, g.get.values, g.get.excludedValues,null)
            }
          }
        }

      }
    }}
    subGroupBuffer.toList
  }

  /*
   * Creates a species group by first determining the left right ranges for the values and excluded values.
   */
  def createSpeciesGroup(title:String, rank:String, values:Array[String], excludedValues:Array[String], parent:String):SpeciesGroup={
    val lftRgts = values.map((v:String) =>{
      var snr:au.org.ala.names.model.NameSearchResult ={ try{Config.nameIndex.searchForRecord(v, au.org.ala.names.model.RankType.getForName(rank))}
      catch{
        case e:au.org.ala.names.search.HomonymException => e.getResults()(0)
        case _:Exception => null
      }}
      if(snr != null){
      if(snr.isSynonym)
        snr = Config.nameIndex.searchForRecordByLsid(snr.getAcceptedLsid)
      (Integer.parseInt(snr.getLeft()), Integer.parseInt(snr.getRight()),true)
    }
    else{
      logger.debug(v + " has no name " )
      (-1,-1,false)
    }
    })
    val lftRgtExcluded = excludedValues.map((v:String) =>{
      var snr = Config.nameIndex.searchForRecord(v, null)
      if(snr != null){
      if(snr.isSynonym)
        snr = Config.nameIndex.searchForRecordByLsid(snr.getAcceptedLsid)
      (Integer.parseInt(snr.getLeft()), Integer.parseInt(snr.getRight()),false)
      }
      else{
        logger.debug(v + " has no name")
        (-1,-1,false)
      }
    })
    SpeciesGroup(title, rank, values, excludedValues, lftRgtExcluded ++ lftRgts, parent) // Excluded values are first so that we can discount a species group if necessary
  }

  def getStringList : java.util.List[String] = groups.map(g => g.name).toList.sorted

  /**
   * Returns all the species groups to which supplied classification belongs
   * @deprecated It is better to use the left right values to determine species groups
   */
  @Deprecated
  def getSpeciesGroups(cl:Classification):Option[List[String]]={
    val matchedGroups = groups.collect{case sg: SpeciesGroup if sg.values.contains(StringUtils.capitalize(StringUtils.lowerCase(cl.getter(sg.rank).asInstanceOf[String]))) => sg.name}
    Some(matchedGroups)
  }

  /**
   * Returns all the species groups to which the supplied left right values belong
   */
  def getSpeciesGroups(lft:String, rgt:String):Option[List[String]]= getGenericGroups(lft,rgt, groups)

  def getSpeciesSubGroups(lft:String, rgt:String):Option[List[String]] = getGenericGroups(lft,rgt,subgroups)

  def getGenericGroups(lft:String, rgt:String, groupingList:List[SpeciesGroup]):Option[List[String]]={
    try{
      val ilft = Integer.parseInt(lft)
      //val irgt = Integer.parseInt(rgt)
      val matchedGroups = groupingList.collect{case sg:SpeciesGroup if(sg.isPartOfGroup(ilft)) => sg.name}
      Some(matchedGroups)
    }
    catch {
      case _:Exception => None
    }
  }
}
