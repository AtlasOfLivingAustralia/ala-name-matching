package au.org.ala.biocache

import java.util.Properties
import au.org.ala.sds.SensitiveSpeciesFinder
import au.org.ala.sds.SensitiveSpeciesFinderFactory
import com.google.inject.name.Names
import au.org.ala.checklist.lucene.CBIndexSearch
import org.slf4j.LoggerFactory
import com.google.inject._
import org.ala.layers.client.Client
import java.io.FileInputStream

/**
 * Simple singleton wrapper for Guice.
 */
object Config {

  protected val logger = LoggerFactory.getLogger("Config")
  private val configModule = new ConfigModule()
  var inj:Injector = Guice.createInjector(configModule)
  def getInstance(classs:Class[_]) = inj.getInstance(classs)

  def occurrenceDAO = getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
  def outlierStatsDAO = getInstance(classOf[OutlierStatsDAO]).asInstanceOf[OutlierStatsDAO]
  def deletedRecordDAO = getInstance(classOf[DeletedRecordDAO]).asInstanceOf[DeletedRecordDAO]
  def duplicateDAO = getInstance(classOf[DuplicateDAO]).asInstanceOf[DuplicateDAO]
  def assertionQueryDAO = getInstance(classOf[AssertionQueryDAO]).asInstanceOf[AssertionQueryDAO]
  def persistenceManager = getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]
  def nameIndex = getInstance(classOf[CBIndexSearch]).asInstanceOf[CBIndexSearch]
  def indexDAO = getInstance(classOf[IndexDAO]).asInstanceOf[IndexDAO]

  lazy val sdsFinder = {
    val sdsUrl = configModule.properties.getProperty("sdsUrl","http://sds.ala.org.au/sensitive-species-data.xml")
    SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder(sdsUrl, nameIndex)
  }

  val allowLayerLookup = {
    val str = configModule.properties.getProperty("allowLayerLookup")
    if(str != null){
      str.toBoolean
    } else {
      false
    }
  }

  val deletedFileStore = configModule.properties.getProperty("deletedFileStore","/data/biocache-delete/")

  lazy val excludeSensitiveValuesFor = configModule.properties.getProperty("excludeSensitiveValuesFor","")
  //NQ 2013-12-16: Can't use http://collections.ala.org.au/ws because the collection/institution code lookup does NOT include the ws suffix
  lazy val registryURL = configModule.properties.getProperty("registryURL","http://collections.ala.org.au")

  lazy val biocacheServiceURL = configModule.properties.getProperty("biocacheServiceURL","http://biocache.ala.org.au/ws")

  lazy val allowCollectoryUpdates = configModule.properties.getProperty("allowCollectoryUpdates","false")

  lazy val extraMiscFields=configModule.properties.getProperty("extraMiscFields","")

  lazy val fieldsToSample = {
    val str = configModule.properties.getProperty("fieldsToSample")
    val defaultFields = configModule.properties.getProperty("defaultFieldsToSample")
    if (str == null || str.trim == ""){
      var dbfields = try{Client.getLayerIntersectDao.getConfig.getFieldsByDB} catch{case e:Exception =>new java.util.ArrayList()}
      var fields: Array[String] = if(dbfields.size>0) Array.ofDim(dbfields.size()) else defaultFields.split(",").map(x => x.trim).toArray
      if(dbfields.size > 0){
          for (a <- 0 until dbfields.size()) {
            fields(a) = dbfields.get(a).getId()
          }
      }
      logger.info("Fields to sample: " + fields.mkString(","))
      fields    //fields.dropWhile(x => List("el898","cl909","cl900").contains(x))
    } else {
      val fields = str.split(",").map(x => x.trim).toArray
      logger.info("Fields to sample: " + fields.mkString(","))
      fields
    }
  }

  lazy val flickrUsersUrl = configModule.properties.getProperty("flickrUsersUrl", "http://auth.ala.org.au/userdetails/external/flickr")
  lazy val reindexUrl = configModule.properties.getProperty("reindexUrl")
  lazy val reindexData = configModule.properties.getProperty("reindexData")
  lazy val reindexViewDataResourceUrl = configModule.properties.getProperty("reindexViewDataResourceUrl")
  lazy val layersServiceUrl = configModule.properties.getProperty("layersServiceUrl")
  lazy val biocacheServiceUrl = configModule.properties.getProperty("biocacheServiceUrl")

  def getProperty(prop:String) = configModule.properties.getProperty(prop)

  def getProperty(prop:String, default:String) = configModule.properties.getProperty(prop,default)
}

/**
 * Guice module
 */
class ConfigModule extends AbstractModule {

  protected val logger = LoggerFactory.getLogger("ConfigModule")

  val properties = {
    val properties = new Properties()
    //NC 2013-08-16: Supply the properties file as a system property via -Dbiocache.config=<file>
    //or the default /data/biocache/config/biocache-config.properties file is used.

    //check to see if a system property has been supplied with the location of the config file
    val filename = System.getProperty("biocache.config","/data/biocache/config/biocache-config.properties")
    var file = new java.io.File(filename)
    
    //only load the properties file if it exists otherwise default to the biocache.properties on the classpath
    val stream = if(file.exists()) {
      new FileInputStream(file) 
    } else {
      this.getClass.getResourceAsStream("/biocache.properties")
    }
    
    if(stream == null){
      throw new RuntimeException("Configuration file not found. Please add to classpath or /data/biocache/config/biocache-config.properties")
    }
    
    logger.info("Loading configuration from " + filename)
    properties.load(stream)

    properties
  }

  override def configure() {

    Names.bindProperties(this.binder, properties)
    //bind concrete implementations
    bind(classOf[OccurrenceDAO]).to(classOf[OccurrenceDAOImpl]).in(Scopes.SINGLETON)
    bind(classOf[OutlierStatsDAO]).to(classOf[OutlierStatsDAOImpl]).in(Scopes.SINGLETON)
    logger.info("Initialise SOLR")
    bind(classOf[IndexDAO]).to(classOf[SolrIndexDAO]).in(Scopes.SINGLETON)
    bind(classOf[DeletedRecordDAO]).to(classOf[DeletedRecordDAOImpl]).in(Scopes.SINGLETON)
    bind(classOf[DuplicateDAO]).to(classOf[DuplicateDAOImpl]).in(Scopes.SINGLETON)
    bind(classOf[AssertionQueryDAO]).to(classOf[AssertionQueryDAOImpl]).in(Scopes.SINGLETON)
    logger.info("Initialise name matching indexes")
    try {
      val nameIndex = new CBIndexSearch(properties.getProperty("nameIndexLocation"))
      bind(classOf[CBIndexSearch]).toInstance(nameIndex)
    } catch {
      case e: Exception => logger.warn("Lucene indexes arent currently available. Message: " + e.getMessage())
    }
    logger.info("Initialise persistence manager")
    properties.getProperty("db") match {
        //case "mock" => bind(classOf[PersistenceManager]).to(classOf[MockPersistenceManager]).in(Scopes.SINGLETON)
        case "postgres" => bind(classOf[PersistenceManager]).to(classOf[PostgresPersistenceManager]).in(Scopes.SINGLETON)
        //case "cassandra" => bind(classOf[PersistenceManager]).to(classOf[CassandraPersistenceManager]).in(Scopes.SINGLETON)
        //case "mongodb" => bind(classOf[PersistenceManager]).to(classOf[MongoDBPersistenceManager]).in(Scopes.SINGLETON)
        case _ => bind(classOf[PersistenceManager]).to(classOf[CassandraPersistenceManager]).in(Scopes.SINGLETON)
    }
    logger.info("Configure complete")
  }
}