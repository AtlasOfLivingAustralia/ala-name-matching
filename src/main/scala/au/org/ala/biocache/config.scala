package au.org.ala.biocache

import java.util.Properties
import au.org.ala.sds.SensitiveSpeciesFinder
import au.org.ala.sds.SensitiveSpeciesFinderFactory
import com.google.inject.name.Names
import au.org.ala.checklist.lucene.CBIndexSearch
import com.google.inject.{Scopes, Guice, Injector, AbstractModule}
import org.slf4j.LoggerFactory

/**
 * Simple singleton wrapper for Guice (or spring)
 */
object Config {
    private val inj:Injector = Guice.createInjector(new ConfigModule())
    def getInstance(classs:Class[_]) = inj.getInstance(classs)

    val occurrenceDAO = getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
    val persistenceManager = getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]
    val nameIndex = getInstance(classOf[CBIndexSearch]).asInstanceOf[CBIndexSearch]
    val sdsFinder = getInstance(classOf[SensitiveSpeciesFinder]).asInstanceOf[SensitiveSpeciesFinder]
}

/**
 * Guice module
 */
class ConfigModule extends AbstractModule {

    protected val logger = LoggerFactory.getLogger("ConfigModule")

    override def configure() {

        val properties = new Properties()
        properties.load(this.getClass.getResourceAsStream("/biocache.properties"))
        Names.bindProperties(this.binder,properties)

        //bind concrete implementations
        bind(classOf[OccurrenceDAO]).to(classOf[OccurrenceDAOImpl]).in(Scopes.SINGLETON)
        bind(classOf[IndexDAO]).to(classOf[SolrIndexDAO]).in(Scopes.SINGLETON)

        try {
            val nameIndex = new CBIndexSearch(properties.getProperty("nameIndexLocation"))
            bind(classOf[CBIndexSearch]).toInstance(nameIndex)
            //Initialising this here because we may wish to process records form the biocache-service and it is expensive to startup
            val sdsFinder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder("http://sds.ala.org.au/sensitive-species-data.xml", nameIndex);
            bind(classOf[SensitiveSpeciesFinder]).toInstance(sdsFinder)
            
        } catch {
            case e: Exception => logger.warn("Lucene indexes arent currently available.")
        }

        properties.getProperty("db") match {
            case "cassandra" => bind(classOf[PersistenceManager]).to(classOf[CassandraPersistenceManager]).in(Scopes.SINGLETON)
            case "mongodb" => bind(classOf[PersistenceManager]).to(classOf[MongoDBPersistenceManager]).in(Scopes.SINGLETON)
            case _ => bind(classOf[PersistenceManager]).to(classOf[CassandraPersistenceManager]).in(Scopes.SINGLETON)
        }
    }
}