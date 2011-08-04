/*
 * The configuration required for the SOLR index
 */
package au.org.ala.biocache

import java.lang.reflect.Method
import java.util.Collections
import org.apache.commons.lang.time.DateUtils
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer
import org.apache.solr.client.solrj.SolrServer
import org.apache.solr.common.SolrInputDocument
import org.apache.solr.core.CoreContainer
import org.slf4j.LoggerFactory
import com.google.inject.Inject
import com.google.inject.name.Named
import scala.actors.Actor
import scala.collection.mutable.ArrayBuffer
import java.util.Date
import java.io.OutputStream



/**
 * All Index implementations need to extend this trait.
 */
trait IndexDAO {

    import org.apache.commons.lang.StringUtils.defaultString
    //import au.org.ala.util.ReflectBean._

//    val elFields = Bean()FullRecordMapper.environmentalDefn.keySet.toList
//    val clFields = FullRecordMapper.contextualDefn.keySet.toList

    val elFields = (new EnvironmentalLayers).propertyNames.toList
    val clFields = (new ContextualLayers).propertyNames.toList

    def getRowKeysForQuery(query:String, limit:Int=1000):Option[List[String]]
    
    def writeRowKeysToStream(query:String, outputStream: OutputStream)
       
    
    def occurrenceDAO:OccurrenceDAO

    /**
     * Index a list of Occurrence Index Model objects
     */
    def index(items: java.util.List[OccurrenceIndex]): Boolean

    /**
     * Index a single Occurrence Index Model
     */
    def index(item: OccurrenceIndex): Boolean

    /**
     * Index a record with the supplied properties.
     */
    def indexFromMap(guid: String, map: Map[String, String], batch:Boolean=true,startDate:Option[Date]=None)

    /**
     * Truncate the current index
     */
    def emptyIndex
    def reload
    /**
     * Remove all the records with the specified value in the specified field
     */
    def removeFromIndex(field:String, values:String)

    /**
     * Perform
     */
    def finaliseIndex(optimise:Boolean=false, shutdown:Boolean=true)

    //def stopThread

    def getValue(field: String, map: Map[String, String]): String = {
        val value = map.get(field)
        if (!value.isEmpty){
            return value.get
        } else {
           return  ""
        }

    }

    def getValue(field: String, map: Map[String, String], checkparsed:Boolean):String ={
       var value = getValue(field, map)
       if(value == "" && checkparsed)
         value = getValue(field + ".p", map)
       value
    }

    /**
     * Returns an array of all the assertions that are in the Map.
     * This duplicates some of the code that is in OccurrenceDAO because
     * we are not interested in processing the other values
     *
     * TODO we may wish to fix this so that it uses the same code in the mappers
     */
    def getAssertions(map: Map[String, String]): Array[String] = {

        val columns = map.keySet
        var assertions = Array[String]("")
        columns.foreach(fieldName => 

            if (FullRecordMapper.isQualityAssertion(fieldName)) {

                val value = map.get(fieldName).get

                if(value != "true" && value != "false"){
                  val arr = Json.toListWithGeneric(value,classOf[java.lang.Integer])
                  for(i <- 0 to arr.size-1)
                    assertions = assertions :+ AssertionCodes.getByCode(arr(0)).get.getName
                }
            }
        )
        assertions
    }

    /**
     * Returns a lat,long string expression formatted to the supplied Double format
     */
    def getLatLongString(lat: Double, lon: Double, format: String): String = {
        if (!lat.isNaN && !lon.isNaN) {
            val df = new java.text.DecimalFormat(format)
            df.format(lat) + "," + df.format(lon)
        } else {
            ""
        }
    }

    /**
     * The header values for the CSV file.
     */
    val header = List("id", "row_key", "occurrence_id", "data_hub_uid", "data_hub", "data_provider_uid", "data_provider", "data_resource_uid",
            "data_resource", "institution_uid", "institution_code", "institution_name",
            "collection_uid", "collection_code", "collection_name", "catalogue_number",
            "taxon_concept_lsid", "occurrence_date", "occurrence_year", "taxon_name", "common_name", "names_and_lsid",
            "rank", "rank_id", "raw_taxon_name", "raw_common_name", "multimedia", "image_url",
            "species_group", "country_code", "country", "lft", "rgt", "kingdom", "phylum", "class", "order",
            "family", "genus", "species","species_guid", "state", "imcra", "ibra", "places", "latitude", "longitude",
            "lat_long", "point-1", "point-0.1", "point-0.01", "point-0.001", "point-0.0001",
            "year", "month", "basis_of_record", "raw_basis_of_record", "type_status",
            "raw_type_status", "taxonomic_kosher", "geospatial_kosher", "assertions", "location_remarks",
            "occurrence_remarks", "citation", "user_assertions", "system_assertions", "collector","state_conservation","raw_state_conservation","sensitive", "coordinate_uncertainty") ++ elFields ++ clFields
//    def getHeaderValues(): List[String] = {
//        List("id", "occurrence_id", "data_hub_uid", "data_hub", "data_provider_uid", "data_provider", "data_resource_uid",
//            "data_resource", "institution_uid", "institution_code", "institution_name",
//            "collection_uid", "collection_code", "collection_name", "catalogue_number",
//            "taxon_concept_lsid", "occurrence_date", "occurrence_year", "taxon_name", "common_name", "names_and_lsid",
//            "rank", "rank_id", "raw_taxon_name", "raw_common_name", "multimedia", "image_url",
//            "species_group", "country_code", "lft", "rgt", "kingdom", "phylum", "class", "order",
//            "family", "genus", "species", "state", "imcra", "ibra", "places", "latitude", "longitude",
//            "lat_long", "point-1", "point-0.1", "point-0.01", "point-0.001", "point-0.0001",
//            "year", "month", "basis_of_record", "raw_basis_of_record", "type_status",
//            "raw_type_status", "taxonomic_kosher", "geospatial_kosher", "assertions", "location_remarks",
//            "occurrence_remarks", "citation", "user_assertions", "collector") ++ FullRecordMapper.environmentalDefn.keySet.toList ++ FullRecordMapper.contextualDefn.keySet.toList
//    }

            
     def getRawScientificName(map:Map[String,String]):String={
        val scientificName:String ={
            if(map.contains("scientificName"))
                map.get("scientificName").get
            else if(map.contains("genus")){
                var tmp:String = map.get("genus").get
                if(map.contains("specificEpithet") || map.contains("species")){
                    tmp=tmp + " " +map.getOrElse("specificEpithet", map.getOrElse("species",""))
                    if(map.contains("infraspecificEpithet") || map.contains("subspecies"))
                        tmp=tmp+ " " + map.getOrElse("infraspecificEpithet",map.getOrElse("subspecies",""))
                }
                tmp
            }
            else if(map.contains("family"))
                map.get("family").get
            else
                ""
        }
        scientificName
    }
            
    /**
     * Generates an string array version of the occurrence model.
     *
     * Access to the values are taken directly from the Map with no reflection. This
     * should result in a quicker load time.
     *
     */
    def getOccIndexModel(guid: String, map: Map[String, String]): List[String] = {

        try {
            //get the lat lon values so that we can determine all the point values
            val deleted = getValue(FullRecordMapper.deletedColumn, map)
            //only add it to the index is it is not deleted
            if (!deleted.equals("true")) {
                var slat = getValue("decimalLatitude.p", map)
                var slon = getValue("decimalLongitude.p", map)
                var latlon = ""
                val sciName = getValue("scientificName.p", map)
                val taxonConceptId = getValue("taxonConceptID.p", map)
                val vernacularName = getValue("vernacularName.p", map)
                val kingdom = getValue("kingdom.p", map)
                val family = getValue("family.p", map)
                val simages = getValue("images.p", map)
                val images = {
                    if (simages.length > 0)
                        Json.toArray(simages, classOf[String].asInstanceOf[java.lang.Class[AnyRef]]).asInstanceOf[Array[String]]
                    else
                        Array[String]("")
                }
                val sspeciesGroup = getValue("speciesGroups.p", map)
                val speciesGroup = {
                    if (sspeciesGroup.length > 0)
                        Json.toArray(sspeciesGroup, classOf[String].asInstanceOf[java.lang.Class[AnyRef]]).asInstanceOf[Array[String]]
                    else
                        Array[String]("")
                }
                val sdatahubs = getValue("dataHubUid",map, true)
                val dataHubUids = {
                    if (sdatahubs.length > 0)
                        Json.toArray(sdatahubs, classOf[String].asInstanceOf[java.lang.Class[AnyRef]]).asInstanceOf[Array[String]]
                    else
                        Array[String]("")
                }
                var eventDate = getValue("eventDate.p", map)
                var occurrenceYear = getValue("year.p", map)
                if (occurrenceYear.length == 4)
                    occurrenceYear += "-01-01T00:00:00Z"
                else
                    occurrenceYear = ""
                //only want to include eventDates that are in the correct format
                try {
                    DateUtils.parseDate(eventDate, Array("yyyy-MM-dd"))
                }
                catch {
                    case e: Exception => eventDate = ""
                }
                var lat = java.lang.Double.NaN
                var lon = java.lang.Double.NaN

                if (slat != "" && slon != "") {
                    try {
                        lat = java.lang.Double.parseDouble(slat)
                        lon = java.lang.Double.parseDouble(slon)
                        val test = -90D
                        val test2 = -180D
                        //ensure that the lat longs are in the required range before
                        if( lat<=90  && lat >= test && lon <=180 && lon>=test2 ){
                          latlon = slat + "," + slon
                        }
                    } catch {
                        //If the latitude or longitude can't be parsed into a double we don't want to index the values
                        case e: Exception => slat = ""; slon = ""
                    }
                }
                val sconservation = getValue("stateConservation.p", map)
                val stateCons = if(sconservation!="")sconservation.split(",")(0)else ""
                val rawStateCons = if(sconservation!="")sconservation.split(",")(1)else ""
                
                val sensitive:String = {
                    if(getValue("originalDecimalLatitude",map) != "") 
                        "generalised"
                    else if(getValue("dataGeneralizations.p",map ) != "")
                        "alreadyGeneralised"
                    else
                        ""
                }

                return List(getValue("uuid", map),
                    getValue("rowKey", map),
                    getValue("occurrenceID", map),
                    if(dataHubUids != null && dataHubUids.size>0)dataHubUids.reduceLeft(_+"|"+_) else"",
                    getValue("dataHub.p", map),
                    getValue("dataProviderUid", map, true),
                    getValue("dataProviderName", map, true),
                    getValue("dataResourceUid", map, true),
                    getValue("dataResourceName", map, true),
                    getValue("institutionUid.p", map),
                    getValue("institutionCode", map),
                    getValue("institutionName.p", map),
                    getValue("collectionUid.p", map),
                    getValue("collectionCode", map),
                    getValue("collectionName.p", map),
                    getValue("catalogNumber", map),
                    taxonConceptId, if (eventDate != "") eventDate + "T00:00:00Z" else "", occurrenceYear,
                    sciName,
                    vernacularName,
                    sciName + "|" + taxonConceptId + "|" + vernacularName + "|" + kingdom + "|" + family,
                    getValue("taxonRank.p", map),
                    getValue("taxonRankID.p", map),
                    getRawScientificName(map),
                    getValue("vernacularName", map),
                    if (images != null && images.size > 0 && images(0) != "") "Multimedia" else "None",
                    if (images != null && images.size > 0) images(0) else "",
                    if (speciesGroup != null) speciesGroup.reduceLeft(_ + "|" + _) else "",
                    getValue("countryCode", map),
                    getValue("country.p", map),
                    getValue("left.p", map),
                    getValue("right.p", map),
                    kingdom, getValue("phylum.p", map),
                    getValue("classs.p", map),
                    getValue("order.p", map), family,
                    getValue("genus.p", map),
                    getValue("species.p", map),
                    getValue("speciesID.p",map),
                    getValue("stateProvince.p", map),
                    getValue("imcra.p", map),
                    getValue("ibra.p", map),
                    getValue("lga.p", map),
                    slat,
                    slon,
                    latlon,
                    getLatLongString(lat, lon, "#"),
                    getLatLongString(lat, lon, "#.#"),
                    getLatLongString(lat, lon, "#.##"),
                    getLatLongString(lat, lon, "#.###"),
                    getLatLongString(lat, lon, "#.####"),
                    getValue("year.p", map),
                    getValue("month.p", map),
                    getValue("basisOfRecord.p", map),
                    getValue("basisOfRecord", map),
                    getValue("typeStatus.p", map),
                    getValue("typeStatus", map),
                    getValue(FullRecordMapper.taxonomicDecisionColumn, map),
                    getValue(FullRecordMapper.geospatialDecisionColumn, map),
                    getAssertions(map).reduceLeft(_ + "|" + _),
                    getValue("locationRemarks", map),
                    getValue("occurrenceRemarks", map),
                    "",
                    (getValue(FullRecordMapper.userQualityAssertionColumn, map) != "").toString,
                    (getValue(FullRecordMapper.qualityAssertionColumn, map).length > 3).toString,
                    getValue("recordedBy", map),
                    //getValue("austConservation.p",map),
                    stateCons,
                    rawStateCons,
                    sensitive,
                    getValue("coordinateUncertaintyInMeters.p",map)
                    
                    
//                    getValue("mean_temperature_cars2009a_band1.p", map),
//                    getValue("mean_oxygen_cars2006_band1.p", map),
//                    getValue("bioclim_bio34.p", map),
//                    getValue("bioclim_bio12.p", map),
//                    getValue("bioclim_bio11.p", map)
                    ) ++ elFields.map(field => getValue(field+".p", map)) ++ clFields.map(field=> getValue(field+".p", map))
            }
            else {
                return List()
            }

        }
        catch {
            case e: Exception => e.printStackTrace; throw e
        }
    }

    /**
     * Generate an index model from the supplied FullRecords
     */
    def getOccIndexModel(raw:FullRecord, processed:FullRecord): Option[OccurrenceIndex] = {
        
        //cycle through all the items in each record and attempt to add them to the index
        val occ = new OccurrenceIndex
        //val x = Bean(classOf[OccurrenceIndex],null,null,null)
        val (rawFields, processedFields) = occ.propertyNames.partition(p => p.startsWith("raw_"))

        rawFields.foreach(field => {
            val value = raw.getNestedProperty(field)
            occ.setProperty(field, value.getOrElse("")) 
    	})

        processedFields.foreach(field => {
            val value = processed.getNestedProperty(field)
            occ.setProperty(field, value.getOrElse("")) 
    	})
        

        //perform all the point construction
        if (occ.getDecimalLatitude != null && occ.getDecimalLongitude != null) {
            var pat = "#"
            var fieldPre = "point"
            val lat = occ.getDecimalLatitude
            val long = occ.getDecimalLongitude
            for {i <- 0 to 4} {
                val df = new java.text.DecimalFormat(pat)
                //FIXME
                occ.setProperty(fieldPre + "1", df.format(lat) + "," + df.format(long))
                if (i == 0) pat = pat + "."
                pat = pat + "#"
                fieldPre = fieldPre + "0"
            }
            occ.setLatLong(occ.getDecimalLatitude.toString + "," + occ.getDecimalLongitude)
        }
        //set the id for the occurrence record to the uuid
        occ.uuid = raw.uuid
        //set the systemAssertions
        occ.assertions = processed.assertions
        occ.setGeospatialKosher(processed.getGeospatiallyKosher().toString)
        occ.setTaxonomicKosher(processed.getTaxonomicallyKosher().toString)
        if (processed.getOccurrence.getImages != null && !processed.getOccurrence.getImages.isEmpty) {
            occ.setImage(processed.getOccurrence.getImages()(0))
            occ.setMultimedia("Multimedia")
        }
        else
            occ.setMultimedia("None")

        //set the occurrence_year
        val year = processed.getEvent.getYear
        if (year.length == 4)
            occ.setOccurrenceYear(DateUtils.parseDate(year, Array("yyyy")))

        occ.setHasUserAssertions((occurrenceDAO.getUserAssertions(occ.uuid).size > 0).toString)
        Some(occ)
    }
}
/**
 * DAO for indexing to SOLR
 */
class SolrIndexDAO @Inject()(@Named("solrHome") solrHome:String) extends IndexDAO{
    import org.apache.commons.lang.StringUtils.defaultString
    import scalaj.collection.Imports._
    //set the solr home
    System.setProperty("solr.solr.home", solrHome)

    //val cc = new CoreContainer.Initializer().initialize
    var cc:CoreContainer = _
    var solrServer:SolrServer =_ //new EmbeddedSolrServer(cc, "")

    @Inject
    var occurrenceDAO:OccurrenceDAO = _

    val logger = LoggerFactory.getLogger("SolrOccurrenceDAO")
    val solrDocList = new java.util.ArrayList[SolrInputDocument](10000)
   
    val thread = new SolrIndexActor()
    
    
    def init(){
      if(solrServer == null){
        cc = new CoreContainer.Initializer().initialize
        solrServer = new EmbeddedSolrServer(cc, "")
        thread.start
      }
    }

    /**
     * returns whether or not the insert was successful
     */
    def index(items: java.util.List[OccurrenceIndex]): Boolean = {
        init
        try {
            solrServer.addBeans(items)
            solrServer.commit
            true
        }
        catch {
            case e: Exception => false
        }
    }

    def index(item: OccurrenceIndex): Boolean = {
        init
        try {
            solrServer.addBean(item)
            solrServer.commit
        }
        catch {
            case e: Exception => logger.error(e.getMessage, e); false
        }
        true
    }

    def reload = thread ! "reload"

    def emptyIndex() {
        init
        try {
        	solrServer.deleteByQuery("*:*")
        } catch {
            case e:Exception =>e.printStackTrace(); println("Problem clearing index...")
        }
    }

    def removeFromIndex(field:String, value:String) ={
        init
        try{
          //println("Deleting " + field +":" + value)
            solrServer.deleteByQuery(field +":\"" + value+"\"")
            solrServer.commit
        }
        catch{
            case e:Exception =>e.printStackTrace
        }
    }

//    def stopThread ={
//      println("Stopping")
//      thread ! "exit"
//      //cc.shutdown
//    }

    def finaliseIndex(optimise:Boolean=false, shutdown:Boolean=true) {
        init
        if (!solrDocList.isEmpty) {
        
           //SolrIndexDAO.solrServer.add(solrDocList)
           while(!thread.ready){ Thread.sleep(50) }
           thread ! solrDocList
           thread ! "exit"
           //wait enough time for the Actor to get the message
           Thread.sleep(50)
        }
        while(!thread.ready){ Thread.sleep(50) }
        solrServer.commit
        printNumDocumentsInIndex
        if(optimise)
          solrServer.optimize
        if(shutdown)
        	cc.shutdown
    }

    override def getOccIndexModel(raw: FullRecord, processed: FullRecord): Option[OccurrenceIndex] = {
        //set the SOLR specific value
        val occ = super.getOccIndexModel(raw,processed)
        if (!occ.isEmpty) {
            //set the names lsid
            val v = List(
                defaultString(occ.get.scientificName), 
                defaultString(occ.get.taxonConceptID),
                defaultString(occ.get.vernacularName), 
                defaultString(occ.get.kingdom), 
                defaultString(occ.get.family)
            )
            occ.get.setNamesLsid(v.mkString("|"))
        }
        occ
    }
    /**
     * Decides whether or not the current record should be indexed based on processed times
     */
    def shouldIndex(map:Map[String,String], startDate:Option[Date]):Boolean={
        if(!startDate.isEmpty){            
            val lastLoaded = DateParser.parseStringToDate(getValue(FullRecordMapper.alaModifiedColumn,map))
            val lastProcessed = DateParser.parseStringToDate(getValue(FullRecordMapper.alaModifiedColumn+".p",map))
            return startDate.get.before(lastProcessed.getOrElse(startDate.get)) || startDate.get.before(lastLoaded.getOrElse(startDate.get))
        }        
        true
    }

    /**
     * A SOLR specific implementation of indexing from a map.
     */
    override def indexFromMap(guid: String, map: Map[String, String], batch:Boolean=true, startDate:Option[Date]=None) = {
        init
        //val header = getHeaderValues()
        if(shouldIndex(map, startDate)){
            val values = getOccIndexModel(guid, map)
            if(values.length != header.length){
              println("values don't matcher header: " +values.length+":"+header.length+", values:header")
              println(header)
              println(values)
              
                exit(1)
            }
            if (values.length > 0) {
                val doc = new SolrInputDocument()
                for (i <- 0 to values.length - 1) {
                    if (values(i) != "") {
                        if (header(i) == "species_group" || header(i) == "assertions" || header(i) =="data_hub_uid") {
                            //multiple valus in this field
                            for (value <- values(i).split('|')){
                            	if(value != "")
                            		doc.addField(header(i), value)
                            }
                        }
                        else
                            doc.addField(header(i), values(i))
                    }
                }
    
    
    
    
               if(!batch){
                 solrServer.add(doc);
                 solrServer.commit
               }
               else{
                 if(values(0) == "" || values(0) == null)
                   println("Unable to add doc with missing uuid " + values(1))
                 else
                    solrDocList.add(doc)
    
                if (solrDocList.size == 10000) {
                  while(!thread.ready){ Thread.sleep(50) }
                  
                  val tmpDocList = new java.util.ArrayList[SolrInputDocument](solrDocList);
                  
                  thread ! tmpDocList
                  solrDocList.clear
                }
               }
          }
        }
    }
    /**
     * Gets the rowKeys for the query that is supplied
     * Do here so that still works if web service is down
     *
     * This causes OOM exceptions at SOLR for large numbers of row keys
     * Use writeRowKeysToStream instead
     *
     */
    override  def getRowKeysForQuery(query:String, limit:Int=1000):Option[List[String]] ={
        init
        val solrQuery = new SolrQuery();
        solrQuery.setQueryType("standard");
        // Facets
        solrQuery.setFacet(true)
        solrQuery.addFacetField("row_key")
        solrQuery.setQuery(query)
        solrQuery.setRows(0)
        solrQuery.setFacetLimit(-1)
        solrQuery.setFacetMinCount(1)
        val response =solrServer.query(solrQuery)
        println("query " + solrQuery.toString)
        //now process all the values that are in the row_key facet
        val rowKeyFacets = response.getFacetField("row_key")
        val values = rowKeyFacets.getValues().asScala[org.apache.solr.client.solrj.response.FacetField.Count]
        if(values.size>0){
            Some(values.map(facet=> facet.getName).asInstanceOf[List[String]]);
        }
        else
            None
    }
    /**
     * Writes the list of row_keys for the results of the sepcified query to the
     * output stream.
     */
    override def writeRowKeysToStream(query:String, outputStream: OutputStream)={
        init
        val size =100;
        var start =0;
        val solrQuery = new SolrQuery();
        var continue = true
        solrQuery.setQueryType("standard");
        solrQuery.setFacet(false)
        solrQuery.setFields("row_key")        
        solrQuery.setQuery(query)
        solrQuery.setRows(100)
        while(continue){
            solrQuery.setStart(start)
            val response = solrServer.query(solrQuery)
            
            val resultsIterator =response.getResults().iterator
            while(resultsIterator.hasNext){
                val result = resultsIterator.next()
                outputStream.write((result.getFieldValue("row_key")+ "\n").getBytes())
            }
            
            start +=size
            continue = response.getResults.getNumFound > start
        }
    }
    

    def printNumDocumentsInIndex() = {
        val rq = solrServer.query(new SolrQuery("*:*"))
        println(">>>>>>>>>>>>>Document count of index: " + rq.getResults().getNumFound())
    }



  /**
   * The Actor which allows solr index inserts to be performed on a Thread.
   * solrServer.add(docs) is not thread safe - we should only ever have one thread adding documents to the solr server
   */
class SolrIndexActor extends Actor{
  var processed = 0
  var received =0

  def ready = processed==received
  def act {
    loop{
      react{
        case docs:java.util.ArrayList[SolrInputDocument] =>{
          //send them off to the solr server using a thread...
          println("Sending docs to SOLR "+ docs.size+"-" + received)
          received += 1
          try {
              solrServer.add(docs)
              solrServer.commit
              //printNumDocumentsInIndex
          } catch {
              case e: Exception => logger.error(e.getMessage, e)
          }
          processed +=1
        }
        case msg:String =>{
            if(msg == "reload"){
              cc.reload("")
            }
            if(msg == "exit")
              exit()
        }
      }
    }
  }
}
}
