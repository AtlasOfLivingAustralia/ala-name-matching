package au.org.ala.biocache.index

import com.google.inject.Inject
import com.google.inject.name.Named
import org.slf4j.LoggerFactory
import org.apache.solr.core.CoreContainer
import org.apache.solr.client.solrj.{StreamingResponseCallback, SolrQuery, SolrServer}
import au.org.ala.biocache.dao.OccurrenceDAO
import org.apache.solr.common.{SolrDocument, SolrInputDocument}
import java.io.{OutputStream, File}
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer
import org.apache.solr.client.solrj.impl.HttpSolrServer
import org.apache.solr.client.solrj.response.FacetField
import org.apache.solr.common.params.{MapSolrParams, ModifiableSolrParams}
import java.util.Date
import au.org.ala.biocache.parser.DateParser
import au.org.ala.biocache.Config
import au.org.ala.biocache.caches.TaxonSpeciesListDAO
import org.apache.commons.lang.StringUtils
import java.util.concurrent.ArrayBlockingQueue
import au.org.ala.biocache.load.FullRecordMapper
import au.org.ala.biocache.vocab.{SpeciesGroups, ErrorCodeCategory, AssertionCodes}
import au.org.ala.biocache.util.Json

/**
 * DAO for indexing to SOLR
 */
class SolrIndexDAO @Inject()(@Named("solr.home") solrHome: String,
                             @Named("exclude.sensitive.values") excludeSensitiveValuesFor: String,
                             @Named("extra.misc.fields") defaultMiscFields: String) extends IndexDAO {

  import scala.collection.JavaConverters._
  import scala.collection.JavaConversions._

  override val logger = LoggerFactory.getLogger("SolrIndexDAO")

  val nameRegex="""(?:name":")([a-zA-z0-9]*)""".r
  val  codeRegex = """(?:code":)([0-9]*)""".r
  val qaStatusRegex = """(?:qaStatus":)([0-9]*)""".r

  val arrDefaultMiscFields = if (defaultMiscFields == null) Array[String]() else defaultMiscFields.split(",")
  var cc: CoreContainer = _
  var solrServer: SolrServer = _
  var cloudServer: org.apache.solr.client.solrj.impl.CloudSolrServer = _
  var solrConfigPath: String = ""

  @Inject
  var occurrenceDAO: OccurrenceDAO = _
  val solrDocList = new java.util.ArrayList[SolrInputDocument](1000)
  var ids = 0
  val fieldSuffix = """([A-Za-z_\-0.9]*)"""
  val doublePattern = (fieldSuffix + """_d""").r
  val intPattern = (fieldSuffix + """_i""").r

  lazy val drToExcludeSensitive = excludeSensitiveValuesFor.split(",")

  override def init() {

    if (solrServer == null) {
      logger.info("Initialising the solr server " + solrHome + " " + cloudServer + " " + solrServer)
      if(!solrHome.startsWith("http://")){
        if(solrHome.contains(":")) {
          //assume that it represents a SolrCloud
          cloudServer = new org.apache.solr.client.solrj.impl.CloudSolrServer(solrHome)
          cloudServer.setDefaultCollection("biocache1")
          solrServer = cloudServer
        } else if (solrConfigPath != "") {
          logger.info("Initialising embedded SOLR server.....")
          cc = CoreContainer.createAndLoad(solrHome, new File(solrHome+"/solr.xml"))
          solrServer = new EmbeddedSolrServer(cc, "biocache")
        } else {
          logger.info("Initialising embedded SOLR server.....")
          System.setProperty("solr.solr.home", solrHome)
          cc = CoreContainer.createAndLoad(solrHome,new File(solrHome + "/solr.xml"))//new CoreContainer(solrHome)
          solrServer = new EmbeddedSolrServer(cc, "biocache")
        }
      } else {
        logger.info("Initialising connection to SOLR server.....")
        solrServer = new HttpSolrServer(solrHome)
        logger.info("Initialising connection to SOLR server - done.")
      }
    }
  }

  def reload = if(cc != null) cc.reload("biocache")

  override def shouldIncludeSensitiveValue(dr: String): Boolean = {
    !drToExcludeSensitive.contains(dr)
  }

  def pageOverFacet(proc: (String, Int) => Boolean, facetName: String, queryString: String = "*:*", filterQueries: Array[String] = Array()) {
    init

    val FACET_PAGE_SIZE = 1000

    val query = new SolrQuery(queryString)
    query.setFacet(true)
    query.addFacetField(facetName)
    query.setRows(0)
    query.setFacetLimit(FACET_PAGE_SIZE)
    query.setStart(0)
    query.setFacetMinCount(1)
    filterQueries.foreach(query.addFilterQuery(_))
    //query.setFilterQueries(filterQueries: _ *)

    var facetOffset = 0
    var values : java.util.List[FacetField.Count] = null

    do {
      query.remove("facet.offset")
      query.add("facet.offset", facetOffset.toString)

      val response = solrServer.query(query)
      values = response.getFacetField(facetName).getValues
      if (values != null) {
        values.asScala.foreach(s => proc(s.getName, s.getCount.toInt))
      }
      facetOffset += FACET_PAGE_SIZE

    } while (values != null && !values.isEmpty)
  }

  def streamIndex(proc: java.util.Map[String,AnyRef] => Boolean, fieldsToRetrieve:Array[String], query:String, filterQueries: Array[String], sortFields: Array[String], multivaluedFields: Option[Array[String]] = None){
    init

    val params = collection.immutable.HashMap(
      "collectionName" -> "biocache",
      "q" -> query,
      "start" -> "0",
      "rows" -> Int.MaxValue.toString,
      "fl" -> fieldsToRetrieve.mkString(","))

    val solrParams = new ModifiableSolrParams()
    solrParams.add(new MapSolrParams(params) )
    solrParams.add("fq", filterQueries:_*)
    if(!sortFields.isEmpty){
      solrParams.add("sort",sortFields.mkString(" asc,") +" asc")
    }

    //now stream
    val solrCallback = new SolrCallback(proc, multivaluedFields)
    logger.info("Starting to strem: " +new java.util.Date().toString + " " + params)
    solrServer.queryAndStreamResponse(solrParams, solrCallback)
    logger.info("Finished streaming : " +new java.util.Date().toString + " " + params)
  }

  def pageOverIndex(proc: java.util.Map[String, AnyRef] => Boolean, fieldToRetrieve: Array[String], queryString: String = "*:*", filterQueries: Array[String] = Array(), sortField: Option[String] = None, sortDir: Option[String] = None, multivaluedFields: Option[Array[String]] = None) {
    init

    var startIndex = 0
    var query: SolrQuery = new SolrQuery(queryString)
    query.setFacet(false)
    query.setRows(0)
    query.setStart(startIndex)
    query.setFilterQueries(filterQueries: _*)
    query.setFacet(false)
    fieldToRetrieve.foreach(f => query.addField(f))
    var response = solrServer.query(query)
    val fullResults = response.getResults.getNumFound.toInt
    logger.debug("Total found for :" + queryString + ", " + fullResults)

    var counter = 0
    var pageSize = 5000
    while (counter < fullResults) {

      val q = new SolrQuery(queryString)
      q.setFacet(false)
      q.setStart(counter)
      q.setFilterQueries(filterQueries: _*)
      q.setFacet(false)
      if (sortField.isDefined) {
        val dir = sortDir.getOrElse("asc")
        q.setSortField(sortField.get, if (dir == "asc") org.apache.solr.client.solrj.SolrQuery.ORDER.asc else org.apache.solr.client.solrj.SolrQuery.ORDER.desc)
      }

      if (counter + pageSize > fullResults) {
        pageSize = fullResults - counter
      }

      //setup the next query
      q.setRows(pageSize)
      response = solrServer.query(q)
      logger.info("Paging through :" + queryString + ", " + counter)
      val solrDocumentList = response.getResults
      val iter = solrDocumentList.iterator()
      while (iter.hasNext) {
        val solrDocument = iter.next()
        val map = new java.util.HashMap[String, Object]
        solrDocument.getFieldValueMap().keySet().asScala.foreach(s => map.put(s, if (multivaluedFields.isDefined && multivaluedFields.get.contains(s)) solrDocument.getFieldValues(s) else solrDocument.getFieldValue(s)))
        proc(map)
      }
      counter += pageSize
    }
  }

  def emptyIndex {
    init
    try {
      solrServer.deleteByQuery("*:*")
    } catch {
      case e: Exception => logger.error("Problem clearing index...", e)
    }
  }

  def removeFromIndex(field: String, value: String) = {
    init
    try {
      logger.debug("Deleting " + field +":" + value)
      solrServer.deleteByQuery(field + ":\"" + value + "\"")
      solrServer.commit
    } catch {
      case e: Exception => logger.error("Problem removing from index...", e)
    }
  }

  def removeByQuery(query: String, commit: Boolean = true) = {
    init
    logger.debug("Deleting by query: " + query)
    try {
      solrServer.deleteByQuery(query)
      if (commit)
        solrServer.commit
    } catch {
      case e: Exception => e.printStackTrace
    }
  }

  def finaliseIndex(optimise: Boolean = false, shutdown: Boolean = true) {
    init
    if (!solrDocList.isEmpty) {
      solrServer.add(solrDocList)
      Thread.sleep(50)
    }

    solrServer.commit
    solrDocList.clear
    //clear the cache for the SpeciesLIst
    //TaxonSpeciesListDAO.clearCache()
    //now we should close the indexWriter
    logger.info(printNumDocumentsInIndex)
    if (optimise) {
      logger.info("Optimising the indexing...")
      this.optimise
    }
    if (shutdown) {
      logger.info("Shutting down the indexing...")
      this.shutdown
    }

    logger.info("Finalise finished.")
  }

  /**
   * Shutdown the index by stopping the indexing thread and shutting down the index core
   */
  def shutdown {
    //threads.foreach(t => t.stopRunning)
    if (cc != null)
      cc.shutdown
  }

  def optimise : String = {
    init
    solrServer.optimize
    printNumDocumentsInIndex
  }

  override def commit() {
    init
    solrServer.commit
  }

  /**
   * Decides whether or not the current record should be indexed based on processed times
   */
  def shouldIndex(map: scala.collection.Map[String, String], startDate: Option[Date]): Boolean = {
    if (!startDate.isEmpty) {
      val lastLoaded = DateParser.parseStringToDate(getValue(FullRecordMapper.alaModifiedColumn, map))
      val lastProcessed = DateParser.parseStringToDate(getValue(FullRecordMapper.alaModifiedColumn + ".p", map))
      return startDate.get.before(lastProcessed.getOrElse(startDate.get)) || startDate.get.before(lastLoaded.getOrElse(startDate.get))
    }
    true
  }

  val multifields = Array("duplicate_inst", "establishment_means", "species_group", "assertions", "data_hub_uid", "interactions", "outlier_layer",
    "species_habitats", "multimedia", "all_image_url", "collectors", "duplicate_record", "duplicate_type","taxonomic_issue")

  val typeNotSuitableForModelling = Array("invalid", "historic", "vagrant", "irruptive")

  def extractPassAndFailed(json:String):(List[Int], List[(String,String)])={
    val codes = codeRegex.findAllMatchIn(json).map(_.group(1).toInt).toList
    val names = nameRegex.findAllMatchIn(json).map(_.group(1)).toList
    val qaStatuses = qaStatusRegex.findAllMatchIn(json).map(_.group(1)).toList
    val assertions = (names zip qaStatuses)
    if(logger.isDebugEnabled()){
      logger.debug("Codes:" + codes.toString)
      logger.debug("Name:" + names.toString)
      logger.debug("QA statuses:" + qaStatuses.toString)
      logger.debug("Assertions:" + assertions.toString)
    }

    (codes, assertions)
  }

  /**
   * A SOLR specific implementation of indexing from a map.
   */
  override def indexFromMap(guid: String, map: scala.collection.Map[String, String], batch: Boolean = true,
                            startDate: Option[Date] = None, commit: Boolean = false,
                            miscIndexProperties: Seq[String] = Array[String](), test:Boolean = false) {
    init

    //val header = getHeaderValues()
    if (shouldIndex(map, startDate)) {
      val values = getOccIndexModel(guid, map)
      if (values.length > 0 && values.length != header.length) {
        logger.warn("values don't matcher header: " + values.length + ":" + header.length + ", values:header")
        logger.warn("Headers: " + header.toString())
        logger.warn("Values: " + values.toString())
        exit(1)
      }
      if (values.length > 0) {
        val doc = new SolrInputDocument()
        for (i <- 0 to values.length - 1) {
          if (values(i) != "") {
            if (multifields.contains(header(i))) {
              //multiple values in this field
              for (value <- values(i).split('|')) {
                if (value != "")
                  doc.addField(header(i), value)
              }
            }
            else
              doc.addField(header(i), values(i))
          }
        }

        //add the misc properties here....
        //NC 2013-04-23: Change this code to support data types in misc fields.
        if (!miscIndexProperties.isEmpty) {
          val unparsedJson = map.getOrElse(FullRecordMapper.miscPropertiesColumn, "")
          if (unparsedJson != "") {
            val map = Json.toMap(unparsedJson)
            miscIndexProperties.foreach(prop =>{
              prop match {
                case it if it.endsWith("_i") || it.endsWith("_d") || it.endsWith("_s") => {
                  val v = map.get(it.take(it.length-2))
                  if(v.isDefined){
                    doc.addField(it, v.get.toString())
                  }
                }
                case _ => {
                  val v = map.get(prop)
                  if(v.isDefined){
                    doc.addField(prop + "_s", v.get.toString())
                  }
                }
              }
            })
          }
        }

        if (!arrDefaultMiscFields.isEmpty) {
          val unparsedJson = map.getOrElse(FullRecordMapper.miscPropertiesColumn, "")
          if (unparsedJson != "") {
            val map = Json.toMap(unparsedJson)
            arrDefaultMiscFields.foreach(value => {
              value match {
                case doublePattern(field) => {
                  //ensure that the value represents a double value before adding to the index.
                  val fvalue = map.getOrElse(field, "").toString()
                  if (fvalue.size > 0) {
                    try {
                      java.lang.Double.parseDouble(fvalue)
                      doc.addField(value, fvalue)
                    }
                    catch {
                      case e:Exception => logger.error("Unable to convert value to double " + fvalue + " for " + guid, e)
                    }
                  }
                }
                case intPattern(field) => {
                  val fvalue = map.getOrElse(field, "").toString()
                  if (fvalue.size > 0) {
                    try {
                      java.lang.Integer.parseInt(fvalue)
                      doc.addField(value, fvalue)
                    }
                    catch {
                      case e:Exception => logger.error("Unable to convert value to int " + fvalue + " for " + guid, e)
                    }
                  }
                }
                case _ => {
                  //remove the suffix
                  val item = if (value.contains("_")) value.substring(0, value.lastIndexOf("_")) else value
                  val fvalue = map.getOrElse(value, map.getOrElse(item, "")).toString()
                  if (fvalue.size > 0)
                    doc.addField(value, fvalue)
                }
              }
            })
          }
        }

        //now index the System QA assertions
        //NC 2013-08-01: It is very inefficient to make a JSONArray of QualityAssertions We will parse the raw string instead.
        val qaJson  = map.getOrElse(FullRecordMapper.qualityAssertionColumn, "[]")
        val(qa, status) = extractPassAndFailed(qaJson)
        var sa = false
        status.foreach{case (test, status)=>
          if (status.equals("1")){
            doc.addField("assertions_passed", test)
          } else if (status.equals("0")){
            sa = true
            //get the error code to see if it is "missing"
            //println(test +" " + guid)
            def indexField = if(AssertionCodes.getByName(test).get.category == ErrorCodeCategory.Missing) "assertions_missing" else "assertions"
            doc.addField(indexField, test)
          }
        }

        val unchecked = AssertionCodes.getMissingByCode(qa)
        unchecked.foreach(ec => doc.addField("assertions_unchecked", ec.name))

        doc.addField("system_assertions", sa)

        //load the species lists that are configured for the matched guid.
        val (speciesLists,extraValues) = TaxonSpeciesListDAO.getCachedListsForTaxon(map.getOrElse("taxonConceptID.p",""))
        speciesLists.foreach(v=>{
          doc.addField("species_list_uid",v)
          //doc.addField(v, "true")
        })
        extraValues.foreach {case (key, value) => {
          doc.addField(key, value)
        }}

        // user if userQA = true
        val hasUserAssertions = map.getOrElse(FullRecordMapper.userQualityAssertionColumn, "false")
        if ("true".equals(hasUserAssertions)) {
          val assertionUserIds = Config.occurrenceDAO.getUserIdsForAssertions(guid)
          assertionUserIds.foreach(id => doc.addField("assertion_user_id", id))
        }

        val queryAssertions = Json.toStringMap(map.getOrElse(FullRecordMapper.queryAssertionColumn, "{}"))
        var suitableForModelling = true
        queryAssertions.foreach {
          case (key, value) => {
            doc.addField("query_assertion_uuid", key)
            doc.addField("query_assertion_type_s", value)
            if (suitableForModelling && typeNotSuitableForModelling.contains(value))
              suitableForModelling = false
          }
        }
        //this will not exist for all records until a complete reindex is performed...
        doc.addField("suitable_modelling", suitableForModelling.toString)

        //index the available el and cl's - more efficient to use the supplied map than using the old way
        val els = Json.toStringMap(map.getOrElse("el.p", "{}"))
        els.foreach {
          case (key, value) => doc.addField(key, value)
        }
        val cls = Json.toStringMap(map.getOrElse("cl.p", "{}"))
        cls.foreach {
          case (key, value) => doc.addField(key, value)
        }

        //TODO Think about moving species group stuff here.
        //index the additional species information - ie species groups
        val lft = map.get("left.p")
        val rgt = map.get("right.p")
        if(lft.isDefined && rgt.isDefined){
          val sgs = SpeciesGroups.getSpeciesSubGroups(lft.get, rgt.get)
          if(sgs.isDefined){
            sgs.get.foreach{v:String => doc.addField("species_subgroup", v)}
          }
        }
        if(!test){
          if (!batch) {
            solrServer.add(doc)
            solrServer.commit
          } else {
            solrDocList.synchronized {
              if (!StringUtils.isEmpty(values(0)))
                solrDocList.add(doc)

              if (solrDocList.size == 1000 || (commit && solrDocList.size > 0)) {

                solrServer.add(solrDocList)
                if (commit || solrDocList.size >= 10000){
                  solrServer.commit
                }
                solrDocList.clear
              }
            }
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
   */
  override def getRowKeysForQuery(query: String, limit: Int = 1000): Option[List[String]] = {

    init
    val solrQuery = new SolrQuery();
    solrQuery.setQueryType("standard");
    // Facets
    solrQuery.setFacet(true)
    solrQuery.addFacetField("row_key")
    solrQuery.setQuery(query)
    solrQuery.setRows(0)
    solrQuery.setFacetLimit(limit)
    solrQuery.setFacetMinCount(1)
    try{
      val response = solrServer.query(solrQuery)
      logger.debug("Query " + solrQuery.toString)
      //now process all the values that are in the row_key facet
      val rowKeyFacets = response.getFacetField("row_key")
      val values = rowKeyFacets.getValues().asScala
      if (values.size > 0) {
        Some(values.map(facet => facet.getName).toList)
      } else {
        None
      }
    } catch {
      case e:Exception => logger.warn("Unable to get key " + query+".");None
    }
  }

  def getDistinctValues(query: String, field: String, max: Int): Option[List[String]] = {
    init
    val solrQuery = new SolrQuery();
    solrQuery.setQueryType("standard");
    // Facets
    solrQuery.setFacet(true)
    solrQuery.addFacetField(field)
    solrQuery.setQuery(query)
    solrQuery.setRows(0)
    solrQuery.setFacetLimit(max)
    solrQuery.setFacetMinCount(1)
    val response = solrServer.query(solrQuery)
    val facets = response.getFacetField(field)
    //TODO page through the facets to make more efficient.
    if (facets.getValues() != null && facets.getValues().size() > 0) {
      val values = facets.getValues().asScala
      if (values != null && !values.isEmpty) {
        /*
          NC: Needed to change this method after the upgrade as it now throws a cast exception
          old value: Some(values.map(facet => facet.getName).asInstanceOf[List[String]])
         */
        Some(values.map(facet => facet.getName).toList)
      } else {
        None
      }
    } else {
      None
    }
  }

  /**
   * Writes the list of row_keys for the results of the specified query to the
   * output stream.
   */
  override def writeRowKeysToStream(query: String, outputStream: OutputStream) {
    init
    val size = 100;
    var start = 0;
    val solrQuery = new SolrQuery();
    var continue = true
    solrQuery.setQueryType("standard");
    solrQuery.setFacet(false)
    solrQuery.setFields("row_key")
    solrQuery.setQuery(query)
    solrQuery.setRows(100)
    while (continue) {
      solrQuery.setStart(start)
      val response = solrServer.query(solrQuery)

      val resultsIterator = response.getResults().iterator
      while (resultsIterator.hasNext) {
        val result = resultsIterator.next()
        outputStream.write((result.getFieldValue("row_key") + "\n").getBytes())
      }

      start += size
      continue = response.getResults.getNumFound > start
    }
  }


  def printNumDocumentsInIndex(): String = {
    ">>>>>>>>>>>>> Document count of index: " + solrServer.query(new SolrQuery("*:*")).getResults().getNumFound()
  }

  class AddDocThread(queue: ArrayBlockingQueue[java.util.List[SolrInputDocument]], id: Int) extends Thread {

    private var shouldRun = true

    def stopRunning {
      shouldRun = false
    }

    override def run() {
      logger.info("Starting AddDocThread thread....")
      while (shouldRun || queue.size > 0) {
        if (queue.size > 0) {
          var docs = queue.poll()
          //add and commit the docs
          if (docs != null && !docs.isEmpty) {
            try {
              logger.info("Thread " + id + " is adding " + docs.size + " documents to the index.")
              solrServer.add(docs)
              //only the first thread should commit
              if (id == 0) solrServer.commit
              docs = null
            } catch {
              case e:Exception => logger.debug("Error committing to index", e) //do nothing
            }
          }
        } else {
          try {
            Thread.sleep(250)
          } catch {
            case e:Exception => logger.debug("Error sleeping thread", e) //do nothing
          }
        }
      }
      logger.info("Finishing AddDocThread thread.")
    }
  }

  //now stream
 class SolrCallback (proc: java.util.Map[String,AnyRef] => Boolean, multivaluedFields:Option[Array[String]]) extends StreamingResponseCallback {

    import scala.collection.JavaConverters._
    import scala.collection.JavaConversions._

    var maxResults = 0l
    var counter = 0l
    val start = System.currentTimeMillis
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis
    def streamSolrDocument(doc: SolrDocument) {
      val map = new java.util.HashMap[String, Object]
      doc.getFieldValueMap().keySet().asScala.foreach(s => {
        val value = if (multivaluedFields.isDefined && multivaluedFields.get.contains(s)){
          doc.getFieldValues(s)
        } else {
          doc.getFieldValue(s)
        }
        map.put(s, value)
      })
      proc(map)
      counter += 1
      if (counter % 10000 == 0){
        finishTime = System.currentTimeMillis
        logger.info(counter + " >> Last record : " + doc.getFieldValueMap + ", records per sec: " +
          10000.toFloat / (((finishTime - startTime).toFloat) / 1000f))
        startTime = System.currentTimeMillis
      }
    }

    def streamDocListInfo(numFound: Long, start: Long, maxScore: java.lang.Float) : Unit = {
      logger.info("NumFound: " + numFound +" start: " +start + " maxScore: " +maxScore)
      logger.info(new java.util.Date().toString)
      startTime = System.currentTimeMillis
      //exit(-2)
      maxResults = numFound
    }
  }
}
