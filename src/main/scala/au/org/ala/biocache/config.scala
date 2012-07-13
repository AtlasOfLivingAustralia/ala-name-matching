package au.org.ala.biocache

import java.util.Properties
import au.org.ala.sds.SensitiveSpeciesFinder
import au.org.ala.sds.SensitiveSpeciesFinderFactory
import com.google.inject.name.Names
import au.org.ala.checklist.lucene.CBIndexSearch
import org.slf4j.LoggerFactory
import com.google.inject._
import org.ala.layers.client.Client

/**
 * Simple singleton wrapper for Guice (or spring)
 */
object Config {

    private val configModule = new ConfigModule()
    var inj:Injector = Guice.createInjector(configModule)
    def getInstance(classs:Class[_]) = inj.getInstance(classs)

    def occurrenceDAO = getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
    def outlierStatsDAO = getInstance(classOf[OutlierStatsDAO]).asInstanceOf[OutlierStatsDAO]
    def deletedRecordDAO = getInstance(classOf[DeletedRecordDAO]).asInstanceOf[DeletedRecordDAO]
    def duplicateDAO = getInstance(classOf[DuplicateDAO]).asInstanceOf[DuplicateDAO]
    def persistenceManager = getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]
    def nameIndex = getInstance(classOf[CBIndexSearch]).asInstanceOf[CBIndexSearch]
    def indexDAO = getInstance(classOf[IndexDAO]).asInstanceOf[IndexDAO]

    lazy val sdsFinder = {
      SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder("http://sds.ala.org.au/sensitive-species-data.xml", nameIndex)
	  }

    val allowLayerLookup = {
      val str = configModule.properties.getProperty("allowLayerLookup")
      if(str != null){
        str.toBoolean
      } else {
        false
      }
    }
    
    lazy val excludeSensitiveValuesFor= configModule.properties.getProperty("excludeSensitiveValuesFor","")
    
    
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
        println("Fields to sample: " + fields.mkString(","))
        fields    //fields.dropWhile(x => List("el898","cl909","cl900").contains(x))
      } else {
        val fields = str.split(",").map(x => x.trim).toArray
        println("Fields to sample: " + fields.mkString(","))
        fields
      }
    }

    lazy val reindexUrl = configModule.properties.getProperty("reindexUrl")
    lazy val reindexData = configModule.properties.getProperty("reindexData")
    lazy val reindexViewDataResourceUrl = configModule.properties.getProperty("reindexViewDataResourceUrl")
    def getProperty(prop:String) = configModule.properties.getProperty(prop)
}

/**
 * Guice module
 */
class ConfigModule extends AbstractModule {

    protected val logger = LoggerFactory.getLogger("ConfigModule")
    val properties = {
      val properties = new Properties()
      properties.load(this.getClass.getResourceAsStream("/biocache.properties"))
      properties
    }

    override def configure() {

        Names.bindProperties(this.binder, properties)

        //bind concrete implementations
        bind(classOf[OccurrenceDAO]).to(classOf[OccurrenceDAOImpl]).in(Scopes.SINGLETON)
        bind(classOf[OutlierStatsDAO]).to(classOf[OutlierStatsDAOImpl]).in(Scopes.SINGLETON)
        bind(classOf[IndexDAO]).to(classOf[SolrIndexDAO]).in(Scopes.SINGLETON)
        bind(classOf[DeletedRecordDAO]).to(classOf[DeletedRecordDAOImpl]).in(Scopes.SINGLETON)
        bind(classOf[DuplicateDAO]).to(classOf[DuplicateDAOImpl]).in(Scopes.SINGLETON)
        try {
            val nameIndex = new CBIndexSearch(properties.getProperty("nameIndexLocation"))
            bind(classOf[CBIndexSearch]).toInstance(nameIndex)
           
            
        } catch {
            case e: Exception => logger.warn("Lucene indexes arent currently available.")
        }

        properties.getProperty("db") match {
            case "cassandra" => bind(classOf[PersistenceManager]).to(classOf[CassandraPersistenceManager]).in(Scopes.SINGLETON)
            //case "mongodb" => bind(classOf[PersistenceManager]).to(classOf[MongoDBPersistenceManager]).in(Scopes.SINGLETON)
            case _ => bind(classOf[PersistenceManager]).to(classOf[CassandraPersistenceManager]).in(Scopes.SINGLETON)
        }
    }
}