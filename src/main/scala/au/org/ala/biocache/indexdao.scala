/*
 * The configuration required for the SOLR index
 */

package au.org.ala.biocache


import org.apache.commons.lang.time.DateUtils
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer
import org.apache.solr.common.SolrInputDocument
import org.apache.solr.core.CoreContainer
import org.slf4j.LoggerFactory

object SolrIndexDAO {
  val solrHome = "/data/solr/bio-proto"
  //set the solr home
  System.setProperty("solr.solr.home", solrHome)

  val cc = new CoreContainer.Initializer().initialize
  val solrServer = new EmbeddedSolrServer(cc, "")
}
/**
 * All Index implementations need to extend this trait.
 */
trait IndexDAO {
  import org.apache.commons.lang.StringUtils.defaultString
  import au.org.ala.util.ReflectBean._
  /**
   * Index a list of Occurrence Index Model objects
   */
  def index(items: java.util.List[OccurrenceIndex]):Boolean
  /**
   * Index a single Occurrence Index Model
   */
  def index(item:OccurrenceIndex):Boolean
  def indexFromMap(guid:String, map:Map[String,String])
  /**
   * Truncate the current index
   */
  def emptyIndex
  /**
   * Perform
   */
  def finaliseIndex
  def getValue(field:String, map:Map[String, String]):String={
    val value = map.get(field)
    if(!value.isEmpty)
      return value.get
    ""
  }

  /**
   * Returns an array of all the assertions that are in the Map.
   * This duplicates some of the code that is in OccurrenceDAO because
   * we are not interested in processing the other values
   *
   * TODO we may wish to fix this so that it
   */
  def getAssertions(map : Map[String, String]):Array[String]={

     val columns = map.keySet
     var assertions =Array[String]("")
     for(fieldName<-columns){
       
      if(OccurrenceDAO.isQualityAssertion(fieldName)){
       
        val value = map.get(fieldName)
        
        if(!value.isEmpty){
          if(value.get equals "true"){
            if(assertions(0) == "")
              assertions = Array(OccurrenceDAO.removeQualityAssertionMarker(fieldName))
            else
              assertions = assertions ++ Array(OccurrenceDAO.removeQualityAssertionMarker(fieldName))
          }
        }
      }
    }
    assertions
  }
  /**
   * Returns a lat,long string expression formatted to the supplied Double format
   */
  def getLatLongString(lat:Double, lon:Double, format:String):String ={
    if(!lat.isNaN && !lon.isNaN){
      val df =new java.text.DecimalFormat(format)
      return df.format(lat) +"," + df.format(lon)
    }
    ""
  }
  /**
   * The header values for the CSV file.
   */
  def getHeaderValues() : Array[String] ={
    Array("id","occurrence_id","hub_uid","data_provider_uid","data_provider","data_resource_uid",
          "data_resource","institution_code_uid", "institution_code","institution_name",
          "collection_code_uid","collection_code","collection_name","catalogue_number",
          "taxon_concept_lsid","occurrence_date","taxon_name","common_name","names_and_lsid",
          "rank","rank_id","raw_taxon_name","raw_common_name","multimedia","image_url",
          "species_group","country_code","lft","rgt","kingdom","phylum","class","order",
          "family","genus","species","state","imcra","ibra","places","latitude","longitude",
          "lat_long","point-1","point-0.1","point-0.01","point-0.001","point-0.0001",
          "year","month","basis_of_record","raw_basis_of_record","type_status",
          "raw_type_status","taxonomic_kosher","geospatial_kosher","assertions","location_remarks",
          "occurrence_remarks","citation", "user_assertions","mean_temperature_cars2009a_band1_env",
          "mean_oxygen_cars2006_band1_env", "bioclim_bio34_env", "bioclim_bio12_env", "bioclim_bio11_env")
  }
  /**
   * Generates an string array version of the occurrence model.
   *
   * Access to the values are taken directly from the Map with no reflection. This
   * should result in a quicker load time.
   *
   */
  def getOccIndexModel(guid:String,map:Map[String,String]):Array[String]={

    try{
    //get the lat lon values so that we can determine all the point values
    var slat = getValue("decimalLatitude.p", map)
    var slon = getValue("decimalLongitude.p",map)
    var latlon =""
    val sciName = getValue("scientificName.p",map)
    val taxonConceptId = getValue("taxonConceptID.p", map)
    val vernacularName = getValue("vernacularName.p", map)
    val kingdom = getValue("kingdom.p", map)
    val family = getValue("family.p", map)
    val simages = getValue("images.p", map)
    val images  = if(simages.length >0) Json.toArray(simages, classOf[String].asInstanceOf[java.lang.Class[AnyRef]]).asInstanceOf[Array[String]] else Array[String]("")
    val sspeciesGroup = getValue("speciesGroups.p", map)
    val speciesGroup = if(sspeciesGroup.length>0) Json.toArray(sspeciesGroup, classOf[String].asInstanceOf[java.lang.Class[AnyRef]]).asInstanceOf[Array[String]] else Array[String]("")
    var eventDate = getValue("eventDate.p", map)
    //only want to include eventDates that are in the correct format
    try{
      DateUtils.parseDate(eventDate, Array("yyyy-MM-dd"))
    }
    catch{
     case e :Exception => eventDate =""
    }
    var lat = Double.NaN
    var lon = Double.NaN

    if(slat != "" && slon!=""){
      try{
      lat = java.lang.Double.parseDouble(slat)
      lon = java.lang.Double.parseDouble(slon)
      latlon = slat +"," +slon
      }
      catch{
        //If the latitude or longitude can't be parsed into a double we don't want to index the values
        case e:Exception =>slat="";slon=""
      }
    }

    Array(guid, getValue("occurrenceID",map),getValue("dataHubUid", map), getValue("dataProviderUid.p", map), getValue("dataProviderName.p",map),
          getValue("dataResourceUid.p", map), getValue("dataResourceName.p", map), getValue("institutionUid.p", map),
          getValue("institutionCode",map), getValue("institutionName.p",map),
          getValue("collectionUid.p",map), getValue("collectionCode", map), getValue("collectionName.p",map),
          getValue("catalogNumber", map), taxonConceptId, if(eventDate != "") eventDate + "T00:00:00Z" else "" ,
          sciName, vernacularName, sciName +"|" + taxonConceptId+"|" + vernacularName +"|" + kingdom +"|" + family,
          getValue("taxonRank.p", map), getValue("taxonRankID.p", map), getValue("scientificName", map),
          getValue("vernacularName",map), if(images !=null && images.size >0 &&images(0) != "") "Multimedia" else "None",if(images!=null && images.size >0)images(0)else "", if(speciesGroup !=  null)speciesGroup.reduceLeft(_+"|"+_) else "", getValue("countryCode", map),
          getValue("left.p", map), getValue("right.p",map),
          kingdom, getValue("phylum.p", map), getValue("classs.p", map),
          getValue("order.p", map), family, getValue("genus.p",map),
          getValue("species.p", map), getValue("stateProvince.p", map), getValue("imcra.p", map),
          getValue("ibra.p", map), getValue("lga.p", map), slat,
          slon, latlon, getLatLongString(lat, lon, "#"), getLatLongString(lat, lon, "#.#"),
          getLatLongString(lat, lon, "#.##"), getLatLongString(lat, lon, "#.###"),
          getLatLongString(lat, lon, "#.####"), getValue("year.p",map), getValue("month.p", map), getValue("basisOfRecord.p",map),
          getValue("basisOfRecord", map), getValue("typeStatus.p", map), getValue("typeStatus",map),
          getValue(OccurrenceDAO.taxonomicDecisionColumn, map), getValue(OccurrenceDAO.geospatialDecisionColumn, map),
          getAssertions(map).reduceLeft(_ + "|"+_), getValue("locationRemarks", map),
          getValue("occurrenceRemarks", map), "",  (getValue(OccurrenceDAO.userQualityAssertionColumn, map) != "").toString,
          getValue("mean_temperature_cars2009a_band1.p", map), getValue("mean_oxygen_cars2006_band1.p", map),
          getValue("bioclim_bio34.p", map), getValue("bioclim_bio12.p", map), getValue("bioclim_bio11.p", map) )
    
    }
    catch{
      case e:Exception => e.printStackTrace; throw e
    }

  }
  


  /**
   * Generate an index model from the supplied FullRecords
   */
  def getOccIndexModel(records: Array[FullRecord]):Option[OccurrenceIndex]={
    //cycle through all the items in each record and attempt to add them to the index
    val occ = new OccurrenceIndex
    for(i <- 0 to 1){
        val record = records(i)
        for(anObject <- record.objectArray){
            val defn = DAO.getDefn(anObject)
            for(field <- defn){
                //first time through we are processing the raw values
                var fieldName = if(i ==0) "raw_" + field else field
                //we only want to attempt to add the items that should appear in the occurrence
                if(DAO.occurrenceIndexDefn.contains(fieldName)){
                    val fieldValue = anObject.getClass.getMethods.find(_.getName == field).get.invoke(anObject).asInstanceOf[Any]
                    if(fieldValue!=null){
                        occ.setter(fieldName, fieldValue);
                    }
                }
            }
        }
    }
    //perform all the point construction
    if(occ.getDecimalLatitude != null && occ.getDecimalLongitude != null){
      var pat = "#"
      var fieldPre ="point"
      val lat = occ.getDecimalLatitude
      val long = occ.getDecimalLongitude
      for{ i <- 0 to 4}{
        val df =new java.text.DecimalFormat(pat)
        occ.setter(fieldPre + "1" ,df.format(lat) + "," + df.format(long))
        if(i == 0) pat = pat +"."
        pat = pat +"#"
        fieldPre = fieldPre + "0"
      }
      occ.setLatLong(occ.getDecimalLatitude.toString +"," + occ.getDecimalLongitude)
    }
    //set the id for the occurrence record to the uuid
    occ.uuid = records(0).uuid
    //set the systemAssertions 
    occ.assertions = records(1).assertions
    occ.setGeospatialKosher(records(1).getGeospatiallyKosher().toString)
    occ.setTaxonomicKosher(records(1).getTaxonomicallyKosher().toString)
    if(records(1).getOccurrence.getImages != null && records(1).getOccurrence.getImages.size >0){
      occ.setImage(records(1).getOccurrence.getImages()(0))
      occ.setMultimedia("Multimedia")
    }
    else
      occ.setMultimedia("None")

    occ.setHasUserAssertions((OccurrenceDAO.getSystemAssertions(occ.uuid).size>0).toString)
    Some(occ)
  }
}

object SolrOccurrenceDAO extends IndexDAO {

  import org.apache.commons.lang.StringUtils.defaultString
  val logger = LoggerFactory.getLogger("SolrOccurrenceDAO")
  val solrDocList = new java.util.ArrayList[SolrInputDocument]
  /**
   * returns whether or not the insert was successful
   */
  def index(items: java.util.List[OccurrenceIndex]):Boolean ={
    try{
        SolrIndexDAO.solrServer.addBeans(items)
        SolrIndexDAO.solrServer.commit
        true
    }
    catch{
      case e:Exception => false
    }

  }

  def index(item:OccurrenceIndex):Boolean ={
    try{
      SolrIndexDAO.solrServer.addBean(item)
      SolrIndexDAO.solrServer.commit
    }
    catch{
      case e:Exception => logger.error(e.getMessage, e); false
    }
    true
  }

  def emptyIndex(){
    SolrIndexDAO.solrServer.deleteByQuery("*:*")
  }

  def finaliseIndex(){
    if(!solrDocList.isEmpty)
      SolrIndexDAO.solrServer.add(solrDocList)
    SolrIndexDAO.solrServer.commit
    SolrIndexDAO.solrServer.optimize
  }
   
  override def getOccIndexModel(records: Array[FullRecord]):Option[OccurrenceIndex]={
    //set the SOLR specific value
    val occ = super.getOccIndexModel(records)
     if(!occ.isEmpty){
     //set the names lsid
      val v = List(defaultString(occ.get.scientificName), defaultString(occ.get.taxonConceptID), defaultString(occ.get.vernacularName), defaultString(occ.get.kingdom), defaultString(occ.get.family))
      occ.get.setNamesLsid(v.mkString("|"))
     }
     occ
  }
  /**
   * A SOLR specific implementation of indexing from a map.
   */
  override def indexFromMap(guid:String, map:Map[String,String])={
    val header = getHeaderValues()
    val values = getOccIndexModel(guid, map)
    val doc = new SolrInputDocument()
    for(i <- 0 to values.length-1){
      if(values(i) != ""){
        if(header(i) == "species_group" || header(i) == "assertions" ){
          //multiple valus in this field
          for(value<-values(i).split('|'))
            doc.addField(header(i), value)
        }
        else
          doc.addField(header(i), values(i))
      }
    }
    solrDocList.add(doc)
    if(solrDocList.size ==10000){
      try{
        SolrIndexDAO.solrServer.add(solrDocList)
        SolrIndexDAO.solrServer.commit
      }
      catch{
        case e:Exception => logger.error(e.getMessage,e)
      }
      solrDocList.clear
    }
  }

}
